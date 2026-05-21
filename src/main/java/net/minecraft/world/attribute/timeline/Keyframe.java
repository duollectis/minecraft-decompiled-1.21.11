package net.minecraft.world.attribute.timeline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;

/**
 * {@code Keyframe}.
 */
public record Keyframe<T>(int ticks, T value) {

	/**
	 * Создаёт codec.
	 *
	 * @param valueCodec value codec
	 *
	 * @return Codec> — результат операции
	 */
	public static <T> Codec<Keyframe<T>> createCodec(Codec<T> valueCodec) {
		return RecordCodecBuilder.create(
				instance -> instance
						.group(
								Codecs.NON_NEGATIVE_INT.fieldOf("ticks").forGetter(Keyframe::ticks),
								valueCodec.fieldOf("value").forGetter(Keyframe::value)
						)
						.apply(instance, Keyframe::new)
		);
	}
}
