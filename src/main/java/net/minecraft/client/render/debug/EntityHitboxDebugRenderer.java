package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.TextGizmo;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class EntityHitboxDebugRenderer implements DebugRenderer.Renderer {
   final MinecraftClient client;

   public EntityHitboxDebugRenderer(MinecraftClient client) {
      this.client = client;
   }

   @Override
   public void render(double cameraX, double cameraY, double cameraZ, DebugDataStore store, Frustum frustum, float tickProgress) {
      if (this.client.world != null) {
         for (Entity entity : this.client.world.getEntities()) {
            if (!entity.isInvisible()
               && frustum.isVisible(entity.getBoundingBox())
               && (entity != this.client.getCameraEntity() || this.client.options.getPerspective() != Perspective.FIRST_PERSON)) {
               this.drawHitbox(entity, tickProgress, false);
               if (SharedConstants.SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES) {
                  Entity entity2 = this.getLocalServerEntity(entity);
                  if (entity2 != null) {
                     this.drawHitbox(entity, tickProgress, true);
                  } else {
                     GizmoDrawing.text(
                        "Missing Server Entity",
                        entity.getLerpedPos(tickProgress).add(0.0, entity.getBoundingBox().getLengthY() + 1.5, 0.0),
                        TextGizmo.Style.left(-65536)
                     );
                  }
               }
            }
         }
      }
   }

   private @Nullable Entity getLocalServerEntity(Entity entity) {
      IntegratedServer integratedServer = this.client.getServer();
      if (integratedServer != null) {
         ServerWorld serverWorld = integratedServer.getWorld(entity.getEntityWorld().getRegistryKey());
         if (serverWorld != null) {
            return serverWorld.getEntityById(entity.getId());
         }
      }

      return null;
   }

   private void drawHitbox(Entity entity, float tickProgress, boolean inLocalServer) {
      Vec3d vec3d = entity.getEntityPos();
      Vec3d vec3d2 = entity.getLerpedPos(tickProgress);
      Vec3d vec3d3 = vec3d2.subtract(vec3d);
      int i = inLocalServer ? -16711936 : -1;
      GizmoDrawing.box(entity.getBoundingBox().offset(vec3d3), DrawStyle.stroked(i));
      GizmoDrawing.point(vec3d2, i, 2.0F);
      Entity entity2 = entity.getVehicle();
      if (entity2 != null) {
         float f = Math.min(entity2.getWidth(), entity.getWidth()) / 2.0F;
         float g = 0.0625F;
         Vec3d vec3d4 = entity2.getPassengerRidingPos(entity).add(vec3d3);
         GizmoDrawing.box(new Box(vec3d4.x - f, vec3d4.y, vec3d4.z - f, vec3d4.x + f, vec3d4.y + 0.0625, vec3d4.z + f), DrawStyle.stroked(-256));
      }

      if (entity instanceof LivingEntity) {
         Box box = entity.getBoundingBox().offset(vec3d3);
         float g = 0.01F;
         GizmoDrawing.box(
            new Box(box.minX, box.minY + entity.getStandingEyeHeight() - 0.01F, box.minZ, box.maxX, box.minY + entity.getStandingEyeHeight() + 0.01F, box.maxZ),
            DrawStyle.stroked(-65536)
         );
      }

      if (entity instanceof EnderDragonEntity enderDragonEntity) {
         for (EnderDragonPart enderDragonPart : enderDragonEntity.getBodyParts()) {
            Vec3d vec3d5 = enderDragonPart.getEntityPos();
            Vec3d vec3d6 = enderDragonPart.getLerpedPos(tickProgress);
            Vec3d vec3d7 = vec3d6.subtract(vec3d5);
            GizmoDrawing.box(enderDragonPart.getBoundingBox().offset(vec3d7), DrawStyle.stroked(ColorHelper.fromFloats(1.0F, 0.25F, 1.0F, 0.0F)));
         }
      }

      Vec3d vec3d8 = vec3d2.add(0.0, entity.getStandingEyeHeight(), 0.0);
      Vec3d vec3d9 = entity.getRotationVec(tickProgress);
      GizmoDrawing.arrow(vec3d8, vec3d8.add(vec3d9.multiply(2.0)), -16776961);
      if (inLocalServer) {
         Vec3d vec3d4 = entity.getVelocity();
         GizmoDrawing.arrow(vec3d2, vec3d2.add(vec3d4), -256);
      }
   }
}
