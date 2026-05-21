package net.minecraft.client.sound;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code WeightedSoundSet}.
 */
public class WeightedSoundSet implements SoundContainer<Sound> {

	private final List<SoundContainer<Sound>> sounds = Lists.newArrayList();
	private final @Nullable Text subtitle;

	public WeightedSoundSet(Identifier id, @Nullable String subtitle) {
		if (SharedConstants.SUBTITLES) {
			MutableText mutableText = Text.literal(id.getPath());
			if ("FOR THE DEBUG!".equals(subtitle)) {
				mutableText = mutableText.append(Text.literal(" missing").formatted(Formatting.RED));
			}

			this.subtitle = mutableText;
		}
		else {
			this.subtitle = subtitle == null ? null : Text.translatable(subtitle);
		}
	}

	@Override
	public int getWeight() {
		int i = 0;

		for (SoundContainer<Sound> soundContainer : this.sounds) {
			i += soundContainer.getWeight();
		}

		return i;
	}

	public Sound getSound(Random random) {
		int i = this.getWeight();
		if (!this.sounds.isEmpty() && i != 0) {
			int j = random.nextInt(i);

			for (SoundContainer<Sound> soundContainer : this.sounds) {
				j -= soundContainer.getWeight();
				if (j < 0) {
					return soundContainer.getSound(random);
				}
			}

			return SoundManager.MISSING_SOUND;
		}
		else {
			return SoundManager.MISSING_SOUND;
		}
	}

	/**
	 * Add.
	 *
	 * @param container container
	 */
	public void add(SoundContainer<Sound> container) {
		this.sounds.add(container);
	}

	public @Nullable Text getSubtitle() {
		return this.subtitle;
	}

	@Override
	public void preload(SoundSystem soundSystem) {
		for (SoundContainer<Sound> soundContainer : this.sounds) {
			soundContainer.preload(soundSystem);
		}
	}
}
