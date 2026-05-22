package net.minecraft.world.spawner;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.List;

/**
 * Спаунер кошек. Периодически пытается заспаунить кошку рядом со случайным игроком:
 * <ul>
 *   <li>в домах деревни (если рядом есть занятые POI типа HOME)</li>
 *   <li>в структурах, помеченных тегом {@link StructureTags#CATS_SPAWN_IN}</li>
 * </ul>
 */
public class CatSpawner implements SpecialSpawner {

	private static final int SPAWN_INTERVAL = 1200;
	private static final int SPAWN_OFFSET_MIN = 8;
	private static final int SPAWN_OFFSET_MAX = 24;
	private static final int REGION_CHECK_RADIUS = 10;
	private static final int VILLAGE_POI_SEARCH_RADIUS = 48;
	private static final int MAX_CATS_IN_VILLAGE = 5;
	private static final int MIN_VILLAGE_POI_COUNT = 4;
	private static final int STRUCTURE_SEARCH_RADIUS = 16;

	private int cooldown;

	@Override
	public void spawn(ServerWorld world, boolean spawnMonsters) {
		cooldown--;

		if (cooldown > 0) {
			return;
		}

		cooldown = SPAWN_INTERVAL;
		PlayerEntity player = world.getRandomAlivePlayer();

		if (player == null) {
			return;
		}

		Random random = world.random;
		int offsetX = (SPAWN_OFFSET_MIN + random.nextInt(SPAWN_OFFSET_MAX)) * (random.nextBoolean() ? -1 : 1);
		int offsetZ = (SPAWN_OFFSET_MIN + random.nextInt(SPAWN_OFFSET_MAX)) * (random.nextBoolean() ? -1 : 1);
		BlockPos spawnPos = player.getBlockPos().add(offsetX, 0, offsetZ);

		if (!world.isRegionLoaded(
			spawnPos.getX() - REGION_CHECK_RADIUS,
			spawnPos.getZ() - REGION_CHECK_RADIUS,
			spawnPos.getX() + REGION_CHECK_RADIUS,
			spawnPos.getZ() + REGION_CHECK_RADIUS
		)) {
			return;
		}

		if (!SpawnRestriction.isSpawnPosAllowed(EntityType.CAT, world, spawnPos)) {
			return;
		}

		if (world.isNearOccupiedPointOfInterest(spawnPos, 2)) {
			spawnInHouse(world, spawnPos);
		} else if (world.getStructureAccessor().getStructureContaining(spawnPos, StructureTags.CATS_SPAWN_IN).hasChildren()) {
			spawnInStructure(world, spawnPos);
		}
	}

	/**
	 * Спаунит кошку в деревне, если количество занятых домов превышает порог
	 * и рядом ещё нет слишком много кошек.
	 */
	private void spawnInHouse(ServerWorld world, BlockPos pos) {
		long occupiedHomes = world.getPointOfInterestStorage().count(
			entry -> entry.matchesKey(PointOfInterestTypes.HOME),
			pos,
			VILLAGE_POI_SEARCH_RADIUS,
			PointOfInterestStorage.OccupationStatus.IS_OCCUPIED
		);

		if (occupiedHomes <= MIN_VILLAGE_POI_COUNT) {
			return;
		}

		List<CatEntity> nearbyCats = world.getNonSpectatingEntities(
			CatEntity.class,
			new Box(pos).expand(VILLAGE_POI_SEARCH_RADIUS, 8.0, VILLAGE_POI_SEARCH_RADIUS)
		);

		if (nearbyCats.size() < MAX_CATS_IN_VILLAGE) {
			spawnCat(pos, world, false);
		}
	}

	/**
	 * Спаунит кошку в структуре, если рядом ещё нет ни одной кошки.
	 */
	private void spawnInStructure(ServerWorld world, BlockPos pos) {
		List<CatEntity> nearbyCats = world.getNonSpectatingEntities(
			CatEntity.class,
			new Box(pos).expand(STRUCTURE_SEARCH_RADIUS, 8.0, STRUCTURE_SEARCH_RADIUS)
		);

		if (nearbyCats.isEmpty()) {
			spawnCat(pos, world, true);
		}
	}

	private void spawnCat(BlockPos pos, ServerWorld world, boolean persistent) {
		CatEntity cat = EntityType.CAT.create(world, SpawnReason.NATURAL);

		if (cat == null) {
			return;
		}

		cat.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, null);

		if (persistent) {
			cat.setPersistent();
		}

		cat.refreshPositionAndAngles(pos, 0.0F, 0.0F);
		world.spawnEntityAndPassengers(cat);
	}
}
