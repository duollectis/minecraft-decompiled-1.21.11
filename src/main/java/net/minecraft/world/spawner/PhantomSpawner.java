package net.minecraft.world.spawner;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.rule.GameRules;

/**
 * Спаунер фантомов. Периодически пытается заспаунить фантомов над игроками,
 * которые не спали достаточно долго. Количество фантомов зависит от глобальной сложности.
 * Спаун происходит только ночью при достаточной темноте или в измерениях без неба.
 */
public class PhantomSpawner implements SpecialSpawner {

	/**
	 * Минимальное время без сна (в тиках), после которого возможен спаун фантомов.
	 * Соответствует 3 игровым суткам (72000 тиков).
	 */
	private static final int MIN_TICKS_WITHOUT_SLEEP = 72000;
	private static final int MIN_AMBIENT_DARKNESS = 5;
	private static final int SPAWN_HEIGHT_OFFSET_MIN = 20;
	private static final int SPAWN_HEIGHT_OFFSET_MAX = 15;
	private static final int SPAWN_HORIZONTAL_SPREAD = 10;
	private static final int SPAWN_HORIZONTAL_RANGE = 21;
	private static final int BASE_COOLDOWN_SECONDS_MIN = 60;
	private static final int BASE_COOLDOWN_SECONDS_MAX = 60;
	private static final int TICKS_PER_SECOND = 20;

	private int cooldown;

	@Override
	public void spawn(ServerWorld world, boolean spawnMonsters) {
		if (!spawnMonsters) {
			return;
		}

		if (!world.getGameRules().getValue(GameRules.SPAWN_PHANTOMS)) {
			return;
		}

		Random random = world.random;
		cooldown--;

		if (cooldown > 0) {
			return;
		}

		cooldown = cooldown + (BASE_COOLDOWN_SECONDS_MIN + random.nextInt(BASE_COOLDOWN_SECONDS_MAX)) * TICKS_PER_SECOND;

		if (world.getAmbientDarkness() < MIN_AMBIENT_DARKNESS && world.getDimension().hasSkyLight()) {
			return;
		}

		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.isSpectator()) {
				continue;
			}

			BlockPos playerPos = player.getBlockPos();

			if (world.getDimension().hasSkyLight()
				&& (playerPos.getY() < world.getSeaLevel() || !world.isSkyVisible(playerPos))
			) {
				continue;
			}

			LocalDifficulty difficulty = world.getLocalDifficulty(playerPos);

			if (!difficulty.isHarderThan(random.nextFloat() * 3.0F)) {
				continue;
			}

			ServerStatHandler stats = player.getStatHandler();
			int ticksWithoutSleep = MathHelper.clamp(
				stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST)),
				1,
				Integer.MAX_VALUE
			);

			if (random.nextInt(ticksWithoutSleep) < MIN_TICKS_WITHOUT_SLEEP) {
				continue;
			}

			BlockPos spawnPos = playerPos
				.up(SPAWN_HEIGHT_OFFSET_MIN + random.nextInt(SPAWN_HEIGHT_OFFSET_MAX))
				.east(-SPAWN_HORIZONTAL_SPREAD + random.nextInt(SPAWN_HORIZONTAL_RANGE))
				.south(-SPAWN_HORIZONTAL_SPREAD + random.nextInt(SPAWN_HORIZONTAL_RANGE));

			BlockState blockState = world.getBlockState(spawnPos);
			FluidState fluidState = world.getFluidState(spawnPos);

			if (!SpawnHelper.isClearForSpawn(world, spawnPos, blockState, fluidState, EntityType.PHANTOM)) {
				continue;
			}

			int phantomCount = 1 + random.nextInt(difficulty.getGlobalDifficulty().getId() + 1);
			EntityData entityData = null;

			for (int index = 0; index < phantomCount; index++) {
				PhantomEntity phantom = EntityType.PHANTOM.create(world, SpawnReason.NATURAL);

				if (phantom == null) {
					continue;
				}

				phantom.refreshPositionAndAngles(spawnPos, 0.0F, 0.0F);
				entityData = phantom.initialize(world, difficulty, SpawnReason.NATURAL, entityData);
				world.spawnEntityAndPassengers(phantom);
			}
		}
	}
}
