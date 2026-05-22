package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

import java.util.Arrays;

/**
 * Экран настроек звука — управляет громкостью по категориям,
 * субтитрами, направленным звуком и частотой музыки.
 */
@Environment(EnvType.CLIENT)
public class SoundOptionsScreen extends GameOptionsScreen {

	private static final Text TITLE_TEXT = Text.translatable("options.sounds.title");

	public SoundOptionsScreen(Screen parent, GameOptions options) {
		super(parent, options, TITLE_TEXT);
	}

	@Override
	protected void addOptions() {
		body.addSingleOptionEntry(gameOptions.getSoundVolumeOption(SoundCategory.MASTER));
		body.addAll(getVolumeOptions());
		body.addSingleOptionEntry(gameOptions.getSoundDevice());
		body.addAll(gameOptions.getShowSubtitles(), gameOptions.getDirectionalAudio());
		body.addAll(gameOptions.getMusicFrequency(), gameOptions.getMusicToast());
	}

	private SimpleOption<?>[] getVolumeOptions() {
		return Arrays.stream(SoundCategory.values())
				.filter(category -> category != SoundCategory.MASTER)
				.map(gameOptions::getSoundVolumeOption)
				.toArray(SimpleOption[]::new);
	}
}
