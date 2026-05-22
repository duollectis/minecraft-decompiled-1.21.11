package net.minecraft.entity.spawn;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.MoonPhase;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.dimension.DimensionType;

/**
 * Условие спауна, проверяющее яркость луны в заданном диапазоне.
 * Яркость определяется через {@link DimensionType#MOON_SIZES} по индексу текущей фазы луны.
 * Значения яркости: 1.0 (полнолуние) → 0.0 (новолуние) → 1.0 (полнолуние).
 */
public record MoonBrightnessSpawnCondition(NumberRange.DoubleRange range) implements SpawnCondition {

	public static final MapCodec<MoonBrightnessSpawnCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(
				NumberRange.DoubleRange.CODEC
					.fieldOf("range")
					.forGetter(MoonBrightnessSpawnCondition::range)
			)
			.apply(instance, MoonBrightnessSpawnCondition::new)
	);

	/**
	 * Проверяет, попадает ли текущая яркость луны в заданный диапазон.
	 * Яркость вычисляется через атрибут {@link EnvironmentAttributes#MOON_PHASE_VISUAL}
	 * в центре блока позиции спауна.
	 */
	@Override
	public boolean test(SpawnContext context) {
		MoonPhase moonPhase = context.environmentAttributes()
			.getAttributeValue(EnvironmentAttributes.MOON_PHASE_VISUAL, Vec3d.ofCenter(context.pos()));
		float moonBrightness = DimensionType.MOON_SIZES[moonPhase.getIndex()];

		return range.test(moonBrightness);
	}

	@Override
	public MapCodec<MoonBrightnessSpawnCondition> getCodec() {
		return CODEC;
	}
}
