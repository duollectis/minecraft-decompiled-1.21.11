package net.minecraft.client.render.entity.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class EntityRenderState implements FabricRenderState {
   public static final int NO_OUTLINE = 0;
   public EntityType<?> entityType;
   public double x;
   public double y;
   public double z;
   public float age;
   public float width;
   public float height;
   public float standingEyeHeight;
   public double squaredDistanceToCamera;
   public boolean invisible;
   public boolean sneaking;
   public boolean onFire;
   public int light = 15728880;
   public int outlineColor = 0;
   public @Nullable Vec3d positionOffset;
   public @Nullable Text displayName;
   public @Nullable Vec3d nameLabelPos;
   public @Nullable List<EntityRenderState.LeashData> leashDatas;
   public float shadowRadius;
   public final List<EntityRenderState.ShadowPiece> shadowPieces = new ArrayList<>();

   public boolean hasOutline() {
      return this.outlineColor != 0;
   }

   public void addCrashReportDetails(CrashReportSection crashReportSection) {
      crashReportSection.add("EntityRenderState", this.getClass().getCanonicalName());
      crashReportSection.add("Entity's Exact location", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.x, this.y, this.z));
   }

   @Environment(EnvType.CLIENT)
   public static class LeashData {
      public Vec3d offset = Vec3d.ZERO;
      public Vec3d startPos = Vec3d.ZERO;
      public Vec3d endPos = Vec3d.ZERO;
      public int leashedEntityBlockLight = 0;
      public int leashHolderBlockLight = 0;
      public int leashedEntitySkyLight = 15;
      public int leashHolderSkyLight = 15;
      public boolean slack = true;
   }

   @Environment(EnvType.CLIENT)
   public record ShadowPiece(float relativeX, float relativeY, float relativeZ, VoxelShape shapeBelow, float alpha) {
   }
}
