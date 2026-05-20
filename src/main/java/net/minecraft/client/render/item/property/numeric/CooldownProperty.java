package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record CooldownProperty() implements NumericProperty {
   public static final MapCodec<CooldownProperty> CODEC = MapCodec.unit(new CooldownProperty());

   @Override
   public float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable HeldItemContext context, int seed) {
      return context != null && context.getEntity() instanceof PlayerEntity playerEntity
         ? playerEntity.getItemCooldownManager().getCooldownProgress(stack, 0.0F)
         : 0.0F;
   }

   @Override
   public MapCodec<CooldownProperty> getCodec() {
      return CODEC;
   }
}
