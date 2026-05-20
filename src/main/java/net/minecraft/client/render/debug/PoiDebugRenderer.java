package net.minecraft.client.render.debug;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.NameGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.data.PoiDebugData;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public class PoiDebugRenderer implements DebugRenderer.Renderer {
   private static final int field_62976 = 30;
   private static final float field_62977 = 0.32F;
   private static final int ORANGE_COLOR = -23296;
   private final BrainDebugRenderer brainDebugRenderer;

   public PoiDebugRenderer(BrainDebugRenderer brainDebugRenderer) {
      this.brainDebugRenderer = brainDebugRenderer;
   }

   @Override
   public void render(double cameraX, double cameraY, double cameraZ, DebugDataStore store, Frustum frustum, float tickProgress) {
      BlockPos blockPos = BlockPos.ofFloored(cameraX, cameraY, cameraZ);
      store.forEachBlockData(DebugSubscriptionTypes.POIS, (blockPos2, poiDebugData) -> {
         if (blockPos.isWithinDistance(blockPos2, 30.0)) {
            accentuatePoi(blockPos2);
            this.drawPoiInfo(poiDebugData, store);
         }
      });
      this.brainDebugRenderer.getGhostPointsOfInterest(store).forEach((blockPos2, list) -> {
         if (store.getBlockData(DebugSubscriptionTypes.POIS, blockPos2) == null) {
            if (blockPos.isWithinDistance(blockPos2, 30.0)) {
               this.drawGhostPoi(blockPos2, (List<String>)list);
            }
         }
      });
   }

   private static void accentuatePoi(BlockPos blockPos) {
      float f = 0.05F;
      GizmoDrawing.box(blockPos, 0.05F, DrawStyle.filled(ColorHelper.fromFloats(0.3F, 0.2F, 0.2F, 1.0F)));
   }

   private void drawGhostPoi(BlockPos blockPos, List<String> list) {
      float f = 0.05F;
      GizmoDrawing.box(blockPos, 0.05F, DrawStyle.filled(ColorHelper.fromFloats(0.3F, 0.2F, 0.2F, 1.0F)));
      GizmoDrawing.blockLabel(list.toString(), blockPos, 0, -256, 0.32F);
      GizmoDrawing.blockLabel("Ghost POI", blockPos, 1, -65536, 0.32F);
   }

   private void drawPoiInfo(PoiDebugData poiDebugData, DebugDataStore debugDataStore) {
      int i = 0;
      if (SharedConstants.BRAIN) {
         List<String> list = this.getTicketHolders(poiDebugData, false, debugDataStore);
         if (list.size() < 4) {
            drawTextOverPoi("Owners: " + list, poiDebugData, i, -256);
         } else {
            drawTextOverPoi(list.size() + " ticket holders", poiDebugData, i, -256);
         }

         i++;
         List<String> list2 = this.getTicketHolders(poiDebugData, true, debugDataStore);
         if (list2.size() < 4) {
            drawTextOverPoi("Candidates: " + list2, poiDebugData, i, -23296);
         } else {
            drawTextOverPoi(list2.size() + " potential owners", poiDebugData, i, -23296);
         }

         i++;
      }

      drawTextOverPoi("Free tickets: " + poiDebugData.freeTicketCount(), poiDebugData, i, -256);
      drawTextOverPoi(poiDebugData.poiType().getIdAsString(), poiDebugData, ++i, -1);
   }

   private static void drawTextOverPoi(String string, PoiDebugData poiDebugData, int i, int j) {
      GizmoDrawing.blockLabel(string, poiDebugData.pos(), i, j, 0.32F);
   }

   private List<String> getTicketHolders(PoiDebugData poiData, boolean potential, DebugDataStore store) {
      List<String> list = new ArrayList<>();
      store.forEachEntityData(DebugSubscriptionTypes.BRAINS, (entity, grainData) -> {
         boolean bl2 = potential ? grainData.potentialPoiContains(poiData.pos()) : grainData.poiContains(poiData.pos());
         if (bl2) {
            list.add(NameGenerator.name(entity.getUuid()));
         }
      });
      return list;
   }
}
