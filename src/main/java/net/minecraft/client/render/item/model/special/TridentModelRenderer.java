package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.TridentEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public class TridentModelRenderer implements SimpleSpecialModelRenderer {
   private final TridentEntityModel model;

   public TridentModelRenderer(TridentEntityModel model) {
      this.model = model;
   }

   @Override
   public void render(ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean glint, int i) {
      matrices.push();
      matrices.scale(1.0F, -1.0F, -1.0F);
      queue.submitModelPart(
         this.model.getRootPart(), matrices, this.model.getLayer(TridentEntityModel.TEXTURE), light, overlay, null, false, glint, -1, null, i
      );
      matrices.pop();
   }

   @Override
   public void collectVertices(Consumer<Vector3fc> consumer) {
      MatrixStack matrixStack = new MatrixStack();
      matrixStack.scale(1.0F, -1.0F, -1.0F);
      this.model.getRootPart().collectVertices(matrixStack, consumer);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked() implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<TridentModelRenderer.Unbaked> CODEC = MapCodec.unit(new TridentModelRenderer.Unbaked());

      @Override
      public MapCodec<TridentModelRenderer.Unbaked> getCodec() {
         return CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
         return new TridentModelRenderer(new TridentEntityModel(context.entityModelSet().getModelPart(EntityModelLayers.TRIDENT)));
      }
   }
}
