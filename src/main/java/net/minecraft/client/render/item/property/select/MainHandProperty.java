package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record MainHandProperty() implements SelectProperty<Arm> {
   public static final Codec<Arm> VALUE_CODEC = Arm.CODEC;
   public static final SelectProperty.Type<MainHandProperty, Arm> TYPE = SelectProperty.Type.create(MapCodec.unit(new MainHandProperty()), VALUE_CODEC);

   public @Nullable Arm getValue(
      ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ItemDisplayContext itemDisplayContext
   ) {
      return livingEntity == null ? null : livingEntity.getMainArm();
   }

   @Override
   public SelectProperty.Type<MainHandProperty, Arm> getType() {
      return TYPE;
   }

   @Override
   public Codec<Arm> valueCodec() {
      return VALUE_CODEC;
   }
}
