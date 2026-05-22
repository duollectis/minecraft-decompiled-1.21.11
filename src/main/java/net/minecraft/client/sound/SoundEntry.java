package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Запись звукового события из {@code sounds.json}.
 * Содержит список вариантов звука, флаг замены предыдущих определений
 * и опциональный ключ субтитра для отображения в интерфейсе.
 */
@Environment(EnvType.CLIENT)
public class SoundEntry {

	private final List<Sound> sounds;
	private final boolean replace;
	private final @Nullable String subtitle;

	public SoundEntry(List<Sound> sounds, boolean replace, @Nullable String subtitle) {
		this.sounds = sounds;
		this.replace = replace;
		this.subtitle = subtitle;
	}

	public List<Sound> getSounds() {
		return sounds;
	}

	public boolean canReplace() {
		return replace;
	}

	public @Nullable String getSubtitle() {
		return subtitle;
	}
}
