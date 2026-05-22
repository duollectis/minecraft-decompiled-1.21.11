package net.minecraft.entity.passive;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Уровень окисления медного голема: от нового до полностью окисленного.
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
