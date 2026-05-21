package net.minecraft.entity.passive;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * {@code CopperGolemOxidationLevel}.
 */
public record CopperGolemOxidationLevel(
		SoundEvent spinHeadSound,
		SoundEvent hurtSound,
		SoundEvent deathSound,
		SoundEvent stepSound,
		Identifier texture,
		Identifier eyeTexture
) {
}
