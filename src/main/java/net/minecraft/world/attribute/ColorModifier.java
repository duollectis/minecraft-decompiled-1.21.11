package net.minecraft.world.attribute;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Interpolator;
import net.minecraft.util.math.MathHelper;

/**
 * {@code ColorModifier}.
 */
public interface ColorModifier<Argument> extends EnvironmentAttributeModifier<Integer, Argument> {

	ColorModifier<Integer> ALPHA_BLEND = new ColorModifier<Integer>() {
		public Integer apply(Integer integer, Integer integer2) {
			return ColorHelper.alphaBlend(integer, integer2);
		}

		@Override
		public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> environmentAttribute) {
			return Codecs.HEX_ARGB;
		}

		@Override
		public Interpolator<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> environmentAttribute) {
			return Interpolator.ofColor();
		}
	};

	ColorModifier<Integer> ADD = (ColorModifier.Argb) ColorHelper::add;

	ColorModifier<Integer> SUBTRACT = (ColorModifier.Argb) ColorHelper::subtract;

	ColorModifier<Integer> MULTIPLY_RGB = (ColorModifier.Rgb) ColorHelper::mix;

	ColorModifier<Integer> MULTIPLY_ARGB = (ColorModifier.Argb) ColorHelper::mix;

	ColorModifier<ColorModifier.BlendToGrayArg> BLEND_TO_GRAY = new ColorModifier<ColorModifier.BlendToGrayArg>() {
		public Integer apply(Integer integer, ColorModifier.BlendToGrayArg blendToGrayArg) {
			int i = ColorHelper.scaleRgb(ColorHelper.grayscale(integer), blendToGrayArg.brightness);
			return ColorHelper.lerp(blendToGrayArg.factor, integer, i);
		}

		@Override
		public Codec<ColorModifier.BlendToGrayArg> argumentCodec(EnvironmentAttribute<Integer> environmentAttribute) {
			return ColorModifier.BlendToGrayArg.CODEC;
		}

		@Override
		public Interpolator<ColorModifier.BlendToGrayArg> argumentKeyframeLerp(EnvironmentAttribute<Integer> environmentAttribute) {
			return (t, a, b) -> new ColorModifier.BlendToGrayArg(
					MathHelper.lerp(t, a.brightness, b.brightness),
					MathHelper.lerp(t, a.factor, b.factor)
			);
		}
	};

	@FunctionalInterface
	/**
	 * {@code Argb}.
	 */
	public interface Argb extends ColorModifier<Integer> {

		@Override
		default Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> environmentAttribute) {
			return Codec.either(Codecs.HEX_ARGB, Codecs.RGB)
			            .xmap(
					            Either::unwrap,
					            argb -> ColorHelper.getAlpha(argb) == 255 ? Either.right(argb) : Either.left(argb)
			            );
		}

		@Override
		default Interpolator<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> environmentAttribute) {
			return Interpolator.ofColor();
		}
	}

	/**
	 * {@code BlendToGrayArg}.
	 */
	public record BlendToGrayArg(float brightness, float factor) {

		public static final Codec<ColorModifier.BlendToGrayArg> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codec
								                    .floatRange(0.0F, 1.0F)
								                    .fieldOf("brightness")
								                    .forGetter(ColorModifier.BlendToGrayArg::brightness),
						                    Codec.floatRange(0.0F, 1.0F).fieldOf("factor").forGetter(ColorModifier.BlendToGrayArg::factor)
				                    )
				                    .apply(instance, ColorModifier.BlendToGrayArg::new)
		);
	}

	@FunctionalInterface
	/**
	 * {@code Rgb}.
	 */
	public interface Rgb extends ColorModifier<Integer> {

		@Override
		default Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> environmentAttribute) {
			return Codecs.HEX_RGB;
		}

		@Override
		default Interpolator<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> environmentAttribute) {
			return Interpolator.ofColor();
		}
	}
}
