package net.minecraft.client.render.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LabelCommandRenderer {
   public void render(BatchingRenderCommandQueue queue, VertexConsumerProvider.Immediate vertexConsumers, TextRenderer renderer) {
      LabelCommandRenderer.Commands commands = queue.getLabelCommands();
      commands.seethroughLabels.sort(Comparator.comparing(OrderedRenderCommandQueueImpl.LabelCommand::distanceToCameraSq).reversed());

      for (OrderedRenderCommandQueueImpl.LabelCommand labelCommand : commands.seethroughLabels) {
         renderer.draw(
            labelCommand.text(),
            labelCommand.x(),
            labelCommand.y(),
            labelCommand.color(),
            false,
            labelCommand.matricesEntry(),
            vertexConsumers,
            TextRenderer.TextLayerType.SEE_THROUGH,
            labelCommand.backgroundColor(),
            labelCommand.lightCoords()
         );
      }

      for (OrderedRenderCommandQueueImpl.LabelCommand labelCommand : commands.normalLabels) {
         renderer.draw(
            labelCommand.text(),
            labelCommand.x(),
            labelCommand.y(),
            labelCommand.color(),
            false,
            labelCommand.matricesEntry(),
            vertexConsumers,
            TextRenderer.TextLayerType.NORMAL,
            labelCommand.backgroundColor(),
            labelCommand.lightCoords()
         );
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Commands {
      final List<OrderedRenderCommandQueueImpl.LabelCommand> seethroughLabels = new ArrayList<>();
      final List<OrderedRenderCommandQueueImpl.LabelCommand> normalLabels = new ArrayList<>();

      public void add(
         MatrixStack matrices,
         @Nullable Vec3d pos,
         int y,
         Text label,
         boolean notSneaking,
         int light,
         double squaredDistanceToCamera,
         CameraRenderState cameraState
      ) {
         if (pos != null) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            matrices.push();
            matrices.translate(pos.x, pos.y + 0.5, pos.z);
            matrices.multiply(cameraState.orientation);
            matrices.scale(0.025F, -0.025F, 0.025F);
            Matrix4f matrix4f = new Matrix4f(matrices.peek().getPositionMatrix());
            float f = -minecraftClient.textRenderer.getWidth(label) / 2.0F;
            int i = (int)(minecraftClient.options.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;
            if (notSneaking) {
               this.normalLabels
                  .add(
                     new OrderedRenderCommandQueueImpl.LabelCommand(
                        matrix4f, f, y, label, LightmapTextureManager.applyEmission(light, 2), -1, 0, squaredDistanceToCamera
                     )
                  );
               this.seethroughLabels.add(new OrderedRenderCommandQueueImpl.LabelCommand(matrix4f, f, y, label, light, -2130706433, i, squaredDistanceToCamera));
            } else {
               this.normalLabels.add(new OrderedRenderCommandQueueImpl.LabelCommand(matrix4f, f, y, label, light, -2130706433, i, squaredDistanceToCamera));
            }

            matrices.pop();
         }
      }

      public void clear() {
         this.normalLabels.clear();
         this.seethroughLabels.clear();
      }
   }
}
