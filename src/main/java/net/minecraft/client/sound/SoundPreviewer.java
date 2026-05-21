package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code SoundPreviewer}.
 */
public final class SoundPreviewer {

	private static @Nullable SoundInstance currentSoundPreview;
	private static @Nullable SoundCategory category;

	/**
	 * Preview.
	 *
	 * @param manager manager
	 * @param category category
	 * @param volume volume
	 */
	public static void preview(SoundManager manager, SoundCategory category, float volume) {
		stopPreviewOfOtherCategory(manager, category);
		if (canPlaySound(manager)) {
			SoundEvent soundEvent = switch (category) {
				case RECORDS -> (SoundEvent) SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value();
				case WEATHER -> SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER;
				case BLOCKS -> SoundEvents.BLOCK_GRASS_PLACE;
				case HOSTILE -> SoundEvents.ENTITY_ZOMBIE_AMBIENT;
				case NEUTRAL -> SoundEvents.ENTITY_COW_AMBIENT;
				case PLAYERS -> (SoundEvent) SoundEvents.ENTITY_GENERIC_EAT.value();
				case AMBIENT -> (SoundEvent) SoundEvents.AMBIENT_CAVE.value();
				case UI -> (SoundEvent) SoundEvents.UI_BUTTON_CLICK.value();
				default -> SoundEvents.INTENTIONALLY_EMPTY;
			};
			if (soundEvent != SoundEvents.INTENTIONALLY_EMPTY) {
				currentSoundPreview = PositionedSoundInstance.master(soundEvent, 1.0F, volume);
				manager.play(currentSoundPreview);
			}
		}
	}

	private static void stopPreviewOfOtherCategory(SoundManager manager, SoundCategory category) {
		if (SoundPreviewer.category != category) {
			SoundPreviewer.category = category;
			if (currentSoundPreview != null) {
				manager.stop(currentSoundPreview);
			}
		}
	}

	private static boolean canPlaySound(SoundManager manager) {
		return currentSoundPreview == null || !manager.isPlaying(currentSoundPreview);
	}
}
