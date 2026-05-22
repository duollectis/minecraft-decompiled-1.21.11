package net.minecraft.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Описывает дополнительный звук биома — редкий случайный звук,
 * воспроизводимый с заданной вероятностью каждый тик.
 *
 * <p>В отличие от {@link BiomeMoodSound}, этот звук не привязан к конкретной
 * позиции блока и воспроизводится непосредственно у игрока.
 *
 * @param sound       ссылка на звуковое событие
 * @param tickChance  вероятность воспроизведения за один тик (от 0.0 до 1.0)
 */
public record BiomeAdditionsSound(RegistryEntry<SoundEvent> sound, double tickChance) {

	public static final Codec<BiomeAdditionsSound> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			SoundEvent.ENTRY_CODEC.fieldOf("sound").forGetter(BiomeAdditionsSound::sound),
			Codec.DOUBLE.fieldOf("tick_chance").forGetter(BiomeAdditionsSound::tickChance)
		).apply(instance, BiomeAdditionsSound::new)
	);
}
