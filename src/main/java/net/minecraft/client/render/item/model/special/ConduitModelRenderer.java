package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.ConduitBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public class ConduitModelRenderer implements SimpleSpecialModelRenderer {
   private final SpriteHolder spriteHolder;
   private final ModelPart shell;

   public ConduitModelRenderer(SpriteHolder spriteHolder, ModelPart shell) {
      this.spriteHolder = spriteHolder;
      this.shell = shell;
   }

   @Override
   public void render(ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean glint, int i) {
      matrices.push();
      matrices.translate(0.5F, 0.5F, 0.5F);
      queue.submitModelPart(
         this.shell,
         matrices,
         ConduitBlockEntityRenderer.BASE_TEXTURE.getRenderLayer(RenderLayers::entitySolid),
         light,
         overlay,
         this.spriteHolder.getSprite(ConduitBlockEntityRenderer.BASE_TEXTURE),
         false,
         false,
         -1,
         null,
         i
      );
      matrices.pop();
   }

   @Override
   public void collectVertices(Consumer<Vector3fc> consumer) {
      MatrixStack matrixStack = new MatrixStack();
      matrixStack.translate(0.5F, 0.5F, 0.5F);
      this.shell.collectVertices(matrixStack, consumer);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked() implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<ConduitModelRenderer.Unbaked> CODEC = MapCodec.unit(new ConduitModelRenderer.Unbaked());

      @Override
      public MapCodec<ConduitModelRenderer.Unbaked> getCodec() {
         return CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
         return new ConduitModelRenderer(context.spriteHolder(), context.entityModelSet().getModelPart(EntityModelLayers.CONDUIT_SHELL));
      }
   }
}
