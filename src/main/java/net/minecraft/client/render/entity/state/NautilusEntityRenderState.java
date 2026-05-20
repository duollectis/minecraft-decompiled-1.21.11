package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.mob.ZombieNautilusVariant;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class NautilusEntityRenderState extends LivingEntityRenderState {
   public ItemStack saddleStack = ItemStack.EMPTY;
   public ItemStack armorStack = ItemStack.EMPTY;
   public @Nullable ZombieNautilusVariant variant;
}
