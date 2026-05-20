package net.minecraft.client.render.debug;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public class CollisionDebugRenderer implements DebugRenderer.Renderer {
   private final MinecraftClient client;
   private double lastUpdateTime = Double.MIN_VALUE;
   private List<VoxelShape> collisions = Collections.emptyList();

   public CollisionDebugRenderer(MinecraftClient client) {
      this.client = client;
   }

   @Override
   public void render(double cameraX, double cameraY, double cameraZ, DebugDataStore store, Frustum frustum, float tickProgress) {
      double d = Util.getMeasuringTimeNano();
      if (d - this.lastUpdateTime > 1.0E8) {
         this.lastUpdateTime = d;
         Entity entity = this.client.gameRenderer.getCamera().getFocusedEntity();
         this.collisions = ImmutableList.copyOf(entity.getEntityWorld().getCollisions(entity, entity.getBoundingBox().expand(6.0)));
      }

      for (VoxelShape voxelShape : this.collisions) {
         DrawStyle drawStyle = DrawStyle.stroked(-1);

         for (Box box : voxelShape.getBoundingBoxes()) {
            GizmoDrawing.box(box, drawStyle);
         }
      }
   }
}
