package net.minecraft.entity;

import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;

/**
 * {@code Shearable}.
 */
public interface Shearable {

	void sheared(ServerWorld world, SoundCategory shearedSoundCategory, ItemStack shears);

	boolean isShearable();
}
