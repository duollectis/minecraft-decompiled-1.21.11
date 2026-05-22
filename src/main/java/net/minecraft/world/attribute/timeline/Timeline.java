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
 * Временна́я шкала (timeline) — именованный набор анимационных треков, каждый из которых
 * управляет одним {@link EnvironmentAttribute} во времени.
 *
 * <p>Timeline может быть периодической (поле {@code period_ticks}): в этом случае время
 * берётся по модулю периода, что позволяет реализовать суточный цикл, лунный цикл и т.п.
 * Без периода треки воспроизводятся один раз от начала до конца.
 *
 * <p>При сетевой синхронизации ({@link #NETWORK_CODEC}) из шкалы удаляются все атрибуты,
 * не помеченные как {@link EnvironmentAttribute#isSynced()}, чтобы не передавать лишние данные.
 */
public class Timeline {

	public static final Codec<RegistryEntry<Timeline>> REGISTRY_CODEC = RegistryFixedCodec.of(RegistryKeys.TIMELINE);

	private static final Codec<Map<EnvironmentAttribute<?>, TimelineEntry<?, ?>>> TRACKS_BY_ATTRIBUTE_CODEC =
			Codec.dispatchedMap(EnvironmentAttributes.CODEC, Util.memoize(TimelineEntry::createCodec));

	public static final Codec<Timeline> CODEC = RecordCodecBuilder.<Timeline>create(
			instance -> instance.group(
					Codecs.POSITIVE_INT.optionalFieldOf("period_ticks").forGetter(timeline -> timeline.periodTicks),
					TRACKS_BY_ATTRIBUTE_CODEC.optionalFieldOf("tracks", Map.of()).forGetter(timeline -> timeline.tracks)
			).apply(instance, Timeline::new)
	).validate(Timeline::validate);

	/** Codec для сетевой передачи: оставляет только синхронизируемые атрибуты. */
	public static final Codec<Timeline> NETWORK_CODEC =
			CODEC.xmap(Timeline::retainSyncedAttributes, Timeline::retainSyncedAttributes);

	private final Optional<Integer> periodTicks;
	private final Map<EnvironmentAttribute<?>, TimelineEntry<?, ?>> tracks;

	Timeline(Optional<Integer> periodTicks, Map<EnvironmentAttribute<?>, TimelineEntry<?, ?>> entries) {
		this.periodTicks = periodTicks;
		this.tracks = entries;
	}

	private static Timeline retainSyncedAttributes(Timeline timeline) {
		Map<EnvironmentAttribute<?>, TimelineEntry<?, ?>> synced =
				Map.copyOf(Maps.filterKeys(timeline.tracks, EnvironmentAttribute::isSynced));

		return new Timeline(timeline.periodTicks, synced);
	}

	/**
	 * Проверяет, что все ключевые кадры каждого трека укладываются в заданный период.
	 * Вызывается автоматически codec'ом при десериализации.
	 */
	private static DataResult<Timeline> validate(Timeline timeline) {
		if (timeline.periodTicks.isEmpty()) {
			return DataResult.success(timeline);
		}

		int period = timeline.periodTicks.get();
		DataResult<Timeline> result = DataResult.success(timeline);

		for (TimelineEntry<?, ?> entry : timeline.tracks.values()) {
			result = result.apply2stable(
					(tl, ignored) -> tl,
					TimelineEntry.validateKeyframesInPeriod(entry, period)
			);
		}

		return result;
	}

	public static Timeline.Builder builder() {
		return new Timeline.Builder();
	}

	/**
	 * Возвращает эффективное время суток с учётом периода шкалы.
	 * Если период задан, возвращает {@code rawTime % period}; иначе — сырое время мира.
	 *
	 * @param world мир, из которого берётся время
	 * @return нормализованное время в тиках
	 */
	public long getEffectiveTimeOfDay(World world) {
		long rawTime = getRawTimeOfDay(world);

		return periodTicks.isEmpty()
				? rawTime
				: rawTime % periodTicks.get().intValue();
	}

	public long getRawTimeOfDay(World world) {
		return world.getTimeOfDay();
	}

	public Optional<Integer> getPeriod() {
		return periodTicks;
	}

	public Set<EnvironmentAttribute<?>> getAttributes() {
		return tracks.keySet();
	}

	/**
	 * Возвращает модификацию атрибута для данной шкалы.
	 * Бросает {@link IllegalStateException}, если трек для атрибута не зарегистрирован.
	 *
	 * @param <Value>      тип значения атрибута
	 * @param attribute    атрибут, для которого нужна модификация
	 * @param timeSupplier поставщик текущего времени (в тиках)
	 * @return объект {@link TrackAttributeModification}, применяющий трек к атрибуту
	 */
	@SuppressWarnings("unchecked")
	public <Value> TrackAttributeModification<Value, ?> getModification(
			EnvironmentAttribute<Value> attribute,
			LongSupplier timeSupplier
	) {
		TimelineEntry<Value, ?> entry = (TimelineEntry<Value, ?>) tracks.get(attribute);

		if (entry == null) {
			throw new IllegalStateException("Timeline has no track for " + attribute);
		}

		return entry.toModification(attribute, periodTicks, timeSupplier);
	}

	/**
	 * Строитель {@link Timeline}.
	 * Позволяет декларативно задавать период и треки атрибутов.
	 */
	public static class Builder {

		private Optional<Integer> periodTicks = Optional.empty();
		private final ImmutableMap.Builder<EnvironmentAttribute<?>, TimelineEntry<?, ?>> entries =
				ImmutableMap.builder();

		Builder() {
		}

		public Timeline.Builder period(int periodTicks) {
			this.periodTicks = Optional.of(periodTicks);
			return this;
		}

		/**
		 * Добавляет трек для атрибута с явно указанным модификатором.
		 *
		 * @param <Value>         тип значения атрибута
		 * @param <Argument>      тип аргумента модификатора
		 * @param attribute       целевой атрибут
		 * @param modifier        модификатор, применяемый к значению атрибута
		 * @param builderCallback лямбда для наполнения трека ключевыми кадрами
		 */
		public <Value, Argument> Timeline.Builder entry(
				EnvironmentAttribute<Value> attribute,
				EnvironmentAttributeModifier<Value, Argument> modifier,
				Consumer<Track.Builder<Argument>> builderCallback
		) {
			attribute.getType().validate(modifier);
			Track.Builder<Argument> trackBuilder = new Track.Builder<>();
			builderCallback.accept(trackBuilder);
			entries.put(attribute, new TimelineEntry<>(modifier, trackBuilder.build()));
			return this;
		}

		/**
		 * Добавляет трек для атрибута с модификатором-перезаписью (override).
		 * Значение атрибута полностью заменяется значением из трека.
		 *
		 * @param <Value>         тип значения атрибута
		 * @param attribute       целевой атрибут
		 * @param builderCallback лямбда для наполнения трека ключевыми кадрами
		 */
		public <Value> Timeline.Builder entry(
				EnvironmentAttribute<Value> attribute,
				Consumer<Track.Builder<Value>> builderCallback
		) {
			return entry(attribute, EnvironmentAttributeModifier.override(), builderCallback);
		}

		public Timeline build() {
			return new Timeline(periodTicks, entries.build());
		}
	}
}
