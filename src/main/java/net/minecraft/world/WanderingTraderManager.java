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
 * {@code WanderingTraderManager}.
 */
public class WanderingTraderManager implements SpecialSpawner {

	private static final int DEFAULT_SPAWN_TIMER = 1200;
	public static final int DEFAULT_SPAWN_DELAY = 24000;
	private static final int MIN_SPAWN_CHANCE = 25;
	private static final int MAX_SPAWN_CHANCE = 75;
	private static final int DEFAULT_SPAWN_CHANCE = 25;
	private static final int SPAWN_DELAY_MIN = 10;
	private static final int SPAWN_DELAY_MAX = 10;
	private final Random random = Random.create();
	private final ServerWorldProperties properties;
	private int spawnTimer;
	private int spawnDelay;
	private int spawnChance;

	public WanderingTraderManager(ServerWorldProperties properties) {
		this.properties = properties;
		this.spawnTimer = 1200;
		this.spawnDelay = properties.getWanderingTraderSpawnDelay();
		this.spawnChance = properties.getWanderingTraderSpawnChance();
		if (this.spawnDelay == 0 && this.spawnChance == 0) {
			this.spawnDelay = 24000;
			properties.setWanderingTraderSpawnDelay(this.spawnDelay);
			this.spawnChance = 25;
			properties.setWanderingTraderSpawnChance(this.spawnChance);
		}
	}

	@Override
	public void spawn(ServerWorld world, boolean spawnMonsters) {
		if (world.getGameRules().getValue(GameRules.SPAWN_WANDERING_TRADERS)) {
			if (--this.spawnTimer <= 0) {
				this.spawnTimer = 1200;
				this.spawnDelay -= 1200;
				this.properties.setWanderingTraderSpawnDelay(this.spawnDelay);
				if (this.spawnDelay <= 0) {
					this.spawnDelay = 24000;
					int i = this.spawnChance;
					this.spawnChance = MathHelper.clamp(this.spawnChance + 25, 25, 75);
					this.properties.setWanderingTraderSpawnChance(this.spawnChance);
					if (this.random.nextInt(100) <= i) {
						if (this.trySpawn(world)) {
							this.spawnChance = 25;
						}
					}
				}
			}
		}
	}

	private boolean trySpawn(ServerWorld world) {
		PlayerEntity playerEntity = world.getRandomAlivePlayer();
		if (playerEntity == null) {
			return true;
		}
		else if (this.random.nextInt(10) != 0) {
			return false;
		}
		else {
			BlockPos blockPos = playerEntity.getBlockPos();
			int i = 48;
			PointOfInterestStorage pointOfInterestStorage = world.getPointOfInterestStorage();
			Optional<BlockPos> optional = pointOfInterestStorage.getPosition(
					poiType -> poiType.matchesKey(PointOfInterestTypes.MEETING),
					pos -> true,
					blockPos,
					48,
					PointOfInterestStorage.OccupationStatus.ANY
			);
			BlockPos blockPos2 = optional.orElse(blockPos);
			BlockPos blockPos3 = this.getNearbySpawnPos(world, blockPos2, 48);
			if (blockPos3 != null && this.doesNotSuffocateAt(world, blockPos3)) {
				if (world.getBiome(blockPos3).isIn(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)) {
					return false;
				}

				WanderingTraderEntity
						wanderingTraderEntity =
						EntityType.WANDERING_TRADER.spawn(world, blockPos3, SpawnReason.EVENT);
				if (wanderingTraderEntity != null) {
					for (int j = 0; j < 2; j++) {
						this.spawnLlama(world, wanderingTraderEntity, 4);
					}

					this.properties.setWanderingTraderId(wanderingTraderEntity.getUuid());
					wanderingTraderEntity.setDespawnDelay(48000);
					wanderingTraderEntity.setWanderTarget(blockPos2);
					wanderingTraderEntity.setPositionTarget(blockPos2, 16);
					return true;
				}
			}

			return false;
		}
	}

	private void spawnLlama(ServerWorld world, WanderingTraderEntity wanderingTrader, int range) {
		BlockPos blockPos = this.getNearbySpawnPos(world, wanderingTrader.getBlockPos(), range);
		if (blockPos != null) {
			TraderLlamaEntity traderLlamaEntity = EntityType.TRADER_LLAMA.spawn(world, blockPos, SpawnReason.EVENT);
			if (traderLlamaEntity != null) {
				traderLlamaEntity.attachLeash(wanderingTrader, true);
			}
		}
	}

	private @Nullable BlockPos getNearbySpawnPos(WorldView world, BlockPos pos, int range) {
		BlockPos blockPos = null;
		SpawnLocation spawnLocation = SpawnRestriction.getLocation(EntityType.WANDERING_TRADER);

		for (int i = 0; i < 10; i++) {
			int j = pos.getX() + this.random.nextInt(range * 2) - range;
			int k = pos.getZ() + this.random.nextInt(range * 2) - range;
			int l = world.getTopY(Heightmap.Type.WORLD_SURFACE, j, k);
			BlockPos blockPos2 = new BlockPos(j, l, k);
			if (spawnLocation.isSpawnPositionOk(world, blockPos2, EntityType.WANDERING_TRADER)) {
				blockPos = blockPos2;
				break;
			}
		}

		return blockPos;
	}

	private boolean doesNotSuffocateAt(BlockView world, BlockPos pos) {
		for (BlockPos blockPos : BlockPos.iterate(pos, pos.add(1, 2, 1))) {
			if (!world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty()) {
				return false;
			}
		}

		return true;
	}
}
