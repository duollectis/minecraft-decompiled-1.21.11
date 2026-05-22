package net.minecraft.world.attribute;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Interpolator;
import net.minecraft.util.math.MathHelper;

/**
 * Модификатор цветовых атрибутов окружения (RGB/ARGB).
 * Определяет набор стандартных операций смешивания цветов.
 *
 * @param <Argument> тип аргумента операции
 */
public interface ColorModifier<Argument> extends EnvironmentAttributeModifier<Integer, Argument> {

	/** Alpha-blend: смешивает текущий цвет с аргументом через альфа-канал аргумента. */
	ColorModifier<Integer> ALPHA_BLEND = new ColorModifier<>() {
		@Override
		public Integer apply(Integer current, Integer argument) {
			return ColorHelper.alphaBlend(current, argument);
		}

		@Override
		public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> attribute) {
			return Codecs.HEX_ARGB;
		}

		@Override
		public Interpolator<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> attribute) {
			return Interpolator.ofColor();
		}
	};

	/** Поканальное сложение ARGB-цветов. */
	ColorModifier<Integer> ADD = (Argb) ColorHelper::add;

	/** Поканальное вычитание ARGB-цветов. */
	ColorModifier<Integer> SUBTRACT = (Argb) ColorHelper::subtract;

	/** Поканальное умножение RGB-цветов (без альфа-канала). */
	ColorModifier<Integer> MULTIPLY_RGB = (Rgb) ColorHelper::mix;

	/** Поканальное умножение ARGB-цветов (включая альфа-канал). */
	ColorModifier<Integer> MULTIPLY_ARGB = (Argb) ColorHelper::mix;

	/**
	 * Смешивание с серым: переводит цвет в оттенок серого с заданной яркостью
	 * и смешивает с исходным цветом по коэффициенту {@code factor}.
	 */
	ColorModifier<BlendToGrayArg> BLEND_TO_GRAY = new ColorModifier<>() {
		@Override
		public Integer apply(Integer current, BlendToGrayArg argument) {
			int gray = ColorHelper.scaleRgb(ColorHelper.grayscale(current), argument.brightness);
			return ColorHelper.lerp(argument.factor, current, gray);
		}

		@Override
		public Codec<BlendToGrayArg> argumentCodec(EnvironmentAttribute<Integer> attribute) {
			return BlendToGrayArg.CODEC;
		}

		@Override
		public Interpolator<BlendToGrayArg> argumentKeyframeLerp(EnvironmentAttribute<Integer> attribute) {
			return (t, a, b) -> new BlendToGrayArg(
				MathHelper.lerp(t, a.brightness, b.brightness),
				MathHelper.lerp(t, a.factor, b.factor)
			);
		}
	};

	/**
	 * Аргумент для операции {@link #BLEND_TO_GRAY}: яркость серого и коэффициент смешивания.
	 */
	record BlendToGrayArg(float brightness, float factor) {

		public static final Codec<BlendToGrayArg> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("brightness").forGetter(BlendToGrayArg::brightness),
				Codec.floatRange(0.0F, 1.0F).fieldOf("factor").forGetter(BlendToGrayArg::factor)
			).apply(instance, BlendToGrayArg::new)
		);
	}

	/**
	 * Специализация {@link ColorModifier} для ARGB-цветов (с альфа-каналом).
	 * Codec принимает как HEX_ARGB, так и HEX_RGB (если альфа == 255).
	 */
	@FunctionalInterface
	interface Argb extends ColorModifier<Integer> {

		@Override
		default Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> attribute) {
			return Codec.either(Codecs.HEX_ARGB, Codecs.RGB)
				.xmap(
					Either::unwrap,
					argb -> ColorHelper.getAlpha(argb) == 255 ? Either.right(argb) : Either.left(argb)
				);
		}

		@Override
		default Interpolator<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> attribute) {
			return Interpolator.ofColor();
		}
	}

	/**
	 * Специализация {@link ColorModifier} для RGB-цветов (без альфа-канала).
	 * Codec принимает только HEX_RGB.
	 */
	@FunctionalInterface
	interface Rgb extends ColorModifier<Integer> {

		@Override
		default Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> attribute) {
			return Codecs.HEX_RGB;
		}

		@Override
		default Interpolator<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> attribute) {
			return Interpolator.ofColor();
		}
	}
}
