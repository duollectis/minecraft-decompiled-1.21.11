package net.minecraft.client.render.gizmo;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawer;
import net.minecraft.world.debug.gizmo.TextGizmo;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public class GizmoDrawerImpl implements GizmoDrawer {
   private final GizmoDrawerImpl.Division opaque = new GizmoDrawerImpl.Division(true);
   private final GizmoDrawerImpl.Division transparent = new GizmoDrawerImpl.Division(false);
   private boolean empty = true;

   private GizmoDrawerImpl.Division getDivision(int color) {
      return ColorHelper.getAlpha(color) < 255 ? this.transparent : this.opaque;
   }

   @Override
   public void addPoint(Vec3d pos, int color, float size) {
      this.getDivision(color).points.add(new GizmoDrawerImpl.Point(pos, color, size));
      this.empty = false;
   }

   @Override
   public void addLine(Vec3d start, Vec3d end, int color, float width) {
      this.getDivision(color).lines.add(new GizmoDrawerImpl.Line(start, end, color, width));
      this.empty = false;
   }

   @Override
   public void addPolygon(Vec3d[] vertices, int color) {
      this.getDivision(color).triangleFans.add(new GizmoDrawerImpl.Polygon(vertices, color));
      this.empty = false;
   }

   @Override
   public void addQuad(Vec3d a, Vec3d b, Vec3d c, Vec3d d, int color) {
      this.getDivision(color).quads.add(new GizmoDrawerImpl.Quad(a, b, c, d, color));
      this.empty = false;
   }

   @Override
   public void addText(Vec3d pos, String text, TextGizmo.Style style) {
      this.getDivision(style.color()).texts.add(new GizmoDrawerImpl.Text(pos, text, style));
      this.empty = false;
   }

   public void draw(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CameraRenderState cameraRenderState, Matrix4f posMatrix) {
      this.opaque.draw(matrices, vertexConsumers, cameraRenderState, posMatrix);
      this.transparent.draw(matrices, vertexConsumers, cameraRenderState, posMatrix);
   }

   public boolean isEmpty() {
      return this.empty;
   }

   @Environment(EnvType.CLIENT)
   record Division(
      boolean opaque,
      List<GizmoDrawerImpl.Line> lines,
      List<GizmoDrawerImpl.Quad> quads,
      List<GizmoDrawerImpl.Polygon> triangleFans,
      List<GizmoDrawerImpl.Text> texts,
      List<GizmoDrawerImpl.Point> points
   ) {

      Division(boolean opaque) {
         this(opaque, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
      }

      public void draw(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CameraRenderState cameraRenderState, Matrix4f posMatrix) {
         this.drawQuads(matrices, vertexConsumers, cameraRenderState);
         this.drawTriangleFans(matrices, vertexConsumers, cameraRenderState);
         this.drawLines(matrices, vertexConsumers, cameraRenderState, posMatrix);
         this.drawText(matrices, vertexConsumers, cameraRenderState);
         this.drawPoints(matrices, vertexConsumers, cameraRenderState);
      }

      private void drawText(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CameraRenderState cameraRenderState) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         TextRenderer textRenderer = minecraftClient.textRenderer;
         if (cameraRenderState.initialized) {
            double d = cameraRenderState.pos.getX();
            double e = cameraRenderState.pos.getY();
            double f = cameraRenderState.pos.getZ();

            for (GizmoDrawerImpl.Text text : this.texts) {
               matrices.push();
               matrices.translate((float)(text.pos().getX() - d), (float)(text.pos().getY() - e), (float)(text.pos().getZ() - f));
               matrices.multiply(cameraRenderState.orientation);
               matrices.scale(text.style.scale() / 16.0F, -text.style.scale() / 16.0F, text.style.scale() / 16.0F);
               float g;
               if (text.style.adjustLeft().isEmpty()) {
                  g = -textRenderer.getWidth(text.text) / 2.0F;
               } else {
                  g = (float)(-text.style.adjustLeft().getAsDouble()) / text.style.scale();
               }

               textRenderer.draw(
                  text.text,
                  g,
                  0.0F,
                  text.style.color(),
                  false,
                  matrices.peek().getPositionMatrix(),
                  vertexConsumers,
                  TextRenderer.TextLayerType.NORMAL,
                  0,
                  15728880
               );
               matrices.pop();
            }
         }
      }

      private void drawLines(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CameraRenderState cameraRenderState, Matrix4f posMatrix) {
         VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.opaque ? RenderLayers.lines() : RenderLayers.linesTranslucent());
         MatrixStack.Entry entry = matrices.peek();
         Vector4f vector4f = new Vector4f();
         Vector4f vector4f2 = new Vector4f();
         Vector4f vector4f3 = new Vector4f();
         Vector4f vector4f4 = new Vector4f();
         Vector4f vector4f5 = new Vector4f();
         double d = cameraRenderState.pos.getX();
         double e = cameraRenderState.pos.getY();
         double f = cameraRenderState.pos.getZ();

         for (GizmoDrawerImpl.Line line : this.lines) {
            vector4f.set(line.start().getX() - d, line.start().getY() - e, line.start().getZ() - f, 1.0);
            vector4f2.set(line.end().getX() - d, line.end().getY() - e, line.end().getZ() - f, 1.0);
            vector4f.mul(posMatrix, vector4f3);
            vector4f2.mul(posMatrix, vector4f4);
            boolean bl = vector4f3.z > -0.05F;
            boolean bl2 = vector4f4.z > -0.05F;
            if (!bl || !bl2) {
               if (bl || bl2) {
                  float g = vector4f4.z - vector4f3.z;
                  if (Math.abs(g) < 1.0E-9F) {
                     continue;
                  }

                  float h = MathHelper.clamp((-0.05F - vector4f3.z) / g, 0.0F, 1.0F);
                  vector4f.lerp(vector4f2, h, vector4f5);
                  if (bl) {
                     vector4f.set(vector4f5);
                  } else {
                     vector4f2.set(vector4f5);
                  }
               }

               vertexConsumer.vertex(entry, vector4f.x, vector4f.y, vector4f.z)
                  .normal(entry, vector4f2.x - vector4f.x, vector4f2.y - vector4f.y, vector4f2.z - vector4f.z)
                  .color(line.color())
                  .lineWidth(line.width());
               vertexConsumer.vertex(entry, vector4f2.x, vector4f2.y, vector4f2.z)
                  .normal(entry, vector4f2.x - vector4f.x, vector4f2.y - vector4f.y, vector4f2.z - vector4f.z)
                  .color(line.color())
                  .lineWidth(line.width());
            }
         }
      }

      private void drawTriangleFans(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CameraRenderState cameraRenderState) {
         MatrixStack.Entry entry = matrices.peek();
         double d = cameraRenderState.pos.getX();
         double e = cameraRenderState.pos.getY();
         double f = cameraRenderState.pos.getZ();

         for (GizmoDrawerImpl.Polygon polygon : this.triangleFans) {
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayers.debugTriangleFan());

            for (Vec3d vec3d : polygon.points()) {
               vertexConsumer.vertex(entry, (float)(vec3d.getX() - d), (float)(vec3d.getY() - e), (float)(vec3d.getZ() - f)).color(polygon.color());
            }
         }
      }

      private void drawQuads(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CameraRenderState cameraRenderState) {
         VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayers.debugFilledBox());
         MatrixStack.Entry entry = matrices.peek();
         double d = cameraRenderState.pos.getX();
         double e = cameraRenderState.pos.getY();
         double f = cameraRenderState.pos.getZ();

         for (GizmoDrawerImpl.Quad quad : this.quads) {
            vertexConsumer.vertex(entry, (float)(quad.a().getX() - d), (float)(quad.a().getY() - e), (float)(quad.a().getZ() - f)).color(quad.color());
            vertexConsumer.vertex(entry, (float)(quad.b().getX() - d), (float)(quad.b().getY() - e), (float)(quad.b().getZ() - f)).color(quad.color());
            vertexConsumer.vertex(entry, (float)(quad.c().getX() - d), (float)(quad.c().getY() - e), (float)(quad.c().getZ() - f)).color(quad.color());
            vertexConsumer.vertex(entry, (float)(quad.d().getX() - d), (float)(quad.d().getY() - e), (float)(quad.d().getZ() - f)).color(quad.color());
         }
      }

      private void drawPoints(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CameraRenderState cameraRenderState) {
         VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayers.debugPoint());
         MatrixStack.Entry entry = matrices.peek();
         double d = cameraRenderState.pos.getX();
         double e = cameraRenderState.pos.getY();
         double f = cameraRenderState.pos.getZ();

         for (GizmoDrawerImpl.Point point : this.points) {
            vertexConsumer.vertex(entry, (float)(point.pos.getX() - d), (float)(point.pos.getY() - e), (float)(point.pos.getZ() - f))
               .color(point.color())
               .lineWidth(point.size());
         }
      }
   }

   @Environment(EnvType.CLIENT)
   record Line(Vec3d start, Vec3d end, int color, float width) {
   }

   @Environment(EnvType.CLIENT)
   record Point(Vec3d pos, int color, float size) {
   }

   @Environment(EnvType.CLIENT)
   record Polygon(Vec3d[] points, int color) {
   }

   @Environment(EnvType.CLIENT)
   record Quad(Vec3d a, Vec3d b, Vec3d c, Vec3d d, int color) {
   }

   @Environment(EnvType.CLIENT)
   record Text(Vec3d pos, String text, TextGizmo.Style style) {
   }
}
