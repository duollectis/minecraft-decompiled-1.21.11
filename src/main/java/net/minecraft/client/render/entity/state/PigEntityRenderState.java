package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.passive.PigVariant;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PigEntityRenderState extends LivingEntityRenderState {
   public ItemStack saddleStack = ItemStack.EMPTY;
   public @Nullable PigVariant variant;
}
