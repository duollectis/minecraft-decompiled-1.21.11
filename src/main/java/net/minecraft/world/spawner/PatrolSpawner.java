package net.minecraft.world.spawner;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.rule.GameRules;

/**
 * Спаунер патрулей пиллагеров. Периодически создаёт группу пиллагеров
 * вдали от деревень, если правило {@link GameRules#SPAWN_PATROLS} включено.
 * Размер патруля зависит от локальной сложности.
 */
public class PatrolSpawner implements SpecialSpawner {

	private static final int BASE_COOLDOWN = 12000;
	private static final int COOLDOWN_JITTER = 1200;
	private static final int SPAWN_CHANCE = 5;
	private static final int OFFSET_MIN = 24;
	private static final int OFFSET_MAX = 24;
	private static final int REGION_CHECK_RADIUS = 10;
	private static final int SCATTER_RANGE = 5;

	private int cooldown;

	@Override
	public void spawn(ServerWorld world, boolean spawnMonsters) {
		if (!spawnMonsters) {
			return;
		}

		if (!world.getGameRules().getValue(GameRules.SPAWN_PATROLS)) {
			return;
		}

		Random random = world.random;
		cooldown--;

		if (cooldown > 0) {
			return;
		}

		cooldown = cooldown + BASE_COOLDOWN + random.nextInt(COOLDOWN_JITTER);

		if (!world.isDay()) {
			return;
		}

		if (random.nextInt(SPAWN_CHANCE) != 0) {
			return;
		}

		int playerCount = world.getPlayers().size();

		if (playerCount < 1) {
			return;
		}

		PlayerEntity player = world.getPlayers().get(random.nextInt(playerCount));

		if (player.isSpectator()) {
			return;
		}

		if (world.isNearOccupiedPointOfInterest(player.getBlockPos(), 2)) {
			return;
		}

		int offsetX = (OFFSET_MIN + random.nextInt(OFFSET_MAX)) * (random.nextBoolean() ? -1 : 1);
		int offsetZ = (OFFSET_MIN + random.nextInt(OFFSET_MAX)) * (random.nextBoolean() ? -1 : 1);
		BlockPos.Mutable spawnPos = player.getBlockPos().mutableCopy().move(offsetX, 0, offsetZ);

		if (!world.isRegionLoaded(
			spawnPos.getX() - REGION_CHECK_RADIUS,
			spawnPos.getZ() - REGION_CHECK_RADIUS,
			spawnPos.getX() + REGION_CHECK_RADIUS,
			spawnPos.getZ() + REGION_CHECK_RADIUS
		)) {
			return;
		}

		if (!world.getEnvironmentAttributes().getAttributeValue(
			EnvironmentAttributes.CAN_PILLAGER_PATROL_SPAWN_GAMEPLAY, spawnPos
		)) {
			return;
		}

		int patrolSize = (int) Math.ceil(world.getLocalDifficulty(spawnPos).getLocalDifficulty()) + 1;

		for (int index = 0; index < patrolSize; index++) {
			spawnPos.setY(world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawnPos).getY());

			if (index == 0) {
				if (!spawnPillager(world, spawnPos, random, true)) {
					break;
				}
			} else {
				spawnPillager(world, spawnPos, random, false);
			}

			spawnPos.setX(spawnPos.getX() + random.nextInt(SCATTER_RANGE) - random.nextInt(SCATTER_RANGE));
			spawnPos.setZ(spawnPos.getZ() + random.nextInt(SCATTER_RANGE) - random.nextInt(SCATTER_RANGE));
		}
	}

	/**
	 * Спаунит одного пиллагера на указанной позиции.
	 * Первый пиллагер в патруле становится капитаном и получает случайную цель патрулирования.
	 *
	 * @param world   серверный мир
	 * @param pos     позиция спауна
	 * @param random  источник случайности
	 * @param captain {@code true}, если пиллагер должен быть капитаном патруля
	 * @return {@code true}, если пиллагер успешно заспаунен
	 */
	private boolean spawnPillager(ServerWorld world, BlockPos pos, Random random, boolean captain) {
		BlockState blockState = world.getBlockState(pos);

		if (!SpawnHelper.isClearForSpawn(world, pos, blockState, blockState.getFluidState(), EntityType.PILLAGER)) {
			return false;
		}

		if (!PatrolEntity.canSpawn(EntityType.PILLAGER, world, SpawnReason.PATROL, pos, random)) {
			return false;
		}

		PatrolEntity pillager = EntityType.PILLAGER.create(world, SpawnReason.PATROL);

		if (pillager == null) {
			return false;
		}

		if (captain) {
			pillager.setPatrolLeader(true);
			pillager.setRandomPatrolTarget();
		}

		pillager.setPosition(pos.getX(), pos.getY(), pos.getZ());
		pillager.initialize(world, world.getLocalDifficulty(pos), SpawnReason.PATROL, null);
		world.spawnEntityAndPassengers(pillager);
		return true;
	}
}
