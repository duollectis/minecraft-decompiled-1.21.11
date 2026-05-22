package net.minecraft.world.attribute.timeline;

import net.minecraft.util.math.Interpolator;
import net.minecraft.world.attribute.EnvironmentAttributeFunction;
import net.minecraft.world.attribute.EnvironmentAttributeModifier;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Реализация {@link EnvironmentAttributeFunction.TimeBased}, применяющая анимационный трек
 * к значению атрибута окружения.
 *
 * <p>На каждый игровой тик вычисляет текущий аргумент модификатора через {@link TrackEvaluator},
 * кэшируя результат: если тик не изменился, повторное вычисление не производится.
 * Это позволяет безопасно вызывать {@link #applyTimeBased} несколько раз за один тик
 * без лишних затрат.
 *
 * @param <Value>    тип значения атрибута
 * @param <Argument> тип аргумента модификатора (тип значений в треке)
 */
public class TrackAttributeModification<Value, Argument> implements EnvironmentAttributeFunction.TimeBased<Value> {

	private final EnvironmentAttributeModifier<Value, Argument> modifier;
	private final TrackEvaluator<Argument> evaluator;
	private final LongSupplier timeSupplier;
	private int lastComputedTick;
	private @Nullable Argument lastComputedValue;

	public TrackAttributeModification(
			Optional<Integer> period,
			EnvironmentAttributeModifier<Value, Argument> modifier,
			Track<Argument> track,
			Interpolator<Argument> interpolator,
			LongSupplier timeSupplier
	) {
		this.modifier = modifier;
		this.timeSupplier = timeSupplier;
		this.evaluator = track.createEvaluator(period, interpolator);
	}

	/**
	 * Применяет трек к значению атрибута для заданного тика.
	 * Аргумент модификатора вычисляется один раз за тик и кэшируется.
	 *
	 * @param current текущее значение атрибута
	 * @param tick    номер текущего тика (используется для инвалидации кэша)
	 * @return новое значение атрибута после применения модификатора
	 */
	@Override
	public Value applyTimeBased(Value current, int tick) {
		if (lastComputedValue == null || tick != lastComputedTick) {
			lastComputedTick = tick;
			lastComputedValue = evaluator.get(timeSupplier.getAsLong());
		}

		return modifier.apply(current, lastComputedValue);
	}
}
