package net.minecraft.client.gui.hud.debug;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Запись отладочного HUD: позиция игрока (XYZ, блок, чанк, направление взгляда).
 */
@Environment(EnvType.CLIENT)
public class PlayerPositionDebugHudEntry implements DebugHudEntry {

	public static final Identifier SECTION_ID = Identifier.ofVanilla("position");

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		Entity cameraEntity = client.getCameraEntity();

		if (cameraEntity == null) {
			return;
		}

		BlockPos blockPos = cameraEntity.getBlockPos();
		ChunkPos chunkPos = new ChunkPos(blockPos);
		Direction facing = cameraEntity.getHorizontalFacing();
		LongSet forcedChunks = world instanceof ServerWorld serverWorld
			? serverWorld.getForcedChunks()
			: LongSets.EMPTY_SET;

		String facingDescription = switch (facing) {
			case NORTH -> "Towards negative Z";
			case SOUTH -> "Towards positive Z";
			case WEST -> "Towards negative X";
			case EAST -> "Towards positive X";
			default -> "Invalid";
		};

		lines.addLinesToSection(
			SECTION_ID,
			List.of(
				String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f", cameraEntity.getX(), cameraEntity.getY(), cameraEntity.getZ()),
				String.format(Locale.ROOT, "Block: %d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ()),
				String.format(
					Locale.ROOT,
					"Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
					chunkPos.x,
					ChunkSectionPos.getSectionCoord(blockPos.getY()),
					chunkPos.z,
					chunkPos.getRegionRelativeX(),
					chunkPos.getRegionRelativeZ(),
					chunkPos.getRegionX(),
					chunkPos.getRegionZ()
				),
				String.format(
					Locale.ROOT,
					"Facing: %s (%s) (%.1f / %.1f)",
					facing,
					facingDescription,
					MathHelper.wrapDegrees(cameraEntity.getYaw()),
					MathHelper.wrapDegrees(cameraEntity.getPitch())
				),
				client.world.getRegistryKey().getValue() + " FC: " + forcedChunks.size()
			)
		);
	}
}
