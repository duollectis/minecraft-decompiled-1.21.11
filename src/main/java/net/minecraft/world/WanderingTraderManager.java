package net.minecraft.world;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocation;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.spawner.SpecialSpawner;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Управляет логикой появления странствующего торговца в мире.
 *
 * <p>Торговец появляется с нарастающей вероятностью: каждые {@link #DEFAULT_SPAWN_DELAY} тиков
 * шанс спавна увеличивается на {@link #MIN_SPAWN_CHANCE}% (от 25% до 75%). После успешного
 * спавна шанс сбрасывается обратно до минимума. Таймер тикает каждые {@link #SPAWN_TIMER_INTERVAL}
 * тиков, уменьшая задержку до следующей попытки.</p>
 */
public class WanderingTraderManager implements SpecialSpawner {

	private static final int SPAWN_TIMER_INTERVAL = 1200;
	public static final int DEFAULT_SPAWN_DELAY = 24000;
	private static final int MIN_SPAWN_CHANCE = 25;
	private static final int MAX_SPAWN_CHANCE = 75;
	private static final int SPAWN_SEARCH_RADIUS = 48;
	private static final int LLAMA_SPAWN_RADIUS = 4;
	private static final int LLAMA_COUNT = 2;
	private static final int MAX_SPAWN_ATTEMPTS = 10;
	private static final int SPAWN_RANDOM_SKIP_CHANCE = 10;
	private static final int DESPAWN_DELAY_TICKS = 48000;
	private static final int WANDER_TARGET_RANGE = 16;

	private final Random random = Random.create();
	private final ServerWorldProperties properties;
	private int spawnTimer;
	private int spawnDelay;
	private int spawnChance;

	public WanderingTraderManager(ServerWorldProperties properties) {
		this.properties = properties;
		spawnTimer = SPAWN_TIMER_INTERVAL;
		spawnDelay = properties.getWanderingTraderSpawnDelay();
		spawnChance = properties.getWanderingTraderSpawnChance();

		if (spawnDelay == 0 && spawnChance == 0) {
			spawnDelay = DEFAULT_SPAWN_DELAY;
			properties.setWanderingTraderSpawnDelay(spawnDelay);
			spawnChance = MIN_SPAWN_CHANCE;
			properties.setWanderingTraderSpawnChance(spawnChance);
		}
	}

	@Override
	public void spawn(ServerWorld world, boolean spawnMonsters) {
		if (!world.getGameRules().getValue(GameRules.SPAWN_WANDERING_TRADERS)) {
			return;
		}

		if (--spawnTimer > 0) {
			return;
		}

		spawnTimer = SPAWN_TIMER_INTERVAL;
		spawnDelay -= SPAWN_TIMER_INTERVAL;
		properties.setWanderingTraderSpawnDelay(spawnDelay);

		if (spawnDelay > 0) {
			return;
		}

		spawnDelay = DEFAULT_SPAWN_DELAY;
		int currentChance = spawnChance;
		spawnChance = MathHelper.clamp(spawnChance + MIN_SPAWN_CHANCE, MIN_SPAWN_CHANCE, MAX_SPAWN_CHANCE);
		properties.setWanderingTraderSpawnChance(spawnChance);

		if (random.nextInt(100) > currentChance) {
			return;
		}

		if (trySpawn(world)) {
			spawnChance = MIN_SPAWN_CHANCE;
		}
	}

	private boolean trySpawn(ServerWorld world) {
		PlayerEntity player = world.getRandomAlivePlayer();
		if (player == null) {
			return true;
		}

		if (random.nextInt(SPAWN_RANDOM_SKIP_CHANCE) != 0) {
			return false;
		}

		BlockPos playerPos = player.getBlockPos();
		BlockPos meetingPos = world.getPointOfInterestStorage()
				.getPosition(
						poiType -> poiType.matchesKey(PointOfInterestTypes.MEETING),
						pos -> true,
						playerPos,
						SPAWN_SEARCH_RADIUS,
						PointOfInterestStorage.OccupationStatus.ANY
				)
				.orElse(playerPos);

		BlockPos spawnPos = getNearbySpawnPos(world, meetingPos, SPAWN_SEARCH_RADIUS);
		if (spawnPos == null || !doesNotSuffocateAt(world, spawnPos)) {
			return false;
		}

		if (world.getBiome(spawnPos).isIn(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)) {
			return false;
		}

		WanderingTraderEntity trader = EntityType.WANDERING_TRADER.spawn(world, spawnPos, SpawnReason.EVENT);
		if (trader == null) {
			return false;
		}

		for (int index = 0; index < LLAMA_COUNT; index++) {
			spawnLlama(world, trader, LLAMA_SPAWN_RADIUS);
		}

		properties.setWanderingTraderId(trader.getUuid());
		trader.setDespawnDelay(DESPAWN_DELAY_TICKS);
		trader.setWanderTarget(meetingPos);
		trader.setPositionTarget(meetingPos, WANDER_TARGET_RANGE);

		return true;
	}

	private void spawnLlama(ServerWorld world, WanderingTraderEntity wanderingTrader, int range) {
		BlockPos llamaPos = getNearbySpawnPos(world, wanderingTrader.getBlockPos(), range);
		if (llamaPos == null) {
			return;
		}

		TraderLlamaEntity llama = EntityType.TRADER_LLAMA.spawn(world, llamaPos, SpawnReason.EVENT);
		if (llama != null) {
			llama.attachLeash(wanderingTrader, true);
		}
	}

	private @Nullable BlockPos getNearbySpawnPos(WorldView world, BlockPos center, int range) {
		SpawnLocation spawnLocation = SpawnRestriction.getLocation(EntityType.WANDERING_TRADER);

		for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
			int x = center.getX() + random.nextInt(range * 2) - range;
			int z = center.getZ() + random.nextInt(range * 2) - range;
			int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
			BlockPos candidate = new BlockPos(x, y, z);

			if (spawnLocation.isSpawnPositionOk(world, candidate, EntityType.WANDERING_TRADER)) {
				return candidate;
			}
		}

		return null;
	}

	private boolean doesNotSuffocateAt(BlockView world, BlockPos pos) {
		for (BlockPos checkPos : BlockPos.iterate(pos, pos.add(1, 2, 1))) {
			if (!world.getBlockState(checkPos).getCollisionShape(world, checkPos).isEmpty()) {
				return false;
			}
		}

		return true;
	}
}
