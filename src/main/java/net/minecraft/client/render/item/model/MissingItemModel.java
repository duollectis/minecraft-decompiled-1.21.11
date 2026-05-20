package net.minecraft.client.render.item.model;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelSettings;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class MissingItemModel implements ItemModel {
   private final List<BakedQuad> quads;
   private final Supplier<Vector3fc[]> vector;
   private final ModelSettings settings;

   public MissingItemModel(List<BakedQuad> quads, ModelSettings settings) {
      this.quads = quads;
      this.settings = settings;
      this.vector = Suppliers.memoize(() -> BasicItemModel.bakeQuads(this.quads));
   }

   @Override
   public void update(
      ItemRenderState state,
      ItemStack stack,
      ItemModelManager resolver,
      ItemDisplayContext displayContext,
      @Nullable ClientWorld world,
      @Nullable HeldItemContext heldItemContext,
      int seed
   ) {
      state.addModelKey(this);
      ItemRenderState.LayerRenderState layerRenderState = state.newLayer();
      layerRenderState.setRenderLayer(TexturedRenderLayers.getEntityCutout());
      this.settings.addSettings(layerRenderState, displayContext);
      layerRenderState.setVertices(this.vector);
      layerRenderState.getQuads().addAll(this.quads);
   }
}
