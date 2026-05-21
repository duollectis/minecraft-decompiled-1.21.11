package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code SoundEntry}.
 */
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
		return this.sounds;
	}

	/**
	 * Проверяет возможность replace.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canReplace() {
		return this.replace;
	}

	public @Nullable String getSubtitle() {
		return this.subtitle;
	}
}
