package net.minecraft.client.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.item.Item;

@Environment(EnvType.CLIENT)
public interface ItemModelOutput {
   default void accept(Item item, ItemModel.Unbaked model) {
      this.accept(item, model, ItemAsset.Properties.DEFAULT);
   }

   void accept(Item item, ItemModel.Unbaked model, ItemAsset.Properties properties);

   void acceptAlias(Item base, Item alias);
}
