package net.minecraft.client.render.debug;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.data.BrainDebugData;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BrainDebugRenderer implements DebugRenderer.Renderer {
   private static final boolean field_32874 = true;
   private static final boolean field_32875 = false;
   private static final boolean field_32876 = false;
   private static final boolean field_32877 = false;
   private static final boolean field_32878 = false;
   private static final boolean field_32879 = false;
   private static final boolean field_32881 = false;
   private static final boolean field_32882 = true;
   private static final boolean field_38346 = false;
   private static final boolean field_32883 = true;
   private static final boolean field_32884 = true;
   private static final boolean field_32885 = true;
   private static final boolean field_32886 = true;
   private static final boolean field_32887 = true;
   private static final boolean field_32888 = true;
   private static final boolean field_32889 = true;
   private static final boolean field_32891 = true;
   private static final boolean field_32892 = true;
   private static final boolean field_38347 = true;
   private static final int POI_RANGE = 30;
   private static final int TARGET_ENTITY_RANGE = 8;
   private static final float DEFAULT_DRAWN_STRING_SIZE = 0.32F;
   private static final int AQUA = -16711681;
   private static final int GRAY = -3355444;
   private static final int PINK = -98404;
   private static final int ORANGE = -23296;
   private final MinecraftClient client;
   private @Nullable UUID targetedEntity;

   public BrainDebugRenderer(MinecraftClient client) {
      this.client = client;
   }

   @Override
   public void render(double cameraX, double cameraY, double cameraZ, DebugDataStore store, Frustum frustum, float tickProgress) {
      this.draw(store);
      if (!this.client.player.isSpectator()) {
         this.updateTargetedEntity();
      }
   }

   private void draw(DebugDataStore debugDataStore) {
      debugDataStore.forEachEntityData(DebugSubscriptionTypes.BRAINS, (entity, brainDebugData) -> {
         if (this.client.player.isInRange(entity, 30.0)) {
            this.drawBrain(entity, brainDebugData);
         }
      });
   }

   private void drawBrain(Entity entity, BrainDebugData brainDebugData) {
      boolean bl = this.isTargeted(entity);
      int i = 0;
      GizmoDrawing.entityLabel(entity, i, brainDebugData.name(), -1, 0.48F);
      i++;
      if (bl) {
         GizmoDrawing.entityLabel(entity, i, brainDebugData.profession() + " " + brainDebugData.xp() + " xp", -1, 0.32F);
         i++;
      }

      if (bl) {
         int j = brainDebugData.health() < brainDebugData.maxHealth() ? -23296 : -1;
         GizmoDrawing.entityLabel(
            entity,
            i,
            "health: " + String.format(Locale.ROOT, "%.1f", brainDebugData.health()) + " / " + String.format(Locale.ROOT, "%.1f", brainDebugData.maxHealth()),
            j,
            0.32F
         );
         i++;
      }

      if (bl && !brainDebugData.inventory().equals("")) {
         GizmoDrawing.entityLabel(entity, i, brainDebugData.inventory(), -98404, 0.32F);
         i++;
      }

      if (bl) {
         for (String string : brainDebugData.behaviors()) {
            GizmoDrawing.entityLabel(entity, i, string, -16711681, 0.32F);
            i++;
         }
      }

      if (bl) {
         for (String string : brainDebugData.activities()) {
            GizmoDrawing.entityLabel(entity, i, string, -16711936, 0.32F);
            i++;
         }
      }

      if (brainDebugData.wantsGolem()) {
         GizmoDrawing.entityLabel(entity, i, "Wants Golem", -23296, 0.32F);
         i++;
      }

      if (bl && brainDebugData.angerLevel() != -1) {
         GizmoDrawing.entityLabel(entity, i, "Anger Level: " + brainDebugData.angerLevel(), -98404, 0.32F);
         i++;
      }

      if (bl) {
         for (String string : brainDebugData.gossips()) {
            if (string.startsWith(brainDebugData.name())) {
               GizmoDrawing.entityLabel(entity, i, string, -1, 0.32F);
            } else {
               GizmoDrawing.entityLabel(entity, i, string, -23296, 0.32F);
            }

            i++;
         }
      }

      if (bl) {
         for (String string : Lists.reverse(brainDebugData.memories())) {
            GizmoDrawing.entityLabel(entity, i, string, -3355444, 0.32F);
            i++;
         }
      }
   }

   private boolean isTargeted(Entity entity) {
      return Objects.equals(this.targetedEntity, entity.getUuid());
   }

   public Map<BlockPos, List<String>> getGhostPointsOfInterest(DebugDataStore store) {
      Map<BlockPos, List<String>> map = Maps.newHashMap();
      store.forEachEntityData(DebugSubscriptionTypes.BRAINS, (entity, data) -> {
         for (BlockPos blockPos : Iterables.concat(data.pois(), data.potentialPois())) {
            map.computeIfAbsent(blockPos, pos -> Lists.newArrayList()).add(data.name());
         }
      });
      return map;
   }

   private void updateTargetedEntity() {
      DebugRenderer.getTargetedEntity(this.client.getCameraEntity(), 8).ifPresent(entity -> this.targetedEntity = entity.getUuid());
   }
}
