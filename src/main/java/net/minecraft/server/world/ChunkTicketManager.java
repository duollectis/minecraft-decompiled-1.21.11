package net.minecraft.server.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Класс Chunk Ticket Manager.
 */
public class ChunkTicketManager extends PersistentState {

	private static final int DEFAULT_TICKETS_MAP_SIZE = 4;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Codec<Pair<ChunkPos, ChunkTicket>>
			TICKET_POS_CODEC =
			Codec.mapPair(ChunkPos.CODEC.fieldOf("chunk_pos"), ChunkTicket.CODEC).codec();
	public static final Codec<ChunkTicketManager> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(TICKET_POS_CODEC
							.listOf()
							.optionalFieldOf("tickets", List.of())
							.forGetter(ChunkTicketManager::getTickets))
					.apply(instance, ChunkTicketManager::create)
	);
	public static final PersistentStateType<ChunkTicketManager> STATE_TYPE = new PersistentStateType<>(
			"chunks", ChunkTicketManager::new, CODEC, DataFixTypes.SAVED_DATA_FORCED_CHUNKS
	);
	private final Long2ObjectOpenHashMap<List<ChunkTicket>> tickets;
	private final Long2ObjectOpenHashMap<List<ChunkTicket>> savedTickets;
	private LongSet forcedChunks = new LongOpenHashSet();
	private ChunkTicketManager.@Nullable LevelUpdater loadingLevelUpdater;
	private ChunkTicketManager.@Nullable LevelUpdater simulationLevelUpdater;

	private ChunkTicketManager(
			Long2ObjectOpenHashMap<List<ChunkTicket>> tickets,
			Long2ObjectOpenHashMap<List<ChunkTicket>> savedTickets
	) {
		this.tickets = tickets;
		this.savedTickets = savedTickets;
		this.recomputeForcedChunks();
	}

	public ChunkTicketManager() {
		this(new Long2ObjectOpenHashMap(4), new Long2ObjectOpenHashMap());
	}

	private static ChunkTicketManager create(List<Pair<ChunkPos, ChunkTicket>> tickets) {
		Long2ObjectOpenHashMap<List<ChunkTicket>> long2ObjectOpenHashMap = new Long2ObjectOpenHashMap();

		for (Pair<ChunkPos, ChunkTicket> pair : tickets) {
			ChunkPos chunkPos = (ChunkPos) pair.getFirst();
			List<ChunkTicket>
					list =
					(List<ChunkTicket>) long2ObjectOpenHashMap.computeIfAbsent(
							chunkPos.toLong(),
							l -> new ObjectArrayList(4)
					);
			list.add((ChunkTicket) pair.getSecond());
		}

		return new ChunkTicketManager(new Long2ObjectOpenHashMap(4), long2ObjectOpenHashMap);
	}

	private List<Pair<ChunkPos, ChunkTicket>> getTickets() {
		List<Pair<ChunkPos, ChunkTicket>> list = new ArrayList<>();
		this.forEachTicket((pos, ticket) -> {
			if (ticket.getType().shouldSerialize()) {
				list.add(new Pair(pos, ticket));
			}
		});
		return list;
	}

	private void forEachTicket(BiConsumer<ChunkPos, ChunkTicket> ticketConsumer) {
		forEachTicket(ticketConsumer, this.tickets);
		forEachTicket(ticketConsumer, this.savedTickets);
	}

	private static void forEachTicket(
			BiConsumer<ChunkPos, ChunkTicket> ticketConsumer,
			Long2ObjectOpenHashMap<List<ChunkTicket>> tickets
	) {
		ObjectIterator var2 = Long2ObjectMaps.fastIterable(tickets).iterator();

		while (var2.hasNext()) {
			Entry<List<ChunkTicket>> entry = (Entry<List<ChunkTicket>>) var2.next();
			ChunkPos chunkPos = new ChunkPos(entry.getLongKey());

			for (ChunkTicket chunkTicket : (List<ChunkTicket>) entry.getValue()) {
				ticketConsumer.accept(chunkPos, chunkTicket);
			}
		}
	}

	/**
	 * Promote to real tickets.
	 */
	public void promoteToRealTickets() {
		ObjectIterator var1 = Long2ObjectMaps.fastIterable(this.savedTickets).iterator();

		while (var1.hasNext()) {
			Entry<List<ChunkTicket>> entry = (Entry<List<ChunkTicket>>) var1.next();

			for (ChunkTicket chunkTicket : (List<ChunkTicket>) entry.getValue()) {
				this.addTicket(entry.getLongKey(), chunkTicket);
			}
		}

		this.savedTickets.clear();
	}

	public void setLoadingLevelUpdater(ChunkTicketManager.@Nullable LevelUpdater loadingLevelUpdater) {
		this.loadingLevelUpdater = loadingLevelUpdater;
	}

	public void setSimulationLevelUpdater(ChunkTicketManager.@Nullable LevelUpdater simulationLevelUpdater) {
		this.simulationLevelUpdater = simulationLevelUpdater;
	}

	public boolean hasTickets() {
		return !this.tickets.isEmpty();
	}

	/**
	 * Определяет, следует ли reset idle timeout.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldResetIdleTimeout() {
		ObjectIterator var1 = this.tickets.values().iterator();

		while (var1.hasNext()) {
			List<ChunkTicket> list = (List<ChunkTicket>) var1.next();

			for (ChunkTicket chunkTicket : list) {
				if (chunkTicket.getType().resetsIdleTimeout()) {
					return true;
				}
			}
		}

		return false;
	}

	public List<ChunkTicket> getTickets(long pos) {
		return (List<ChunkTicket>) this.tickets.getOrDefault(pos, List.of());
	}

	private List<ChunkTicket> getTicketsMutable(long pos) {
		return (List<ChunkTicket>) this.tickets.computeIfAbsent(pos, chunkPos -> new ObjectArrayList(4));
	}

	/**
	 * Добавляет ticket.
	 *
	 * @param type type
	 * @param pos pos
	 * @param radius radius
	 */
	public void addTicket(ChunkTicketType type, ChunkPos pos, int radius) {
		ChunkTicket chunkTicket = new ChunkTicket(type, ChunkLevels.getLevelFromType(ChunkLevelType.FULL) - radius);
		this.addTicket(pos.toLong(), chunkTicket);
	}

	/**
	 * Добавляет ticket.
	 *
	 * @param ticket ticket
	 * @param pos pos
	 */
	public void addTicket(ChunkTicket ticket, ChunkPos pos) {
		this.addTicket(pos.toLong(), ticket);
	}

	/**
	 * Добавляет ticket.
	 *
	 * @param pos pos
	 * @param ticket ticket
	 *
	 * @return boolean — результат операции
	 */
	public boolean addTicket(long pos, ChunkTicket ticket) {
		List<ChunkTicket> list = this.getTicketsMutable(pos);

		for (ChunkTicket chunkTicket : list) {
			if (ticketsEqual(ticket, chunkTicket)) {
				chunkTicket.refreshExpiry();
				this.markDirty();
				return false;
			}
		}

		int i = getLevel(list, true);
		int j = getLevel(list, false);
		list.add(ticket);
		if (SharedConstants.VERBOSE_SERVER_EVENTS) {
			LOGGER.debug("ATI {} {}", new ChunkPos(pos), ticket);
		}

		if (ticket.getType().isForSimulation() && ticket.getLevel() < i && this.simulationLevelUpdater != null) {
			this.simulationLevelUpdater.update(pos, ticket.getLevel(), true);
		}

		if (ticket.getType().isForLoading() && ticket.getLevel() < j && this.loadingLevelUpdater != null) {
			this.loadingLevelUpdater.update(pos, ticket.getLevel(), true);
		}

		if (ticket.getType().equals(ChunkTicketType.FORCED)) {
			this.forcedChunks.add(pos);
		}

		this.markDirty();
		return true;
	}

	private static boolean ticketsEqual(ChunkTicket a, ChunkTicket b) {
		return b.getType() == a.getType() && b.getLevel() == a.getLevel();
	}

	public int getLevel(long pos, boolean forSimulation) {
		return getLevel(this.getTickets(pos), forSimulation);
	}

	private static int getLevel(List<ChunkTicket> tickets, boolean forSimulation) {
		ChunkTicket chunkTicket = getActiveTicket(tickets, forSimulation);
		return chunkTicket == null ? ChunkLevels.INACCESSIBLE + 1 : chunkTicket.getLevel();
	}

	private static @Nullable ChunkTicket getActiveTicket(@Nullable List<ChunkTicket> tickets, boolean forSimulation) {
		if (tickets == null) {
			return null;
		}
		else {
			ChunkTicket chunkTicket = null;

			for (ChunkTicket chunkTicket2 : tickets) {
				if (chunkTicket == null || chunkTicket2.getLevel() < chunkTicket.getLevel()) {
					if (forSimulation && chunkTicket2.getType().isForSimulation()) {
						chunkTicket = chunkTicket2;
					}
					else if (!forSimulation && chunkTicket2.getType().isForLoading()) {
						chunkTicket = chunkTicket2;
					}
				}
			}

			return chunkTicket;
		}
	}

	/**
	 * Удаляет ticket.
	 *
	 * @param type type
	 * @param pos pos
	 * @param radius radius
	 */
	public void removeTicket(ChunkTicketType type, ChunkPos pos, int radius) {
		ChunkTicket chunkTicket = new ChunkTicket(type, ChunkLevels.getLevelFromType(ChunkLevelType.FULL) - radius);
		this.removeTicket(pos.toLong(), chunkTicket);
	}

	/**
	 * Удаляет ticket.
	 *
	 * @param ticket ticket
	 * @param pos pos
	 */
	public void removeTicket(ChunkTicket ticket, ChunkPos pos) {
		this.removeTicket(pos.toLong(), ticket);
	}

	/**
	 * Удаляет ticket.
	 *
	 * @param pos pos
	 * @param ticket ticket
	 *
	 * @return boolean — результат операции
	 */
	public boolean removeTicket(long pos, ChunkTicket ticket) {
		List<ChunkTicket> list = (List<ChunkTicket>) this.tickets.get(pos);
		if (list == null) {
			return false;
		}
		else {
			boolean bl = false;
			Iterator<ChunkTicket> iterator = list.iterator();

			while (iterator.hasNext()) {
				ChunkTicket chunkTicket = iterator.next();
				if (ticketsEqual(ticket, chunkTicket)) {
					iterator.remove();
					if (SharedConstants.VERBOSE_SERVER_EVENTS) {
						LOGGER.debug("RTI {} {}", new ChunkPos(pos), chunkTicket);
					}

					bl = true;
					break;
				}
			}

			if (!bl) {
				return false;
			}
			else {
				if (list.isEmpty()) {
					this.tickets.remove(pos);
				}

				if (ticket.getType().isForSimulation() && this.simulationLevelUpdater != null) {
					this.simulationLevelUpdater.update(pos, getLevel(list, true), false);
				}

				if (ticket.getType().isForLoading() && this.loadingLevelUpdater != null) {
					this.loadingLevelUpdater.update(pos, getLevel(list, false), false);
				}

				if (ticket.getType().equals(ChunkTicketType.FORCED)) {
					this.recomputeForcedChunks();
				}

				this.markDirty();
				return true;
			}
		}
	}

	private void recomputeForcedChunks() {
		this.forcedChunks = this.getAllChunksMatching(ticket -> ticket.getType().equals(ChunkTicketType.FORCED));
	}

	public String getDebugString(long pos, boolean forSimulation) {
		List<ChunkTicket> list = this.getTickets(pos);
		ChunkTicket chunkTicket = getActiveTicket(list, forSimulation);
		return chunkTicket == null ? "no_ticket" : chunkTicket.toString();
	}

	/**
	 * Tick.
	 *
	 * @param chunkLoadingManager chunk loading manager
	 */
	public void tick(ServerChunkLoadingManager chunkLoadingManager) {
		this.removeTicketsIf(
				(ticket, pos) -> {
					if (this.canTicketExpire(chunkLoadingManager, ticket, pos)) {
						ticket.tick();
						return ticket.isExpired();
					}
					else {
						return false;
					}
				}, null
		);
		this.markDirty();
	}

	private boolean canTicketExpire(ServerChunkLoadingManager chunkLoadingManager, ChunkTicket ticket, long pos) {
		if (!ticket.getType().canExpire()) {
			return false;
		}
		else if (ticket.getType().canExpireBeforeLoad()) {
			return true;
		}
		else {
			ChunkHolder chunkHolder = chunkLoadingManager.getCurrentChunkHolder(pos);
			return chunkHolder == null || chunkHolder.isSavable();
		}
	}

	/**
	 * Shutdown.
	 */
	public void shutdown() {
		this.removeTicketsIf((ticket, pos) -> ticket.getType() != ChunkTicketType.UNKNOWN, this.savedTickets);
	}

	public void removeTicketsIf(
			ChunkTicketManager.TicketPredicate predicate,
			@Nullable Long2ObjectOpenHashMap<List<ChunkTicket>> transferTo
	) {
		ObjectIterator<Entry<List<ChunkTicket>>> objectIterator = this.tickets.long2ObjectEntrySet().fastIterator();
		boolean bl = false;

		while (objectIterator.hasNext()) {
			Entry<List<ChunkTicket>> entry = (Entry<List<ChunkTicket>>) objectIterator.next();
			Iterator<ChunkTicket> iterator = ((List) entry.getValue()).iterator();
			long l = entry.getLongKey();
			boolean bl2 = false;
			boolean bl3 = false;

			while (iterator.hasNext()) {
				ChunkTicket chunkTicket = iterator.next();
				if (predicate.test(chunkTicket, l)) {
					if (transferTo != null) {
						List<ChunkTicket>
								list =
								(List<ChunkTicket>) transferTo.computeIfAbsent(
										l,
										pos -> new ObjectArrayList(((List) entry.getValue()).size())
								);
						list.add(chunkTicket);
					}

					iterator.remove();
					if (chunkTicket.getType().isForLoading()) {
						bl3 = true;
					}

					if (chunkTicket.getType().isForSimulation()) {
						bl2 = true;
					}

					if (chunkTicket.getType().equals(ChunkTicketType.FORCED)) {
						bl = true;
					}
				}
			}

			if (bl3 || bl2) {
				if (bl3 && this.loadingLevelUpdater != null) {
					this.loadingLevelUpdater.update(l, getLevel((List<ChunkTicket>) entry.getValue(), false), false);
				}

				if (bl2 && this.simulationLevelUpdater != null) {
					this.simulationLevelUpdater.update(l, getLevel((List<ChunkTicket>) entry.getValue(), true), false);
				}

				this.markDirty();
				if (((List) entry.getValue()).isEmpty()) {
					objectIterator.remove();
				}
			}
		}

		if (bl) {
			this.recomputeForcedChunks();
		}
	}

	/**
	 * Обновляет level.
	 *
	 * @param level level
	 * @param type type
	 */
	public void updateLevel(int level, ChunkTicketType type) {
		List<Pair<ChunkTicket, Long>> list = new ArrayList<>();
		ObjectIterator var4 = this.tickets.long2ObjectEntrySet().iterator();

		while (var4.hasNext()) {
			Entry<List<ChunkTicket>> entry = (Entry<List<ChunkTicket>>) var4.next();

			for (ChunkTicket chunkTicket : (List<ChunkTicket>) entry.getValue()) {
				if (chunkTicket.getType() == type) {
					list.add(Pair.of(chunkTicket, entry.getLongKey()));
				}
			}
		}

		for (Pair<ChunkTicket, Long> pair : list) {
			Long long_ = (Long) pair.getSecond();
			ChunkTicket chunkTicketx = (ChunkTicket) pair.getFirst();
			this.removeTicket(long_, chunkTicketx);
			ChunkTicketType chunkTicketType = chunkTicketx.getType();
			this.addTicket(long_, new ChunkTicket(chunkTicketType, level));
		}
	}

	public boolean setChunkForced(ChunkPos pos, boolean forced) {
		ChunkTicket chunkTicket = new ChunkTicket(ChunkTicketType.FORCED, ServerChunkLoadingManager.FORCED_CHUNK_LEVEL);
		return forced ? this.addTicket(pos.toLong(), chunkTicket) : this.removeTicket(pos.toLong(), chunkTicket);
	}

	public LongSet getForcedChunks() {
		return this.forcedChunks;
	}

	private LongSet getAllChunksMatching(Predicate<ChunkTicket> predicate) {
		LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
		ObjectIterator var3 = Long2ObjectMaps.fastIterable(this.tickets).iterator();

		while (var3.hasNext()) {
			Entry<List<ChunkTicket>> entry = (Entry<List<ChunkTicket>>) var3.next();

			for (ChunkTicket chunkTicket : (List<ChunkTicket>) entry.getValue()) {
				if (predicate.test(chunkTicket)) {
					longOpenHashSet.add(entry.getLongKey());
					break;
				}
			}
		}

		return longOpenHashSet;
	}

	@FunctionalInterface
	public interface LevelUpdater {

		void update(long pos, int level, boolean added);
	}

	public interface TicketPredicate {

		boolean test(ChunkTicket ticket, long pos);
	}
}
