package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;

@Environment(EnvType.CLIENT)
public class LancerEntityRenderState extends BipedEntityRenderState {
   @Override
   public ItemStack getItemStackForArm(Arm arm) {
      return this.getMainHandItemStack();
   }
}
