package net.minecraft.world.attribute;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Аргумент для операции alpha-blend: целевое значение и коэффициент смешивания.
 * При {@code alpha == 1.0} сериализуется как простое число (только {@code value}).
 */
public record BlendArgument(float value, float alpha) {

	private static final Codec<BlendArgument> INTERNAL_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Codec.FLOAT.fieldOf("value").forGetter(BlendArgument::value),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("alpha", 1.0F).forGetter(BlendArgument::alpha)
		).apply(instance, BlendArgument::new)
	);

	/**
	 * Codec с компактной формой: если {@code alpha == 1.0}, сериализуется как одно число.
	 * Иначе — как объект с полями {@code value} и {@code alpha}.
	 */
	public static final Codec<BlendArgument> CODEC = Codec.either(Codec.FLOAT, INTERNAL_CODEC)
		.xmap(
			either -> either.map(BlendArgument::new, blend -> blend),
			blend -> blend.alpha() == 1.0F
				? Either.left(blend.value())
				: Either.right(blend)
		);

	/** Создаёт аргумент с полным смешиванием (alpha = 1.0). */
	public BlendArgument(float value) {
		this(value, 1.0F);
	}
}
