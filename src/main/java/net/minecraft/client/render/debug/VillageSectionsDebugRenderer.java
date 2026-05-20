package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public class VillageSectionsDebugRenderer implements DebugRenderer.Renderer {
   @Override
   public void render(double cameraX, double cameraY, double cameraZ, DebugDataStore store, Frustum frustum, float tickProgress) {
      store.forEachBlockData(DebugSubscriptionTypes.VILLAGE_SECTIONS, (blockPos, unit) -> {
         ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(blockPos);
         GizmoDrawing.box(chunkSectionPos.getCenterPos(), DrawStyle.filled(ColorHelper.fromFloats(0.15F, 0.2F, 1.0F, 0.2F)));
      });
   }
}
