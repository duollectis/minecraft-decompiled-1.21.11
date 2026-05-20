package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record TrimMaterialProperty() implements SelectProperty<RegistryKey<ArmorTrimMaterial>> {
   public static final Codec<RegistryKey<ArmorTrimMaterial>> VALUE_CODEC = RegistryKey.createCodec(RegistryKeys.TRIM_MATERIAL);
   public static final SelectProperty.Type<TrimMaterialProperty, RegistryKey<ArmorTrimMaterial>> TYPE = SelectProperty.Type.create(
      MapCodec.unit(new TrimMaterialProperty()), VALUE_CODEC
   );

   public @Nullable RegistryKey<ArmorTrimMaterial> getValue(
      ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ItemDisplayContext itemDisplayContext
   ) {
      ArmorTrim armorTrim = itemStack.get(DataComponentTypes.TRIM);
      return armorTrim == null ? null : armorTrim.material().getKey().orElse(null);
   }

   @Override
   public SelectProperty.Type<TrimMaterialProperty, RegistryKey<ArmorTrimMaterial>> getType() {
      return TYPE;
   }

   @Override
   public Codec<RegistryKey<ArmorTrimMaterial>> valueCodec() {
      return VALUE_CODEC;
   }
}
