package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record CustomModelDataStringProperty(int index) implements SelectProperty<String> {
   public static final PrimitiveCodec<String> VALUE_CODEC = Codec.STRING;
   public static final SelectProperty.Type<CustomModelDataStringProperty, String> TYPE = SelectProperty.Type.create(
      RecordCodecBuilder.mapCodec(
         instance -> instance.group(Codecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataStringProperty::index))
            .apply(instance, CustomModelDataStringProperty::new)
      ),
      VALUE_CODEC
   );

   public @Nullable String getValue(
      ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ItemDisplayContext itemDisplayContext
   ) {
      CustomModelDataComponent customModelDataComponent = itemStack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
      return customModelDataComponent != null ? customModelDataComponent.getString(this.index) : null;
   }

   @Override
   public SelectProperty.Type<CustomModelDataStringProperty, String> getType() {
      return TYPE;
   }

   @Override
   public Codec<String> valueCodec() {
      return VALUE_CODEC;
   }
}
