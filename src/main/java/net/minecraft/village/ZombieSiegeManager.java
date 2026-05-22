package net.minecraft.village;

import com.mojang.logging.LogUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.spawner.SpecialSpawner;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Управляет осадой деревни зомби — ночным событием, при котором зомби
 * атакуют деревню волнами.
 * <p>
 * Осада начинается в полночь (тик 18000) с вероятностью 1/10 и продолжается
 * до тех пор, пока не будут заспавнены все {@link #SIEGE_ZOMBIE_COUNT} зомби.
 * Между спавнами зомби выдерживается пауза {@link #SPAWN_INTERVAL_TICKS} тиков.
 */
public class ZombieSiegeManager implements SpecialSpawner {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final long MIDNIGHT_TICK = 18000L;
	private static final long DAY_CYCLE_TICKS = 24000L;
	private static final int SIEGE_CHANCE = 10;
	private static final int SIEGE_ZOMBIE_COUNT = 20;
	private static final int SPAWN_INTERVAL_TICKS = 2;
	private static final int SPAWN_ATTEMPTS = 10;
	private static final float SIEGE_SPAWN_RADIUS = 32.0F;
	private static final int SPAWN_SCATTER_RADIUS = 8;

	private boolean spawned;
	private State state = State.SIEGE_DONE;
	private int remaining;
	private int countdown;
	private int startX;
	private int startY;
	private int startZ;

	@Override
	public void spawn(ServerWorld world, boolean spawnMonsters) {
		if (world.isDay() || !spawnMonsters) {
			state = State.SIEGE_DONE;
			spawned = false;
			return;
		}

		long timeOfDay = world.getTimeOfDay() % DAY_CYCLE_TICKS;

		if (timeOfDay == MIDNIGHT_TICK) {
			state = world.random.nextInt(SIEGE_CHANCE) == 0
					? State.SIEGE_TONIGHT
					: State.SIEGE_DONE;
		}

		if (state == State.SIEGE_DONE) {
			return;
		}

		if (!spawned) {
			if (!tryInitiateSiege(world)) {
				return;
			}

			spawned = true;
		}

		if (countdown > 0) {
			countdown--;
			return;
		}

		countdown = SPAWN_INTERVAL_TICKS;

		if (remaining > 0) {
			trySpawnZombie(world);
			remaining--;
		} else {
			state = State.SIEGE_DONE;
		}
	}

	/**
	 * Ищет подходящего игрока рядом с деревней и инициализирует точку спавна осады.
	 *
	 * @param world серверный мир
	 * @return {@code true}, если точка спавна успешно найдена
	 */
	private boolean tryInitiateSiege(ServerWorld world) {
		for (PlayerEntity player : world.getPlayers()) {
			if (player.isSpectator()) {
				continue;
			}

			BlockPos playerPos = player.getBlockPos();

			if (!world.isNearOccupiedPointOfInterest(playerPos)
					|| world.getBiome(playerPos).isIn(BiomeTags.WITHOUT_ZOMBIE_SIEGES)
			) {
				continue;
			}

			for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
				float angle = world.random.nextFloat() * (float) (Math.PI * 2);
				startX = playerPos.getX() + MathHelper.floor(MathHelper.cos(angle) * SIEGE_SPAWN_RADIUS);
				startY = playerPos.getY();
				startZ = playerPos.getZ() + MathHelper.floor(MathHelper.sin(angle) * SIEGE_SPAWN_RADIUS);

				if (getSpawnVector(world, new BlockPos(startX, startY, startZ)) != null) {
					countdown = 0;
					remaining = SIEGE_ZOMBIE_COUNT;
					break;
				}
			}

			return true;
		}

		return false;
	}

	private void trySpawnZombie(ServerWorld world) {
		Vec3d spawnPos = getSpawnVector(world, new BlockPos(startX, startY, startZ));

		if (spawnPos == null) {
			return;
		}

		ZombieEntity zombie;

		try {
			zombie = new ZombieEntity(world);
			zombie.initialize(
					world,
					world.getLocalDifficulty(zombie.getBlockPos()),
					SpawnReason.EVENT,
					null
			);
		} catch (Exception exception) {
			LOGGER.warn("Failed to create zombie for village siege at {}", spawnPos, exception);
			return;
		}

		zombie.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, world.random.nextFloat() * 360.0F, 0.0F);
		world.spawnEntityAndPassengers(zombie);
	}

	private @Nullable Vec3d getSpawnVector(ServerWorld world, BlockPos origin) {
		for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
			int x = origin.getX() + world.random.nextInt(16) - SPAWN_SCATTER_RADIUS;
			int z = origin.getZ() + world.random.nextInt(16) - SPAWN_SCATTER_RADIUS;
			int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
			BlockPos candidate = new BlockPos(x, y, z);

			if (world.isNearOccupiedPointOfInterest(candidate)
					&& HostileEntity.canSpawnInDark(EntityType.ZOMBIE, world, SpawnReason.EVENT, candidate, world.random)
			) {
				return Vec3d.ofBottomCenter(candidate);
			}
		}

		return null;
	}

	enum State {
		SIEGE_CAN_ACTIVATE,
		SIEGE_TONIGHT,
		SIEGE_DONE
	}
}
