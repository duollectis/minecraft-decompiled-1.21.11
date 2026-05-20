package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class VertexRendering {
   public static void drawOutline(
      MatrixStack matrices, VertexConsumer vertexConsumers, VoxelShape shape, double offsetX, double offsetY, double offsetZ, int color, float lineWidth
   ) {
      MatrixStack.Entry entry = matrices.peek();
      shape.forEachEdge(
         (minX, minY, minZ, maxX, maxY, maxZ) -> {
            Vector3f vector3f = new Vector3f((float)(maxX - minX), (float)(maxY - minY), (float)(maxZ - minZ)).normalize();
            vertexConsumers.vertex(entry, (float)(minX + offsetX), (float)(minY + offsetY), (float)(minZ + offsetZ))
               .color(color)
               .normal(entry, vector3f)
               .lineWidth(lineWidth);
            vertexConsumers.vertex(entry, (float)(maxX + offsetX), (float)(maxY + offsetY), (float)(maxZ + offsetZ))
               .color(color)
               .normal(entry, vector3f)
               .lineWidth(lineWidth);
         }
      );
   }
}
