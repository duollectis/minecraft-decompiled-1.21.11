package net.minecraft.entity.spawn;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.MoonPhase;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.dimension.DimensionType;

/**
 * {@code MoonBrightnessSpawnCondition}.
 */
public record MoonBrightnessSpawnCondition(NumberRange.DoubleRange range) implements SpawnCondition {

	public static final MapCodec<MoonBrightnessSpawnCondition> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(NumberRange.DoubleRange.CODEC
							.fieldOf("range")
							.forGetter(MoonBrightnessSpawnCondition::range))
					.apply(instance, MoonBrightnessSpawnCondition::new)
	);

	/**
	 * Test.
	 *
	 * @param spawnContext spawn context
	 *
	 * @return boolean — результат операции
	 */
	public boolean test(SpawnContext spawnContext) {
		MoonPhase
				moonPhase =
				spawnContext
						.environmentAttributes()
						.getAttributeValue(EnvironmentAttributes.MOON_PHASE_VISUAL, Vec3d.ofCenter(spawnContext.pos()));
		float f = DimensionType.MOON_SIZES[moonPhase.getIndex()];
		return this.range.test(f);
	}

	@Override
	public MapCodec<MoonBrightnessSpawnCondition> getCodec() {
		return CODEC;
	}
}
