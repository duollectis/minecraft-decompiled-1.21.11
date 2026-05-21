package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.Interpolator;
import net.minecraft.util.math.MathHelper;

/**
 * {@code FloatModifier}.
 */
public interface FloatModifier<Argument> extends EnvironmentAttributeModifier<Float, Argument> {

	FloatModifier<BlendArgument> ALPHA_BLEND = new FloatModifier<BlendArgument>() {
		/**
		 * Apply.
		 *
		 * @param float_ float_
		 * @param blendArgument blend argument
		 *
		 * @return Float — результат операции
		 */
		public Float apply(Float float_, BlendArgument blendArgument) {
			return MathHelper.lerp(blendArgument.alpha(), float_, blendArgument.value());
		}

		@Override
		public Codec<BlendArgument> argumentCodec(EnvironmentAttribute<Float> environmentAttribute) {
			return BlendArgument.CODEC;
		}

		@Override
		public Interpolator<BlendArgument> argumentKeyframeLerp(EnvironmentAttribute<Float> environmentAttribute) {
			return (f, blendArgument, blendArgument2) -> new BlendArgument(
					MathHelper.lerp(f, blendArgument.value(), blendArgument2.value()),
					MathHelper.lerp(f, blendArgument.alpha(), blendArgument2.alpha())
			);
		}
	};

	FloatModifier<Float> ADD = (FloatModifier.Binary) Float::sum;

	FloatModifier<Float> SUBTRACT = (FloatModifier.Binary) (a, b) -> a - b;

	FloatModifier<Float> MULTIPLY = (FloatModifier.Binary) (a, b) -> a * b;

	FloatModifier<Float> MINIMUM = (FloatModifier.Binary) Math::min;

	FloatModifier<Float> MAXIMUM = (FloatModifier.Binary) Math::max;

	@FunctionalInterface
	/**
	 * {@code Binary}.
	 */
	public interface Binary extends FloatModifier<Float> {

		@Override
		default Codec<Float> argumentCodec(EnvironmentAttribute<Float> environmentAttribute) {
			return Codec.FLOAT;
		}

		@Override
		default Interpolator<Float> argumentKeyframeLerp(EnvironmentAttribute<Float> environmentAttribute) {
			return Interpolator.ofFloat();
		}
	}
}
