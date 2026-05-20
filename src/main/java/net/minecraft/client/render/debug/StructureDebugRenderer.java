package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.data.StructureDebugData;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public class StructureDebugRenderer implements DebugRenderer.Renderer {
   @Override
   public void render(double cameraX, double cameraY, double cameraZ, DebugDataStore store, Frustum frustum, float tickProgress) {
      store.forEachChunkData(DebugSubscriptionTypes.STRUCTURES, (chunkPos, list) -> {
         for (StructureDebugData structureDebugData : list) {
            GizmoDrawing.box(Box.from(structureDebugData.boundingBox()), DrawStyle.stroked(ColorHelper.fromFloats(1.0F, 1.0F, 1.0F, 1.0F)));

            for (StructureDebugData.Piece piece : structureDebugData.pieces()) {
               if (piece.isStart()) {
                  GizmoDrawing.box(Box.from(piece.boundingBox()), DrawStyle.stroked(ColorHelper.fromFloats(1.0F, 0.0F, 1.0F, 0.0F)));
               } else {
                  GizmoDrawing.box(Box.from(piece.boundingBox()), DrawStyle.stroked(ColorHelper.fromFloats(1.0F, 0.0F, 0.0F, 1.0F)));
               }
            }
         }
      });
   }
}
