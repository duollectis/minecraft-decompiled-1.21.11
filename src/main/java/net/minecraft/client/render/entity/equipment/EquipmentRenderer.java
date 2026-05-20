package net.minecraft.client.render.entity.equipment;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class EquipmentRenderer {
   private static final int field_54178 = 0;
   private final EquipmentModelLoader equipmentModelLoader;
   private final Function<EquipmentRenderer.LayerTextureKey, Identifier> layerTextures;
   private final Function<EquipmentRenderer.TrimSpriteKey, Sprite> trimSprites;

   public EquipmentRenderer(EquipmentModelLoader equipmentModelLoader, SpriteAtlasTexture armorTrimsAtlas) {
      this.equipmentModelLoader = equipmentModelLoader;
      this.layerTextures = Util.memoize(key -> key.layer.getFullTextureId(key.layerType));
      this.trimSprites = Util.memoize(key -> armorTrimsAtlas.getSprite(key.getTexture()));
   }

   public <S> void render(
      EquipmentModel.LayerType layerType,
      RegistryKey<EquipmentAsset> assetKey,
      Model<? super S> model,
      S object,
      ItemStack itemStack,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      int j
   ) {
      this.render(layerType, assetKey, model, object, itemStack, matrixStack, orderedRenderCommandQueue, i, null, j, 1);
   }

   public <S> void render(
      EquipmentModel.LayerType layerType,
      RegistryKey<EquipmentAsset> assetKey,
      Model<? super S> model,
      S object,
      ItemStack itemStack,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      @Nullable Identifier identifier,
      int j,
      int k
   ) {
      List<EquipmentModel.Layer> list = this.equipmentModelLoader.get(assetKey).getLayers(layerType);
      if (!list.isEmpty()) {
         int l = DyedColorComponent.getColor(itemStack, 0);
         boolean bl = itemStack.hasGlint();
         int m = k;

         for (EquipmentModel.Layer layer : list) {
            int n = getDyeColor(layer, l);
            if (n != 0) {
               Identifier identifier2 = layer.usePlayerTexture() && identifier != null
                  ? identifier
                  : this.layerTextures.apply(new EquipmentRenderer.LayerTextureKey(layerType, layer));
               orderedRenderCommandQueue.getBatchingQueue(m++)
                  .submitModel(model, object, matrixStack, RenderLayers.armorCutoutNoCull(identifier2), i, OverlayTexture.DEFAULT_UV, n, null, j, null);
               if (bl) {
                  orderedRenderCommandQueue.getBatchingQueue(m++)
                     .submitModel(model, object, matrixStack, RenderLayers.armorEntityGlint(), i, OverlayTexture.DEFAULT_UV, n, null, j, null);
               }

               bl = false;
            }
         }

         ArmorTrim armorTrim = itemStack.get(DataComponentTypes.TRIM);
         if (armorTrim != null) {
            Sprite sprite = this.trimSprites.apply(new EquipmentRenderer.TrimSpriteKey(armorTrim, layerType, assetKey));
            RenderLayer renderLayer = TexturedRenderLayers.getArmorTrims(armorTrim.pattern().value().decal());
            orderedRenderCommandQueue.getBatchingQueue(m++)
               .submitModel(model, object, matrixStack, renderLayer, i, OverlayTexture.DEFAULT_UV, -1, sprite, j, null);
         }
      }
   }

   private static int getDyeColor(EquipmentModel.Layer layer, int dyeColor) {
      Optional<EquipmentModel.Dyeable> optional = layer.dyeable();
      if (optional.isPresent()) {
         int i = optional.get().colorWhenUndyed().map(ColorHelper::fullAlpha).orElse(0);
         return dyeColor != 0 ? dyeColor : i;
      } else {
         return -1;
      }
   }

   @Environment(EnvType.CLIENT)
   record LayerTextureKey(EquipmentModel.LayerType layerType, EquipmentModel.Layer layer) {
   }

   @Environment(EnvType.CLIENT)
   record TrimSpriteKey(ArmorTrim trim, EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> equipmentAssetId) {
      public Identifier getTexture() {
         return this.trim.getTextureId(this.layerType.getTrimsDirectory(), this.equipmentAssetId);
      }
   }
}
