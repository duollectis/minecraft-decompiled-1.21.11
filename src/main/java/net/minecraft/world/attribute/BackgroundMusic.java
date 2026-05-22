package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.MusicType;
import net.minecraft.sound.SoundEvent;

import java.util.Optional;

/**
 * Описывает фоновую музыку окружения с тремя независимыми слотами:
 * обычный режим, творческий режим и подводный режим.
 * Используется как атрибут биома/измерения для управления музыкой.
 */
public record BackgroundMusic(
	Optional<MusicSound> defaultMusic,
	Optional<MusicSound> creativeMusic,
	Optional<MusicSound> underwaterMusic
) {

	/** Полностью пустой набор — музыка не задана ни для одного режима. */
	public static final BackgroundMusic EMPTY = new BackgroundMusic(Optional.empty(), Optional.empty(), Optional.empty());

	/** Стандартный набор: обычная игровая музыка и музыка творческого режима. */
	public static final BackgroundMusic DEFAULT = new BackgroundMusic(
		Optional.of(MusicType.GAME),
		Optional.of(MusicType.CREATIVE),
		Optional.empty()
	);

	public static final Codec<BackgroundMusic> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			MusicSound.CODEC.optionalFieldOf("default").forGetter(BackgroundMusic::defaultMusic),
			MusicSound.CODEC.optionalFieldOf("creative").forGetter(BackgroundMusic::creativeMusic),
			MusicSound.CODEC.optionalFieldOf("underwater").forGetter(BackgroundMusic::underwaterMusic)
		).apply(instance, BackgroundMusic::new)
	);

	public BackgroundMusic(MusicSound defaultMusic) {
		this(Optional.of(defaultMusic), Optional.empty(), Optional.empty());
	}

	public BackgroundMusic(RegistryEntry<SoundEvent> defaultMusic) {
		this(MusicType.createIngameMusic(defaultMusic));
	}

	/**
	 * Создаёт копию с заданной подводной музыкой.
	 *
	 * @param underwater музыка для подводного режима
	 * @return новый экземпляр с установленной подводной музыкой
	 */
	public BackgroundMusic withUnderwater(MusicSound underwater) {
		return new BackgroundMusic(defaultMusic, creativeMusic, Optional.of(underwater));
	}

	/**
	 * Возвращает актуальный трек музыки в зависимости от текущего режима игры.
	 * Приоритет: подводный > творческий > обычный.
	 *
	 * @param creative {@code true} если игрок в творческом режиме
	 * @param underwater {@code true} если игрок под водой
	 * @return подходящий трек или {@link Optional#empty()} если музыка не задана
	 */
	public Optional<MusicSound> getCurrent(boolean creative, boolean underwater) {
		if (underwater && underwaterMusic.isPresent()) {
			return underwaterMusic;
		}

		return creative && creativeMusic.isPresent() ? creativeMusic : defaultMusic;
	}
}
