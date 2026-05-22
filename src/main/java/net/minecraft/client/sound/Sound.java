package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.floatprovider.FloatSupplier;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

/**
 * Описание конкретного звукового файла или ссылки на событие звука.
 * Хранит параметры воспроизведения: громкость, высоту, вес для случайного выбора,
 * режим потоковой передачи и расстояние затухания.
 */
@Environment(EnvType.CLIENT)
public class Sound implements SoundContainer<Sound> {

	public static final ResourceFinder FINDER = new ResourceFinder("sounds", ".ogg");

	private final Identifier id;
	private final FloatSupplier volume;
	private final FloatSupplier pitch;
	private final int weight;
	private final Sound.RegistrationType registrationType;
	private final boolean stream;
	private final boolean preload;
	private final int attenuation;

	public Sound(
		Identifier id,
		FloatSupplier volume,
		FloatSupplier pitch,
		int weight,
		Sound.RegistrationType registrationType,
		boolean stream,
		boolean preload,
		int attenuation
	) {
		this.id = id;
		this.volume = volume;
		this.pitch = pitch;
		this.weight = weight;
		this.registrationType = registrationType;
		this.stream = stream;
		this.preload = preload;
		this.attenuation = attenuation;
	}

	public Identifier getIdentifier() {
		return id;
	}

	public Identifier getLocation() {
		return FINDER.toResourcePath(id);
	}

	public FloatSupplier getVolume() {
		return volume;
	}

	public FloatSupplier getPitch() {
		return pitch;
	}

	@Override
	public int getWeight() {
		return weight;
	}

	@Override
	public Sound getSound(Random random) {
		return this;
	}

	@Override
	public void preload(SoundSystem soundSystem) {
		if (preload) {
			soundSystem.addPreloadedSound(this);
		}
	}

	public Sound.RegistrationType getRegistrationType() {
		return registrationType;
	}

	public boolean isStreamed() {
		return stream;
	}

	public boolean isPreloaded() {
		return preload;
	}

	public int getAttenuation() {
		return attenuation;
	}

	@Override
	public String toString() {
		return "Sound[" + id + "]";
	}

	/**
	 * Способ регистрации звука: прямой файл или ссылка на другое звуковое событие.
	 */
	@Environment(EnvType.CLIENT)
	public enum RegistrationType {
		FILE("file"),
		SOUND_EVENT("event");

		private final String name;

		RegistrationType(String name) {
			this.name = name;
		}

		public static Sound.@Nullable RegistrationType getByName(String name) {
			for (Sound.RegistrationType type : values()) {
				if (type.name.equals(name)) {
					return type;
				}
			}

			return null;
		}
	}
}
