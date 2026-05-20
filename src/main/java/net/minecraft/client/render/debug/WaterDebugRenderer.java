package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.TextGizmo;

@Environment(EnvType.CLIENT)
public class WaterDebugRenderer implements DebugRenderer.Renderer {
   private final MinecraftClient client;

   public WaterDebugRenderer(MinecraftClient client) {
      this.client = client;
   }

   @Override
   public void render(double cameraX, double cameraY, double cameraZ, DebugDataStore store, Frustum frustum, float tickProgress) {
      BlockPos blockPos = this.client.player.getBlockPos();
      WorldView worldView = this.client.player.getEntityWorld();

      for (BlockPos blockPos2 : BlockPos.iterate(blockPos.add(-10, -10, -10), blockPos.add(10, 10, 10))) {
         FluidState fluidState = worldView.getFluidState(blockPos2);
         if (fluidState.isIn(FluidTags.WATER)) {
            double d = blockPos2.getY() + fluidState.getHeight(worldView, blockPos2);
            GizmoDrawing.box(
               new Box(blockPos2.getX() + 0.01F, blockPos2.getY() + 0.01F, blockPos2.getZ() + 0.01F, blockPos2.getX() + 0.99F, d, blockPos2.getZ() + 0.99F),
               DrawStyle.filled(ColorHelper.fromFloats(0.15F, 0.0F, 1.0F, 0.0F))
            );
         }
      }

      for (BlockPos blockPos2x : BlockPos.iterate(blockPos.add(-10, -10, -10), blockPos.add(10, 10, 10))) {
         FluidState fluidState = worldView.getFluidState(blockPos2x);
         if (fluidState.isIn(FluidTags.WATER)) {
            GizmoDrawing.text(
               String.valueOf(fluidState.getLevel()),
               Vec3d.add(blockPos2x, 0.5, fluidState.getHeight(worldView, blockPos2x), 0.5),
               TextGizmo.Style.left(-16777216)
            );
         }
      }
   }
}
