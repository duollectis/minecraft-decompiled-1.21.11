package net.minecraft.entity.ai;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Angriness;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Менеджер гнева Вардена: отслеживает уровни злости к каждому подозреваемому,
 * уменьшает их со временем и определяет главного подозреваемого.
 * Поддерживает сериализацию через UUID для существ, покинувших измерение или чанк.
 */
public class WardenAngerManager {

	@VisibleForTesting
	protected static final int UPDATE_TIMER_MAX = 2;
	@VisibleForTesting
	protected static final int MAX_ANGER = 150;

	private static final int ANGER_DECREASE_PER_TICK = 1;
	private static final Codec<Pair<UUID, Integer>> SUSPECT_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Uuids.INT_STREAM_CODEC.fieldOf("uuid").forGetter(Pair::getFirst),
					Codecs.NON_NEGATIVE_INT.fieldOf("anger").forGetter(Pair::getSecond)
			).apply(instance, Pair::of)
	);

	private final Predicate<Entity> suspectPredicate;
	@VisibleForTesting
	protected final ArrayList<Entity> suspects;
	private final SuspectComparator suspectComparator;
	@VisibleForTesting
	protected final Object2IntMap<Entity> suspectsToAngerLevel;
	@VisibleForTesting
	protected final Object2IntMap<UUID> suspectUuidsToAngerLevel;

	private int updateTimer = MathHelper.nextBetween(Random.create(), 0, UPDATE_TIMER_MAX);
	int primeAnger;

	/**
	 * Создаёт кодек для сериализации менеджера гнева.
	 * Список подозреваемых хранится как пары (UUID, уровень злости).
	 *
	 * @param suspectPredicate предикат допустимости подозреваемого (например, живой игрок)
	 */
	public static Codec<WardenAngerManager> createCodec(Predicate<Entity> suspectPredicate) {
		return RecordCodecBuilder.create(
				instance -> instance
						.group(
								SUSPECT_CODEC.listOf()
										.fieldOf("suspects")
										.orElse(Collections.emptyList())
										.forGetter(WardenAngerManager::getSuspects)
						)
						.apply(
								instance,
								suspectUuidsToAngerLevel -> new WardenAngerManager(suspectPredicate, suspectUuidsToAngerLevel)
						)
		);
	}

	public WardenAngerManager(Predicate<Entity> suspectPredicate, List<Pair<UUID, Integer>> suspectUuidsToAngerLevel) {
		this.suspectPredicate = suspectPredicate;
		suspects = new ArrayList<>();
		suspectComparator = new SuspectComparator(this);
		suspectsToAngerLevel = new Object2IntOpenHashMap<>();
		this.suspectUuidsToAngerLevel = new Object2IntOpenHashMap<>(suspectUuidsToAngerLevel.size());
		suspectUuidsToAngerLevel.forEach(suspect -> this.suspectUuidsToAngerLevel.put(suspect.getFirst(), suspect.getSecond()));
	}

	private List<Pair<UUID, Integer>> getSuspects() {
		return Streams.concat(
				suspects.stream()
						.map(suspect -> Pair.of(suspect.getUuid(), suspectsToAngerLevel.getInt(suspect))),
				suspectUuidsToAngerLevel.object2IntEntrySet()
						.stream()
						.map(suspect -> Pair.of(suspect.getKey(), suspect.getIntValue()))
		).collect(Collectors.toList());
	}

	/**
	 * Обновляет состояние гнева за один тик: уменьшает злость ко всем подозреваемым,
	 * удаляет тех, чья злость упала до нуля, и переносит злость к UUID для ушедших существ.
	 *
	 * @param world мир для поиска существ по UUID
	 * @param suspectPredicate предикат допустимости подозреваемого
	 */
	public void tick(ServerWorld world, Predicate<Entity> suspectPredicate) {
		updateTimer--;

		if (updateTimer <= 0) {
			updateSuspectsMap(world);
			updateTimer = UPDATE_TIMER_MAX;
		}

		ObjectIterator<Entry<UUID>> uuidIterator = suspectUuidsToAngerLevel.object2IntEntrySet().iterator();

		while (uuidIterator.hasNext()) {
			Entry<UUID> entry = uuidIterator.next();
			int anger = entry.getIntValue();

			if (anger <= ANGER_DECREASE_PER_TICK) {
				uuidIterator.remove();
			} else {
				entry.setValue(anger - ANGER_DECREASE_PER_TICK);
			}
		}

		ObjectIterator<Entry<Entity>> entityIterator = suspectsToAngerLevel.object2IntEntrySet().iterator();

		while (entityIterator.hasNext()) {
			Entry<Entity> entry = entityIterator.next();
			int anger = entry.getIntValue();
			Entity entity = entry.getKey();
			Entity.RemovalReason removalReason = entity.getRemovalReason();

			if (anger > ANGER_DECREASE_PER_TICK && suspectPredicate.test(entity) && removalReason == null) {
				entry.setValue(anger - ANGER_DECREASE_PER_TICK);
			} else {
				suspects.remove(entity);
				entityIterator.remove();

				if (anger > ANGER_DECREASE_PER_TICK && removalReason != null) {
					switch (removalReason) {
						case CHANGED_DIMENSION, UNLOADED_TO_CHUNK, UNLOADED_WITH_PLAYER ->
								suspectUuidsToAngerLevel.put(entity.getUuid(), anger - ANGER_DECREASE_PER_TICK);
					}
				}
			}
		}

		updatePrimeAnger();
	}

	private void updatePrimeAnger() {
		primeAnger = 0;
		suspects.sort(suspectComparator);

		if (suspects.size() == 1) {
			primeAnger = suspectsToAngerLevel.getInt(suspects.get(0));
		}
	}

	private void updateSuspectsMap(ServerWorld world) {
		ObjectIterator<Entry<UUID>> iterator = suspectUuidsToAngerLevel.object2IntEntrySet().iterator();

		while (iterator.hasNext()) {
			Entry<UUID> entry = iterator.next();
			Entity entity = world.getEntity(entry.getKey());

			if (entity != null) {
				suspectsToAngerLevel.put(entity, entry.getIntValue());
				suspects.add(entity);
				iterator.remove();
			}
		}
	}

	/**
	 * Увеличивает уровень злости к существу на {@code amount}, не превышая {@link #MAX_ANGER}.
	 * Если существо ранее отслеживалось только по UUID — переносит накопленную злость.
	 *
	 * @param entity подозреваемое существо
	 * @param amount количество добавляемой злости
	 * @return итоговый уровень злости к существу
	 */
	public int increaseAngerAt(Entity entity, int amount) {
		boolean isNew = !suspectsToAngerLevel.containsKey(entity);
		int anger = suspectsToAngerLevel.computeInt(
				entity,
				(suspect, current) -> Math.min(MAX_ANGER, (current == null ? 0 : current) + amount)
		);

		if (isNew) {
			int uuidAnger = suspectUuidsToAngerLevel.removeInt(entity.getUuid());
			anger += uuidAnger;
			suspectsToAngerLevel.put(entity, anger);
			suspects.add(entity);
		}

		updatePrimeAnger();
		return anger;
	}

	public void removeSuspect(Entity entity) {
		suspectsToAngerLevel.removeInt(entity);
		suspects.remove(entity);
		updatePrimeAnger();
	}

	private @Nullable Entity getPrimeSuspectInternal() {
		return suspects.stream().filter(suspectPredicate).findFirst().orElse(null);
	}

	public int getAngerFor(@Nullable Entity entity) {
		return entity == null ? primeAnger : suspectsToAngerLevel.getInt(entity);
	}

	public Optional<LivingEntity> getPrimeSuspect() {
		return Optional.ofNullable(getPrimeSuspectInternal())
				.filter(suspect -> suspect instanceof LivingEntity)
				.map(suspect -> (LivingEntity) suspect);
	}

	/**
	 * Компаратор подозреваемых: сначала злые, затем игроки, затем по убыванию злости.
	 * Побочный эффект: обновляет {@code primeAnger} при каждом сравнении.
	 */
	@VisibleForTesting
	protected record SuspectComparator(WardenAngerManager angerManagement) implements Comparator<Entity> {

		@Override
		public int compare(Entity entity, Entity other) {
			if (entity.equals(other)) {
				return 0;
			}

			int angerA = angerManagement.suspectsToAngerLevel.getOrDefault(entity, 0);
			int angerB = angerManagement.suspectsToAngerLevel.getOrDefault(other, 0);
			angerManagement.primeAnger = Math.max(angerManagement.primeAnger, Math.max(angerA, angerB));

			boolean isAngryA = Angriness.getForAnger(angerA).isAngry();
			boolean isAngryB = Angriness.getForAnger(angerB).isAngry();

			if (isAngryA != isAngryB) {
				return isAngryA ? -1 : 1;
			}

			boolean isPlayerA = entity instanceof PlayerEntity;
			boolean isPlayerB = other instanceof PlayerEntity;

			if (isPlayerA != isPlayerB) {
				return isPlayerA ? -1 : 1;
			}

			return Integer.compare(angerB, angerA);
		}
	}
}
