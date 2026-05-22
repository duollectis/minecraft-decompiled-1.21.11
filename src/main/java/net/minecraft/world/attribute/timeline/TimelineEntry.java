package net.minecraft.world.attribute.timeline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeModifier;

import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Запись в {@link Timeline}, связывающая конкретный {@link EnvironmentAttributeModifier}
 * с анимационным треком {@link Track}, чьи ключевые кадры задают аргумент модификатора.
 *
 * <p>Codec создаётся динамически через {@link #createCodec}: сначала десериализуется
 * модификатор (или используется override по умолчанию), затем — трек с типом аргумента,
 * соответствующим выбранному модификатору.
 *
 * @param <Value>    тип значения атрибута
 * @param <Argument> тип аргумента модификатора (тип значений в треке)
 */
public record TimelineEntry<Value, Argument>(
		EnvironmentAttributeModifier<Value, Argument> modifier,
		Track<Argument> argumentTrack
) {

	/**
	 * Создаёт codec для записи с произвольным типом аргумента.
	 * Модификатор десериализуется первым и определяет тип аргумента трека.
	 *
	 * @param <Value>   тип значения атрибута
	 * @param attribute атрибут, для которого создаётся codec
	 * @return codec, поддерживающий любой зарегистрированный модификатор атрибута
	 */
	public static <Value> Codec<TimelineEntry<Value, ?>> createCodec(EnvironmentAttribute<Value> attribute) {
		MapCodec<EnvironmentAttributeModifier<Value, ?>> modifierCodec = attribute.getType()
				.modifierCodec()
				.optionalFieldOf("modifier", EnvironmentAttributeModifier.override());

		return modifierCodec.dispatch(
				TimelineEntry::modifier,
				Util.memoize(modifier -> createMapCodec(attribute, modifier))
		);
	}

	private static <Value, Argument> MapCodec<TimelineEntry<Value, Argument>> createMapCodec(
			EnvironmentAttribute<Value> attribute,
			EnvironmentAttributeModifier<Value, Argument> modifier
	) {
		return Track.createCodec(modifier.argumentCodec(attribute))
				.xmap(
						argumentTrack -> new TimelineEntry<>(modifier, argumentTrack),
						TimelineEntry::argumentTrack
				);
	}

	/**
	 * Создаёт {@link TrackAttributeModification} — исполняемый объект, применяющий
	 * данный трек к атрибуту в реальном времени.
	 *
	 * @param attribute    атрибут, к которому применяется модификация
	 * @param period       период шкалы (если задан, время берётся по модулю)
	 * @param timeSupplier поставщик текущего времени в тиках
	 * @return объект модификации, готовый к использованию в {@link TrackEvaluator}
	 */
	public TrackAttributeModification<Value, Argument> toModification(
			EnvironmentAttribute<Value> attribute,
			Optional<Integer> period,
			LongSupplier timeSupplier
	) {
		return new TrackAttributeModification<>(
				period,
				modifier,
				argumentTrack,
				modifier.argumentKeyframeLerp(attribute),
				timeSupplier
		);
	}

	/**
	 * Проверяет, что все ключевые кадры трека укладываются в заданный период.
	 *
	 * @param entry  запись для проверки
	 * @param period длина периода в тиках
	 * @return {@code DataResult.success} если все кадры в диапазоне {@code [0; period]},
	 *         иначе — ошибка с описанием нарушения
	 */
	public static DataResult<TimelineEntry<?, ?>> validateKeyframesInPeriod(TimelineEntry<?, ?> entry, int period) {
		return Track.validateKeyframesInPeriod(entry.argumentTrack(), period).map(track -> entry);
	}
}
