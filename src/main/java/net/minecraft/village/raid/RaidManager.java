package net.minecraft.village.raid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;

/**
 * Менеджер рейдов серверного мира — хранит все активные рейды и управляет их жизненным циклом.
 * <p>
 * Сохраняется как {@link PersistentState} под именем {@code "raids"} (или {@code "raids_end"}
 * для Края). Каждый тик обновляет все активные рейды и удаляет завершённые.
 */
public class RaidManager extends PersistentState {

	private static final int DIRTY_INTERVAL_TICKS = 200;
	private static final int VILLAGE_SEARCH_RADIUS = 64;

	public static final Codec<RaidManager> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					RaidManager.RaidWithId.CODEC
							.listOf()
							.optionalFieldOf("raids", List.of())
							.forGetter(manager -> manager.raids
									.int2ObjectEntrySet()
									.stream()
									.map(RaidManager.RaidWithId::fromMapEntry)
									.toList()),
					Codec.INT.fieldOf("next_id").forGetter(manager -> manager.nextAvailableId),
					Codec.INT.fieldOf("tick").forGetter(manager -> manager.currentTime)
			).apply(instance, RaidManager::new)
	);

	public static final PersistentStateType<RaidManager> STATE_TYPE = new PersistentStateType<>(
			"raids", RaidManager::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS
	);

	public static final PersistentStateType<RaidManager> END_STATE_TYPE = new PersistentStateType<>(
			"raids_end", RaidManager::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS
	);

	private final Int2ObjectMap<Raid> raids = new Int2ObjectOpenHashMap<>();
	private int nextAvailableId = 1;
	private int currentTime;

	public static PersistentStateType<RaidManager> getPersistentStateType(RegistryEntry<DimensionType> dimensionType) {
		return dimensionType.matchesKey(DimensionTypes.THE_END) ? END_STATE_TYPE : STATE_TYPE;
	}

	public RaidManager() {
		markDirty();
	}

	private RaidManager(List<RaidManager.RaidWithId> raidList, int nextAvailableId, int currentTime) {
		for (RaidManager.RaidWithId entry : raidList) {
			raids.put(entry.id(), entry.raid());
		}

		this.nextAvailableId = nextAvailableId;
		this.currentTime = currentTime;
	}

	public @Nullable Raid getRaid(int id) {
		return raids.get(id);
	}

	/**
	 * Ищет идентификатор рейда по его объекту.
	 *
	 * @param raid рейд для поиска
	 * @return идентификатор рейда или пустой {@link OptionalInt}
	 */
	public OptionalInt getRaidId(Raid raid) {
		for (Int2ObjectMap.Entry<Raid> entry : raids.int2ObjectEntrySet()) {
			if (entry.getValue() == raid) {
				return OptionalInt.of(entry.getIntKey());
			}
		}

		return OptionalInt.empty();
	}

	public void tick(ServerWorld world) {
		currentTime++;
		Iterator<Raid> iterator = raids.values().iterator();

		while (iterator.hasNext()) {
			Raid raid = iterator.next();

			if (!world.getGameRules().getValue(GameRules.DISABLE_RAIDS)) {
				raid.invalidate();
			}

			if (raid.hasStopped()) {
				iterator.remove();
				markDirty();
			} else {
				raid.tick(world);
			}
		}

		if (currentTime % DIRTY_INTERVAL_TICKS == 0) {
			markDirty();
		}
	}

	public static boolean isValidRaiderFor(RaiderEntity raider) {
		return raider.isAlive() && raider.canJoinRaid() && raider.getDespawnCounter() <= Raid.MAX_DESPAWN_COUNTER;
	}

	/**
	 * Запускает или усиливает рейд для игрока с эффектом «Предзнаменование рейда».
	 * <p>
	 * Вычисляет центр деревни как среднее положение всех занятых точек интереса
	 * в радиусе {@link #VILLAGE_SEARCH_RADIUS} блоков. Если деревня не найдена —
	 * использует позицию игрока.
	 *
	 * @param player игрок, инициирующий рейд
	 * @param pos    позиция игрока
	 * @return созданный или усиленный рейд, либо {@code null} если рейд не может начаться
	 */
	public @Nullable Raid startRaid(ServerPlayerEntity player, BlockPos pos) {
		if (player.isSpectator()) {
			return null;
		}

		ServerWorld serverWorld = player.getEntityWorld();

		if (!serverWorld.getGameRules().getValue(GameRules.DISABLE_RAIDS)) {
			return null;
		}

		if (!serverWorld.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.CAN_START_RAID_GAMEPLAY, pos)) {
			return null;
		}

		List<PointOfInterest> nearbyPois = serverWorld.getPointOfInterestStorage()
				.getInCircle(
						poiType -> poiType.isIn(PointOfInterestTypeTags.VILLAGE),
						pos,
						VILLAGE_SEARCH_RADIUS,
						PointOfInterestStorage.OccupationStatus.IS_OCCUPIED
				)
				.toList();

		BlockPos villageCenter = calculateVillageCenter(nearbyPois, pos);
		Raid raid = getOrCreateRaid(serverWorld, villageCenter);

		if (!raid.hasStarted() && !raids.containsValue(raid)) {
			raids.put(nextId(), raid);
		}

		if (!raid.hasStarted() || raid.getBadOmenLevel() < raid.getMaxAcceptableBadOmenLevel()) {
			raid.start(player);
		}

		markDirty();
		return raid;
	}

	private BlockPos calculateVillageCenter(List<PointOfInterest> pois, BlockPos fallback) {
		if (pois.isEmpty()) {
			return fallback;
		}

		Vec3d sum = Vec3d.ZERO;

		for (PointOfInterest poi : pois) {
			BlockPos poiPos = poi.getPos();
			sum = sum.add(poiPos.getX(), poiPos.getY(), poiPos.getZ());
		}

		return BlockPos.ofFloored(sum.multiply(1.0 / pois.size()));
	}

	private Raid getOrCreateRaid(ServerWorld world, BlockPos pos) {
		Raid existing = world.getRaidAt(pos);
		return existing != null ? existing : new Raid(pos, world.getDifficulty());
	}

	public static RaidManager fromNbt(NbtCompound nbt) {
		return CODEC.parse(NbtOps.INSTANCE, nbt).resultOrPartial().orElseGet(RaidManager::new);
	}

	private int nextId() {
		return ++nextAvailableId;
	}

	/**
	 * Ищет ближайший активный рейд в заданном радиусе от позиции.
	 *
	 * @param pos            позиция для поиска
	 * @param searchDistance максимальное расстояние поиска
	 * @return ближайший активный рейд или {@code null}
	 */
	public @Nullable Raid getRaidAt(BlockPos pos, int searchDistance) {
		Raid closest = null;
		double closestDistance = searchDistance;

		for (Raid raid : raids.values()) {
			double distance = raid.getCenter().getSquaredDistance(pos);

			if (raid.isActive() && distance < closestDistance) {
				closest = raid;
				closestDistance = distance;
			}
		}

		return closest;
	}

	@Debug
	public List<BlockPos> getRaidCenters(ChunkPos chunkPos) {
		return raids.values().stream().map(Raid::getCenter).filter(chunkPos::contains).toList();
	}

	record RaidWithId(int id, Raid raid) {

		public static final Codec<RaidManager.RaidWithId> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Codec.INT.fieldOf("id").forGetter(RaidManager.RaidWithId::id),
						Raid.CODEC.forGetter(RaidManager.RaidWithId::raid)
				).apply(instance, RaidManager.RaidWithId::new)
		);

		public static RaidManager.RaidWithId fromMapEntry(Int2ObjectMap.Entry<Raid> entry) {
			return new RaidManager.RaidWithId(entry.getIntKey(), entry.getValue());
		}
	}
}
