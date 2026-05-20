package net.minecraft.client.item;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ItemModelManager {
   private final Function<Identifier, ItemModel> modelGetter;
   private final Function<Identifier, ItemAsset.Properties> propertiesGetter;

   public ItemModelManager(BakedModelManager bakedModelManager) {
      this.modelGetter = bakedModelManager::getItemModel;
      this.propertiesGetter = bakedModelManager::getItemProperties;
   }

   public void updateForLivingEntity(ItemRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, LivingEntity entity) {
      this.clearAndUpdate(renderState, stack, displayContext, entity.getEntityWorld(), entity, entity.getId() + displayContext.ordinal());
   }

   public void updateForNonLivingEntity(ItemRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, Entity entity) {
      this.clearAndUpdate(renderState, stack, displayContext, entity.getEntityWorld(), null, entity.getId());
   }

   public void clearAndUpdate(
      ItemRenderState renderState,
      ItemStack stack,
      ItemDisplayContext displayContext,
      @Nullable World world,
      @Nullable HeldItemContext heldItemContext,
      int seed
   ) {
      renderState.clear();
      if (!stack.isEmpty()) {
         renderState.displayContext = displayContext;
         this.update(renderState, stack, displayContext, world, heldItemContext, seed);
      }
   }

   public void update(
      ItemRenderState renderState,
      ItemStack stack,
      ItemDisplayContext displayContext,
      @Nullable World world,
      @Nullable HeldItemContext heldItemContext,
      int seed
   ) {
      Identifier identifier = stack.get(DataComponentTypes.ITEM_MODEL);
      if (identifier != null) {
         renderState.setOversizedInGui(this.propertiesGetter.apply(identifier).oversizedInGui());
         this.modelGetter
            .apply(identifier)
            .update(renderState, stack, this, displayContext, world instanceof ClientWorld clientWorld ? clientWorld : null, heldItemContext, seed);
      }
   }

   public boolean hasHandAnimationOnSwap(ItemStack stack) {
      Identifier identifier = stack.get(DataComponentTypes.ITEM_MODEL);
      return identifier == null ? true : this.propertiesGetter.apply(identifier).handAnimationOnSwap();
   }

   public float getSwapAnimationScale(ItemStack stack) {
      Identifier identifier = stack.get(DataComponentTypes.ITEM_MODEL);
      return identifier == null ? 1.0F : this.propertiesGetter.apply(identifier).swapAnimationScale();
   }
}
