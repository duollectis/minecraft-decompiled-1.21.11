package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
/**
 * {@code SoundOptionsScreen}.
 */
public class SoundOptionsScreen extends GameOptionsScreen {

	private static final Text TITLE_TEXT = Text.translatable("options.sounds.title");

	public SoundOptionsScreen(Screen parent, GameOptions options) {
		super(parent, options, TITLE_TEXT);
	}

	@Override
	protected void addOptions() {
		this.body.addSingleOptionEntry(this.gameOptions.getSoundVolumeOption(SoundCategory.MASTER));
		this.body.addAll(this.getVolumeOptions());
		this.body.addSingleOptionEntry(this.gameOptions.getSoundDevice());
		this.body.addAll(this.gameOptions.getShowSubtitles(), this.gameOptions.getDirectionalAudio());
		this.body.addAll(this.gameOptions.getMusicFrequency(), this.gameOptions.getMusicToast());
	}

	private SimpleOption<?>[] getVolumeOptions() {
		return Arrays.stream(SoundCategory.values())
		             .filter(category -> category != SoundCategory.MASTER)
		             .map(this.gameOptions::getSoundVolumeOption)
		             .toArray(SimpleOption[]::new);
	}
}
