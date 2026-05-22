package net.minecraft.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Описывает «атмосферный» звук биома, воспроизводимый с задержкой
 * в случайно выбранной точке вблизи игрока.
 *
 * <p>Система настроения биома периодически ищет подходящий блок
 * в кубе радиуса {@link #blockSearchExtent} и воспроизводит звук
 * со смещением {@link #offset} от найденной позиции.
 *
 * @param sound             ссылка на звуковое событие
 * @param tickDelay         минимальная задержка в тиках между воспроизведениями
 * @param blockSearchExtent радиус поиска блока для позиционирования звука
 * @param offset            смещение источника звука от найденного блока
 */
public record BiomeMoodSound(
	RegistryEntry<SoundEvent> sound,
	int tickDelay,
	int blockSearchExtent,
	double offset
) {

	/** Стандартный звук пещеры, используемый в большинстве подземных биомов. */
	public static final BiomeMoodSound CAVE = new BiomeMoodSound(SoundEvents.AMBIENT_CAVE, 6000, 8, 2.0);

	public static final Codec<BiomeMoodSound> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			SoundEvent.ENTRY_CODEC.fieldOf("sound").forGetter(BiomeMoodSound::sound),
			Codec.INT.fieldOf("tick_delay").forGetter(BiomeMoodSound::tickDelay),
			Codec.INT.fieldOf("block_search_extent").forGetter(BiomeMoodSound::blockSearchExtent),
			Codec.DOUBLE.fieldOf("offset").forGetter(BiomeMoodSound::offset)
		).apply(instance, BiomeMoodSound::new)
	);
}
