package net.minecraft.world.attribute.timeline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;

/**
 * Один ключевой кадр анимационного трека — пара (момент времени в тиках, значение атрибута).
 * Треки состоят из упорядоченного списка кадров; интерполятор вычисляет промежуточные значения
 * между соседними кадрами с учётом выбранного {@link EasingType}.
 *
 * @param <T> тип значения атрибута
 */
public record Keyframe<T>(int ticks, T value) {

	/**
	 * Создаёт codec для сериализации кадра с произвольным типом значения.
	 *
	 * @param <T>        тип значения
	 * @param valueCodec codec для сериализации значения
	 * @return codec, кодирующий кадр как объект с полями {@code ticks} и {@code value}
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
