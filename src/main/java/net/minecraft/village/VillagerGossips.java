package net.minecraft.village;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
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
 * {@code VillagerGossips}.
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
		gossips.forEach(gossip -> this.getReputationFor(gossip.target).associatedGossip.put(gossip.type, gossip.value));
	}

	@Debug
	public Map<UUID, Object2IntMap<VillagerGossipType>> getEntityReputationAssociatedGossips() {
		Map<UUID, Object2IntMap<VillagerGossipType>> map = Maps.newHashMap();
		this.entityReputation.keySet().forEach(uuid -> {
			VillagerGossips.Reputation reputation = this.entityReputation.get(uuid);
			map.put(uuid, reputation.associatedGossip);
		});
		return map;
	}

	public void decay() {
		Iterator<VillagerGossips.Reputation> iterator = this.entityReputation.values().iterator();

		while (iterator.hasNext()) {
			VillagerGossips.Reputation reputation = iterator.next();
			reputation.decay();
			if (reputation.isObsolete()) {
				iterator.remove();
			}
		}
	}

	private Stream<VillagerGossips.GossipEntry> entries() {
		return this.entityReputation.entrySet().stream().flatMap(entry -> entry.getValue().entriesFor(entry.getKey()));
	}

	private Collection<VillagerGossips.GossipEntry> pickGossips(Random random, int count) {
		List<VillagerGossips.GossipEntry> list = this.entries().toList();
		if (list.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			int[] is = new int[list.size()];
			int i = 0;

			for (int j = 0; j < list.size(); j++) {
				VillagerGossips.GossipEntry gossipEntry = list.get(j);
				i += Math.abs(gossipEntry.getValue());
				is[j] = i - 1;
			}

			Set<VillagerGossips.GossipEntry> set = Sets.newIdentityHashSet();

			for (int k = 0; k < count; k++) {
				int l = random.nextInt(i);
				int m = Arrays.binarySearch(is, l);
				set.add(list.get(m < 0 ? -m - 1 : m));
			}

			return set;
		}
	}

	private VillagerGossips.Reputation getReputationFor(UUID target) {
		return this.entityReputation.computeIfAbsent(target, uuid -> new VillagerGossips.Reputation());
	}

	public void shareGossipFrom(VillagerGossips from, Random random, int count) {
		Collection<VillagerGossips.GossipEntry> collection = from.pickGossips(random, count);
		collection.forEach(gossip -> {
			int i = gossip.value - gossip.type.shareDecrement;
			if (i >= 2) {
				this.getReputationFor(gossip.target).associatedGossip.mergeInt(gossip.type, i, VillagerGossips::max);
			}
		});
	}

	public int getReputationFor(UUID target, Predicate<VillagerGossipType> gossipTypeFilter) {
		VillagerGossips.Reputation reputation = this.entityReputation.get(target);
		return reputation != null ? reputation.getValueFor(gossipTypeFilter) : 0;
	}

	public long getReputationCount(VillagerGossipType type, DoublePredicate predicate) {
		return this.entityReputation
				.values()
				.stream()
				.filter(reputation -> predicate.test(
						reputation.associatedGossip.getOrDefault(type, 0) * type.multiplier))
				.count();
	}

	public void startGossip(UUID target, VillagerGossipType type, int value) {
		VillagerGossips.Reputation reputation = this.getReputationFor(target);
		reputation.associatedGossip.mergeInt(type, value, (left, right) -> this.mergeReputation(type, left, right));
		reputation.clamp(type);
		if (reputation.isObsolete()) {
			this.entityReputation.remove(target);
		}
	}

	public void removeGossip(UUID target, VillagerGossipType type, int value) {
		this.startGossip(target, type, -value);
	}

	public void remove(UUID target, VillagerGossipType type) {
		VillagerGossips.Reputation reputation = this.entityReputation.get(target);
		if (reputation != null) {
			reputation.remove(type);
			if (reputation.isObsolete()) {
				this.entityReputation.remove(target);
			}
		}
	}

	public void remove(VillagerGossipType type) {
		Iterator<VillagerGossips.Reputation> iterator = this.entityReputation.values().iterator();

		while (iterator.hasNext()) {
			VillagerGossips.Reputation reputation = iterator.next();
			reputation.remove(type);
			if (reputation.isObsolete()) {
				iterator.remove();
			}
		}
	}

	public void clear() {
		this.entityReputation.clear();
	}

	public void add(VillagerGossips gossips) {
		gossips.entityReputation.forEach((target, reputation) -> this.getReputationFor(target).associatedGossip.putAll(
				reputation.associatedGossip));
	}

	private static int max(int left, int right) {
		return Math.max(left, right);
	}

	private int mergeReputation(VillagerGossipType type, int left, int right) {
		int i = left + right;
		return i > type.maxValue ? Math.max(type.maxValue, left) : i;
	}

	public VillagerGossips copy() {
		VillagerGossips villagerGossips = new VillagerGossips();
		villagerGossips.add(this);
		return villagerGossips;
	}

	/**
	 * {@code GossipEntry}.
	 */
	record GossipEntry(UUID target, VillagerGossipType type, int value) {

		public static final Codec<VillagerGossips.GossipEntry> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Uuids.INT_STREAM_CODEC.fieldOf("Target").forGetter(VillagerGossips.GossipEntry::target),
						                    VillagerGossipType.CODEC.fieldOf("Type").forGetter(VillagerGossips.GossipEntry::type),
						                    Codecs.POSITIVE_INT.fieldOf("Value").forGetter(VillagerGossips.GossipEntry::value)
				                    )
				                    .apply(instance, VillagerGossips.GossipEntry::new)
		);

		public int getValue() {
			return this.value * this.type.multiplier;
		}
	}

	/**
	 * {@code Reputation}.
	 */
	static class Reputation {

		final Object2IntMap<VillagerGossipType> associatedGossip = new Object2IntOpenHashMap();

		public int getValueFor(Predicate<VillagerGossipType> gossipTypeFilter) {
			return this.associatedGossip
					.object2IntEntrySet()
					.stream()
					.filter(entry -> gossipTypeFilter.test((VillagerGossipType) entry.getKey()))
					.mapToInt(entry -> entry.getIntValue() * ((VillagerGossipType) entry.getKey()).multiplier)
					.sum();
		}

		public Stream<VillagerGossips.GossipEntry> entriesFor(UUID target) {
			return this.associatedGossip
					.object2IntEntrySet()
					.stream()
					.map(entry -> new VillagerGossips.GossipEntry(
							target,
							(VillagerGossipType) entry.getKey(),
							entry.getIntValue()
					));
		}

		public void decay() {
			ObjectIterator<Entry<VillagerGossipType>>
					objectIterator =
					this.associatedGossip.object2IntEntrySet().iterator();

			while (objectIterator.hasNext()) {
				Entry<VillagerGossipType> entry = (Entry<VillagerGossipType>) objectIterator.next();
				int i = entry.getIntValue() - ((VillagerGossipType) entry.getKey()).decay;
				if (i < 2) {
					objectIterator.remove();
				}
				else {
					entry.setValue(i);
				}
			}
		}

		public boolean isObsolete() {
			return this.associatedGossip.isEmpty();
		}

		public void clamp(VillagerGossipType gossipType) {
			int i = this.associatedGossip.getInt(gossipType);
			if (i > gossipType.maxValue) {
				this.associatedGossip.put(gossipType, gossipType.maxValue);
			}

			if (i < 2) {
				this.remove(gossipType);
			}
		}

		public void remove(VillagerGossipType gossipType) {
			this.associatedGossip.removeInt(gossipType);
		}
	}
}
