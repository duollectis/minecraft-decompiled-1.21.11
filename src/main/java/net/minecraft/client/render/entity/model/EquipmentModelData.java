package net.minecraft.client.render.entity.model;

import com.google.common.collect.ImmutableMap.Builder;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.entity.EquipmentSlot;

@Environment(EnvType.CLIENT)
public record EquipmentModelData<T>(T head, T chest, T legs, T feet) {
   public T getModelData(EquipmentSlot slot) {
      return (T)(switch (slot) {
         case HEAD -> (Object)this.head;
         case CHEST -> (Object)this.chest;
         case LEGS -> (Object)this.legs;
         case FEET -> (Object)this.feet;
         default -> throw new IllegalStateException("No model for slot: " + slot);
      });
   }

   public <U> EquipmentModelData<U> map(Function<? super T, ? extends U> f) {
      return (EquipmentModelData<U>)(new EquipmentModelData<>(f.apply(this.head), f.apply(this.chest), f.apply(this.legs), f.apply(this.feet)));
   }

   public void addTo(EquipmentModelData<TexturedModelData> texturedModelData, Builder<T, TexturedModelData> builder) {
      builder.put(this.head, texturedModelData.head);
      builder.put(this.chest, texturedModelData.chest);
      builder.put(this.legs, texturedModelData.legs);
      builder.put(this.feet, texturedModelData.feet);
   }

   public static <M extends BipedEntityModel<?>> EquipmentModelData<M> mapToEntityModel(
      EquipmentModelData<EntityModelLayer> data, LoadedEntityModels models, Function<ModelPart, M> modelPartToModel
   ) {
      return data.map(layer -> modelPartToModel.apply(models.getModelPart(layer)));
   }
}
