package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
/**
 * {@code HappyGhastEntityRenderState}.
 */
public class HappyGhastEntityRenderState extends LivingEntityRenderState {

	public ItemStack harnessStack = ItemStack.EMPTY;
	public boolean hasPassengers;
	public boolean hasRopes;
}
