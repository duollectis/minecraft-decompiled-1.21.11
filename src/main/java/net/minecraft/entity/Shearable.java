package net.minecraft.entity;

import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;

/**
 * Интерфейс для сущностей, которых можно стричь ножницами (овцы, снежные голем и т.д.).
 */
public interface Shearable {

	void sheared(ServerWorld world, SoundCategory shearedSoundCategory, ItemStack shears);

	boolean isShearable();
}
