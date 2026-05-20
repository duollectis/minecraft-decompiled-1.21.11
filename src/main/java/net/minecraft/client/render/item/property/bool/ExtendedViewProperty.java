package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record ExtendedViewProperty() implements BooleanProperty {
   public static final MapCodec<ExtendedViewProperty> CODEC = MapCodec.unit(new ExtendedViewProperty());

   @Override
   public boolean test(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed, ItemDisplayContext displayContext) {
      return displayContext == ItemDisplayContext.GUI && MinecraftClient.getInstance().isShiftPressed();
   }

   @Override
   public MapCodec<ExtendedViewProperty> getCodec() {
      return CODEC;
   }
}
