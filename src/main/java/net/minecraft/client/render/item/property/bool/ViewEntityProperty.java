package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record ViewEntityProperty() implements BooleanProperty {
   public static final MapCodec<ViewEntityProperty> CODEC = MapCodec.unit(new ViewEntityProperty());

   @Override
   public boolean test(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed, ItemDisplayContext displayContext) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Entity entity2 = minecraftClient.getCameraEntity();
      return entity2 != null ? entity == entity2 : entity == minecraftClient.player;
   }

   @Override
   public MapCodec<ViewEntityProperty> getCodec() {
      return CODEC;
   }
}
