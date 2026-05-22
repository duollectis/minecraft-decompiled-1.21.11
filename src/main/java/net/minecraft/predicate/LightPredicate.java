package net.minecraft.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Предикат для проверки уровня освещённости в позиции мира.
 */
public record LightPredicate(NumberRange.IntRange range) {

	public static final Codec<LightPredicate> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(
							NumberRange.IntRange.CODEC
									.optionalFieldOf("light", NumberRange.IntRange.ANY)
									.forGetter(LightPredicate::range)
					)
					.apply(instance, LightPredicate::new)
	);

	public boolean test(ServerWorld world, BlockPos pos) {
		if (!world.isPosLoaded(pos)) {
			return false;
		}

		return range.test(world.getLightLevel(pos));
	}

	public static class Builder {

		private NumberRange.IntRange light = NumberRange.IntRange.ANY;

		public static LightPredicate.Builder create() {
			return new LightPredicate.Builder();
		}

		public LightPredicate.Builder light(NumberRange.IntRange light) {
			this.light = light;
			return this;
		}

		public LightPredicate build() {
			return new LightPredicate(light);
		}
	}
}
