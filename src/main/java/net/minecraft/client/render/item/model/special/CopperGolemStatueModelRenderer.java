package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.CopperGolemStatueBlock;
import net.minecraft.block.Oxidizable;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.model.CopperGolemStatueModel;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.CopperGolemOxidationLevels;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public class CopperGolemStatueModelRenderer implements SimpleSpecialModelRenderer {
   private static final Direction field_64689 = Direction.SOUTH;
   private final CopperGolemStatueModel model;
   private final Identifier texture;

   public CopperGolemStatueModelRenderer(CopperGolemStatueModel model, Identifier texture) {
      this.model = model;
      this.texture = texture;
   }

   @Override
   public void render(ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean glint, int i) {
      setAngles(matrices);
      queue.submitModel(this.model, Direction.SOUTH, matrices, RenderLayers.entityCutoutNoCull(this.texture), light, overlay, -1, null, i, null);
   }

   @Override
   public void collectVertices(Consumer<Vector3fc> consumer) {
      MatrixStack matrixStack = new MatrixStack();
      setAngles(matrixStack);
      this.model.setAngles(field_64689);
      this.model.getRootPart().collectVertices(matrixStack, consumer);
   }

   private static void setAngles(MatrixStack matrices) {
      matrices.translate(0.5F, 1.5F, 0.5F);
      matrices.scale(-1.0F, -1.0F, 1.0F);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked(Identifier texture, CopperGolemStatueBlock.Pose pose) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<CopperGolemStatueModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Identifier.CODEC.fieldOf("texture").forGetter(CopperGolemStatueModelRenderer.Unbaked::texture),
               CopperGolemStatueBlock.Pose.CODEC.fieldOf("pose").forGetter(CopperGolemStatueModelRenderer.Unbaked::pose)
            )
            .apply(instance, CopperGolemStatueModelRenderer.Unbaked::new)
      );

      public Unbaked(Oxidizable.OxidationLevel oxidationLevel, CopperGolemStatueBlock.Pose pose) {
         this(CopperGolemOxidationLevels.get(oxidationLevel).texture(), pose);
      }

      @Override
      public MapCodec<CopperGolemStatueModelRenderer.Unbaked> getCodec() {
         return CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
         CopperGolemStatueModel copperGolemStatueModel = new CopperGolemStatueModel(context.entityModelSet().getModelPart(getLayer(this.pose)));
         return new CopperGolemStatueModelRenderer(copperGolemStatueModel, this.texture);
      }

      private static EntityModelLayer getLayer(CopperGolemStatueBlock.Pose pose) {
         return switch (pose) {
            case STANDING -> EntityModelLayers.COPPER_GOLEM;
            case SITTING -> EntityModelLayers.COPPER_GOLEM_SITTING;
            case STAR -> EntityModelLayers.COPPER_GOLEM_STAR;
            case RUNNING -> EntityModelLayers.COPPER_GOLEM_RUNNING;
         };
      }
   }
}
