package net.minecraft.world;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkPos;

import java.util.List;
import java.util.Map;

/**
 * Ограничитель плотности спавна мобов на основе количества мобов,
 * видимых каждым игроком в радиусе загрузки чанков.
 * <p>
 * Для каждого игрока отслеживается счётчик мобов по группам спавна.
 * Спавн разрешается, если хотя бы один игрок, наблюдающий за чанком,
 * не превысил лимит своей группы.
 */
public class SpawnDensityCapper {

	private final Long2ObjectMap<List<ServerPlayerEntity>> chunkPosToMobSpawnablePlayers = new Long2ObjectOpenHashMap<>();
	private final Map<ServerPlayerEntity, DensityCap> playersToDensityCap = Maps.newHashMap();
	private final ServerChunkLoadingManager chunkLoadingManager;

	public SpawnDensityCapper(ServerChunkLoadingManager chunkLoadingManager) {
		this.chunkLoadingManager = chunkLoadingManager;
	}

	private List<ServerPlayerEntity> getMobSpawnablePlayers(ChunkPos chunkPos) {
		return chunkPosToMobSpawnablePlayers.computeIfAbsent(
			chunkPos.toLong(),
			pos -> chunkLoadingManager.getPlayersWatchingChunk(chunkPos)
		);
	}

	/**
	 * Увеличивает счётчик плотности для всех игроков, наблюдающих за данным чанком.
	 *
	 * @param chunkPos   позиция чанка
	 * @param spawnGroup группа спавна
	 */
	public void increaseDensity(ChunkPos chunkPos, SpawnGroup spawnGroup) {
		for (ServerPlayerEntity player : getMobSpawnablePlayers(chunkPos)) {
			playersToDensityCap
				.computeIfAbsent(player, p -> new DensityCap())
				.increaseDensity(spawnGroup);
		}
	}

	/**
	 * Проверяет, разрешён ли спавн в данном чанке для указанной группы.
	 * <p>
	 * Возвращает {@code true}, если хотя бы один наблюдающий игрок
	 * не превысил лимит своей группы спавна.
	 *
	 * @param spawnGroup группа спавна
	 * @param chunkPos   позиция чанка
	 * @return {@code true} если спавн разрешён
	 */
	public boolean canSpawn(SpawnGroup spawnGroup, ChunkPos chunkPos) {
		for (ServerPlayerEntity player : getMobSpawnablePlayers(chunkPos)) {
			DensityCap densityCap = playersToDensityCap.get(player);
			if (densityCap == null || densityCap.canSpawn(spawnGroup)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Хранит счётчики мобов по группам спавна для одного игрока.
	 */
	static class DensityCap {

		private final Object2IntMap<SpawnGroup> spawnGroupsToDensity =
			new Object2IntOpenHashMap<>(SpawnGroup.values().length);

		void increaseDensity(SpawnGroup spawnGroup) {
			spawnGroupsToDensity.computeInt(spawnGroup, (group, density) -> density == null ? 1 : density + 1);
		}

		boolean canSpawn(SpawnGroup spawnGroup) {
			return spawnGroupsToDensity.getOrDefault(spawnGroup, 0) < spawnGroup.getCapacity();
		}
	}
}
