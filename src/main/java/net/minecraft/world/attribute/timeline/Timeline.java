package net.minecraft.world.attribute.timeline;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeModifier;
import net.minecraft.world.attribute.EnvironmentAttributes;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * {@code Timeline}.
 */
public class Timeline {

	public static final Codec<RegistryEntry<Timeline>> REGISTRY_CODEC = RegistryFixedCodec.of(RegistryKeys.TIMELINE);
	private static final Codec<Map<EnvironmentAttribute<?>, TimelineEntry<?, ?>>>
			TRACKS_BY_ATTRIBUTE_CODEC =
			Codec.dispatchedMap(
					EnvironmentAttributes.CODEC, Util.memoize(TimelineEntry::createCodec)
			);
	public static final Codec<Timeline> CODEC = RecordCodecBuilder.<Timeline>create(
			                                                              instance -> instance.group(
					                                                                                  Codecs.POSITIVE_INT.optionalFieldOf("period_ticks").forGetter(timeline -> timeline.periodTicks),
					                                                                                  TRACKS_BY_ATTRIBUTE_CODEC.optionalFieldOf("tracks", Map.of()).forGetter(timeline -> timeline.tracks)
			                                                                                  )
			                                                                                  .apply(instance, Timeline::new)
	                                                              )
	                                                              .validate(Timeline::validate);
	public static final Codec<Timeline>
			NETWORK_CODEC =
			CODEC.xmap(Timeline::retainSyncedAttributes, Timeline::retainSyncedAttributes);
	private final Optional<Integer> periodTicks;
	private final Map<EnvironmentAttribute<?>, TimelineEntry<?, ?>> tracks;

	private static Timeline retainSyncedAttributes(Timeline timeline) {
		Map<EnvironmentAttribute<?>, TimelineEntry<?, ?>>
				map =
				Map.copyOf(Maps.filterKeys(timeline.tracks, EnvironmentAttribute::isSynced));
		return new Timeline(timeline.periodTicks, map);
	}

	Timeline(Optional<Integer> periodTicks, Map<EnvironmentAttribute<?>, TimelineEntry<?, ?>> entries) {
		this.periodTicks = periodTicks;
		this.tracks = entries;
	}

	private static DataResult<Timeline> validate(Timeline timeline) {
		if (timeline.periodTicks.isEmpty()) {
			return DataResult.success(timeline);
		}
		else {
			int i = timeline.periodTicks.get();
			DataResult<Timeline> dataResult = DataResult.success(timeline);

			for (TimelineEntry<?, ?> timelineEntry : timeline.tracks.values()) {
				dataResult =
						dataResult.apply2stable(
								(timelinex, timelineEntryx) -> timelinex,
								TimelineEntry.validateKeyframesInPeriod(timelineEntry, i)
						);
			}

			return dataResult;
		}
	}

	public static Timeline.Builder builder() {
		return new Timeline.Builder();
	}

	public long getEffectiveTimeOfDay(World world) {
		long l = this.getRawTimeOfDay(world);
		return this.periodTicks.isEmpty() ? l : l % this.periodTicks.get().intValue();
	}

	public long getRawTimeOfDay(World world) {
		return world.getTimeOfDay();
	}

	public Optional<Integer> getPeriod() {
		return this.periodTicks;
	}

	public Set<EnvironmentAttribute<?>> getAttributes() {
		return this.tracks.keySet();
	}

	public <Value> TrackAttributeModification<Value, ?> getModification(
			EnvironmentAttribute<Value> attribute,
			LongSupplier timeSupplier
	) {
		TimelineEntry<Value, ?> timelineEntry = (TimelineEntry<Value, ?>) this.tracks.get(attribute);
		if (timelineEntry == null) {
			throw new IllegalStateException("Timeline has no track for " + attribute);
		}
		else {
			return timelineEntry.toModification(attribute, this.periodTicks, timeSupplier);
		}
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private Optional<Integer> periodTicks = Optional.empty();
		private final com.google.common.collect.ImmutableMap.Builder<EnvironmentAttribute<?>, TimelineEntry<?, ?>>
				entries =
				ImmutableMap.builder();

		Builder() {
		}

		public Timeline.Builder period(int periodTicks) {
			this.periodTicks = Optional.of(periodTicks);
			return this;
		}

		public <Value, Argument> Timeline.Builder entry(
				EnvironmentAttribute<Value> attribute,
				EnvironmentAttributeModifier<Value, Argument> modifier,
				Consumer<Track.Builder<Argument>> builderCallback
		) {
			attribute.getType().validate(modifier);
			Track.Builder<Argument> builder = new Track.Builder<>();
			builderCallback.accept(builder);
			this.entries.put(attribute, new TimelineEntry<>(modifier, builder.build()));
			return this;
		}

		public <Value> Timeline.Builder entry(
				EnvironmentAttribute<Value> attribute,
				Consumer<Track.Builder<Value>> builderCallback
		) {
			return this.entry(attribute, EnvironmentAttributeModifier.override(), builderCallback);
		}

		public Timeline build() {
			return new Timeline(this.periodTicks, this.entries.build());
		}
	}
}
