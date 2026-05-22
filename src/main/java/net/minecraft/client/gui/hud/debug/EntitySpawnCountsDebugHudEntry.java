package net.minecraft.client.gui.hud.debug;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Запись отладочного HUD: количество заспавненных сущностей по группам.
 */
@Environment(EnvType.CLIENT)
public class EntitySpawnCountsDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		Entity cameraEntity = client.getCameraEntity();
		ServerWorld serverWorld = world instanceof ServerWorld sw ? sw : null;

		if (cameraEntity == null || serverWorld == null) {
			return;
		}

		ServerChunkManager chunkManager = serverWorld.getChunkManager();
		SpawnHelper.Info spawnInfo = chunkManager.getSpawnInfo();

		if (spawnInfo == null) {
			return;
		}

		Object2IntMap<SpawnGroup> groupCounts = spawnInfo.getGroupToCount();
		int spawningChunks = spawnInfo.getSpawningChunkCount();
		String groupSummary = Stream.of(SpawnGroup.values())
			.map(group -> Character.toUpperCase(group.getName().charAt(0)) + ": " + groupCounts.getInt(group))
			.collect(Collectors.joining(", "));

		lines.addLine("SC: " + spawningChunks + ", " + groupSummary);
	}
}
