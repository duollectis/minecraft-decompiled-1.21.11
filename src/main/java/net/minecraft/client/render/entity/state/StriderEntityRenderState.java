package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public class StriderEntityRenderState extends LivingEntityRenderState {
   public ItemStack saddleStack = ItemStack.EMPTY;
   public boolean cold;
   public boolean hasPassengers;
}
