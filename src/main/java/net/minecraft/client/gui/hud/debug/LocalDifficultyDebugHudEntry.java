package net.minecraft.client.gui.hud.debug;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LocalDifficultyDebugHudEntry implements DebugHudEntry {
   @Override
   public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Entity entity = minecraftClient.getCameraEntity();
      if (entity != null && chunk != null && world instanceof ServerWorld serverWorld) {
         BlockPos blockPos = entity.getBlockPos();
         if (serverWorld.isInHeightLimit(blockPos.getY())) {
            float f = serverWorld.getMoonSize(blockPos);
            long l = chunk.getInhabitedTime();
            LocalDifficulty localDifficulty = new LocalDifficulty(serverWorld.getDifficulty(), serverWorld.getTimeOfDay(), l, f);
            lines.addLine(
               String.format(
                  Locale.ROOT,
                  "Local Difficulty: %.2f // %.2f (Day %d)",
                  localDifficulty.getLocalDifficulty(),
                  localDifficulty.getClampedLocalDifficulty(),
                  serverWorld.getDay()
               )
            );
         }
      }
   }
}
