package net.minecraft.village;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.util.Uuids;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.random.Random;

import java.util.*;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Хранилище слухов жителя деревни о конкретных сущностях.
 * <p>
 * Слухи влияют на репутацию игрока у жителя: положительные увеличивают скидки,
 * отрицательные — повышают цены. Слухи затухают со временем через {@link #decay()}
 * и распространяются между жителями через {@link #shareGossipFrom}.
 */
public class VillagerGossips {

	public static final Codec<VillagerGossips> CODEC = VillagerGossips.GossipEntry.CODEC
			.listOf()
			.xmap(VillagerGossips::new, gossips -> gossips.entries().toList());

	public static final int MAX_GOSSIP_ENTRIES = 2;

	private final Map<UUID, VillagerGossips.Reputation> entityReputation = new HashMap<>();

	public VillagerGossips() {
	}

	private VillagerGossips(List<VillagerGossips.GossipEntry> gossips) {
		gossips.forEach(gossip -> getOrCreateReputation(gossip.target).associatedGossip.put(gossip.type, gossip.value));
	}

	@Debug
	public Map<UUID, Object2IntMap<VillagerGossipType>> getEntityReputationAssociatedGossips() {
		Map<UUID, Object2IntMap<VillagerGossipType>> result = Maps.newHashMap();

		entityReputation.forEach((uuid, reputation) -> result.put(uuid, reputation.associatedGossip));

		return result;
	}

	/**
	 * Уменьшает все слухи на величину их затухания. Удаляет слухи, упавшие ниже минимума.
	 */
	public void decay() {
		Iterator<VillagerGossips.Reputation> iterator = entityReputation.values().iterator();

		while (iterator.hasNext()) {
			VillagerGossips.Reputation reputation = iterator.next();
			reputation.decay();

			if (reputation.isObsolete()) {
				iterator.remove();
			}
		}
	}

	private Stream<VillagerGossips.GossipEntry> entries() {
		return entityReputation.entrySet()
				.stream()
				.flatMap(entry -> entry.getValue().entriesFor(entry.getKey()));
	}

	/**
	 * Выбирает случайные слухи для передачи другому жителю.
	 * Вероятность выбора слуха пропорциональна его абсолютному значению.
	 *
	 * @param random генератор случайных чисел
	 * @param count  количество слухов для выбора
	 * @return набор выбранных слухов
	 */
	private Collection<VillagerGossips.GossipEntry> pickGossips(Random random, int count) {
		List<VillagerGossips.GossipEntry> allEntries = entries().toList();

		if (allEntries.isEmpty()) {
			return Collections.emptyList();
		}

		// Строим массив накопленных весов для взвешенного случайного выбора
		int[] cumulativeWeights = new int[allEntries.size()];
		int totalWeight = 0;

		for (int index = 0; index < allEntries.size(); index++) {
			totalWeight += Math.abs(allEntries.get(index).getValue());
			cumulativeWeights[index] = totalWeight - 1;
		}

		Set<VillagerGossips.GossipEntry> selected = Sets.newIdentityHashSet();

		for (int pick = 0; pick < count; pick++) {
			int randomWeight = random.nextInt(totalWeight);
			int foundIndex = Arrays.binarySearch(cumulativeWeights, randomWeight);
			selected.add(allEntries.get(foundIndex < 0 ? -foundIndex - 1 : foundIndex));
		}

		return selected;
	}

	private VillagerGossips.Reputation getOrCreateReputation(UUID target) {
		return entityReputation.computeIfAbsent(target, uuid -> new VillagerGossips.Reputation());
	}

	/**
	 * Перенимает слухи от другого жителя. Слух уменьшается на {@code shareDecrement} при передаче.
	 * Слухи со значением ниже {@link VillagerGossipType#MIN_GOSSIP_VALUE} не передаются.
	 *
	 * @param from   источник слухов
	 * @param random генератор случайных чисел
	 * @param count  количество слухов для перенятия
	 */
	public void shareGossipFrom(VillagerGossips from, Random random, int count) {
		Collection<VillagerGossips.GossipEntry> picked = from.pickGossips(random, count);

		picked.forEach(gossip -> {
			int sharedValue = gossip.value - gossip.type.shareDecrement;

			if (sharedValue >= VillagerGossipType.MIN_GOSSIP_VALUE) {
				getOrCreateReputation(gossip.target).associatedGossip.mergeInt(gossip.type, sharedValue, VillagerGossips::max);
			}
		});
	}

	/**
	 * Возвращает суммарную репутацию сущности с учётом фильтра типов слухов.
	 *
	 * @param target           UUID целевой сущности
	 * @param gossipTypeFilter фильтр типов слухов для учёта
	 * @return суммарная репутация или 0, если слухов нет
	 */
	public int getReputationFor(UUID target, Predicate<VillagerGossipType> gossipTypeFilter) {
		VillagerGossips.Reputation reputation = entityReputation.get(target);
		return reputation != null ? reputation.getValueFor(gossipTypeFilter) : 0;
	}

	public long getReputationCount(VillagerGossipType type, DoublePredicate predicate) {
		return entityReputation.values()
				.stream()
				.filter(reputation -> predicate.test(
						reputation.associatedGossip.getOrDefault(type, 0) * type.multiplier
				))
				.count();
	}

	public void startGossip(UUID target, VillagerGossipType type, int value) {
		VillagerGossips.Reputation reputation = getOrCreateReputation(target);
		reputation.associatedGossip.mergeInt(type, value, (left, right) -> mergeReputation(type, left, right));
		reputation.clamp(type);

		if (reputation.isObsolete()) {
			entityReputation.remove(target);
		}
	}

	public void removeGossip(UUID target, VillagerGossipType type, int value) {
		startGossip(target, type, -value);
	}

	public void remove(UUID target, VillagerGossipType type) {
		VillagerGossips.Reputation reputation = entityReputation.get(target);

		if (reputation == null) {
			return;
		}

		reputation.remove(type);

		if (reputation.isObsolete()) {
			entityReputation.remove(target);
		}
	}

	public void remove(VillagerGossipType type) {
		Iterator<VillagerGossips.Reputation> iterator = entityReputation.values().iterator();

		while (iterator.hasNext()) {
			VillagerGossips.Reputation reputation = iterator.next();
			reputation.remove(type);

			if (reputation.isObsolete()) {
				iterator.remove();
			}
		}
	}

	public void clear() {
		entityReputation.clear();
	}

	public void add(VillagerGossips gossips) {
		gossips.entityReputation.forEach(
				(target, reputation) -> getOrCreateReputation(target).associatedGossip.putAll(reputation.associatedGossip)
		);
	}

	private static int max(int left, int right) {
		return Math.max(left, right);
	}

	private int mergeReputation(VillagerGossipType type, int left, int right) {
		int merged = left + right;
		return merged > type.maxValue ? Math.max(type.maxValue, left) : merged;
	}

	public VillagerGossips copy() {
		VillagerGossips copy = new VillagerGossips();
		copy.add(this);
		return copy;
	}

	record GossipEntry(UUID target, VillagerGossipType type, int value) {

		public static final Codec<VillagerGossips.GossipEntry> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Uuids.INT_STREAM_CODEC.fieldOf("Target").forGetter(VillagerGossips.GossipEntry::target),
						VillagerGossipType.CODEC.fieldOf("Type").forGetter(VillagerGossips.GossipEntry::type),
						Codecs.POSITIVE_INT.fieldOf("Value").forGetter(VillagerGossips.GossipEntry::value)
				).apply(instance, VillagerGossips.GossipEntry::new)
		);

		public int getValue() {
			return value * type.multiplier;
		}
	}

	static class Reputation {

		final Object2IntMap<VillagerGossipType> associatedGossip = new Object2IntOpenHashMap<>();

		public int getValueFor(Predicate<VillagerGossipType> gossipTypeFilter) {
			return associatedGossip.object2IntEntrySet()
					.stream()
					.filter(entry -> gossipTypeFilter.test(entry.getKey()))
					.mapToInt(entry -> entry.getIntValue() * entry.getKey().multiplier)
					.sum();
		}

		public Stream<VillagerGossips.GossipEntry> entriesFor(UUID target) {
			return associatedGossip.object2IntEntrySet()
					.stream()
					.map(entry -> new VillagerGossips.GossipEntry(target, entry.getKey(), entry.getIntValue()));
		}

		public void decay() {
			ObjectIterator<Object2IntMap.Entry<VillagerGossipType>> iterator =
					associatedGossip.object2IntEntrySet().iterator();

			while (iterator.hasNext()) {
				Object2IntMap.Entry<VillagerGossipType> entry = iterator.next();
				int decayed = entry.getIntValue() - entry.getKey().decay;

				if (decayed < VillagerGossipType.MIN_GOSSIP_VALUE) {
					iterator.remove();
				} else {
					entry.setValue(decayed);
				}
			}
		}

		public boolean isObsolete() {
			return associatedGossip.isEmpty();
		}

		public void clamp(VillagerGossipType gossipType) {
			int value = associatedGossip.getInt(gossipType);

			if (value > gossipType.maxValue) {
				associatedGossip.put(gossipType, gossipType.maxValue);
			}

			if (value < VillagerGossipType.MIN_GOSSIP_VALUE) {
				remove(gossipType);
			}
		}

		public void remove(VillagerGossipType gossipType) {
			associatedGossip.removeInt(gossipType);
		}
	}
}
