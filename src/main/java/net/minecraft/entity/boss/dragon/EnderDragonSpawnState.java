package net.minecraft.entity.boss.dragon;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.EndSpikeFeature;
import net.minecraft.world.gen.feature.EndSpikeFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

import java.util.List;

/**
 * Конечный автомат процесса призыва Эндер-дракона.
 * Каждое состояние реализует один шаг анимации: от начального луча кристаллов
 * до финального взрыва и появления дракона.
 */
public enum EnderDragonSpawnState {

	START {
		@Override
		public void run(ServerWorld world, EnderDragonFight fight, List<EndCrystalEntity> crystals, int tick, BlockPos pos) {
			BlockPos beamTarget = new BlockPos(0, BEAM_TARGET_Y, 0);
			for (EndCrystalEntity crystal : crystals) {
				crystal.setBeamTarget(beamTarget);
			}

			fight.setSpawnState(PREPARING_TO_SUMMON_PILLARS);
		}
	},

	PREPARING_TO_SUMMON_PILLARS {
		@Override
		public void run(ServerWorld world, EnderDragonFight fight, List<EndCrystalEntity> crystals, int tick, BlockPos pos) {
			if (tick >= PREPARING_DURATION) {
				fight.setSpawnState(SUMMONING_PILLARS);
				return;
			}

			if (tick == 0 || tick == 50 || tick == 51 || tick == 52 || tick >= 95) {
				world.syncWorldEvent(WORLD_EVENT_BEAM, new BlockPos(0, BEAM_TARGET_Y, 0), 0);
			}
		}
	},

	SUMMONING_PILLARS {
		@Override
		public void run(ServerWorld world, EnderDragonFight fight, List<EndCrystalEntity> crystals, int tick, BlockPos pos) {
			boolean isStartOfPillar = tick % TICKS_PER_PILLAR == 0;
			boolean isEndOfPillar = tick % TICKS_PER_PILLAR == TICKS_PER_PILLAR - 1;

			if (!isStartOfPillar && !isEndOfPillar) {
				return;
			}

			List<EndSpikeFeature.Spike> spikes = EndSpikeFeature.getSpikes(world);
			int pillarIndex = tick / TICKS_PER_PILLAR;

			if (pillarIndex < spikes.size()) {
				EndSpikeFeature.Spike spike = spikes.get(pillarIndex);
				if (isStartOfPillar) {
					BlockPos beamTarget = new BlockPos(spike.getCenterX(), spike.getHeight() + 1, spike.getCenterZ());
					for (EndCrystalEntity crystal : crystals) {
						crystal.setBeamTarget(beamTarget);
					}
				} else {
					clearAndRegeneratePillar(world, spike);
				}
			} else if (isStartOfPillar) {
				fight.setSpawnState(SUMMONING_DRAGON);
			}
		}

		/**
		 * Очищает область вокруг шипа и регенерирует его структуру через генератор фич.
		 */
		private void clearAndRegeneratePillar(ServerWorld world, EndSpikeFeature.Spike spike) {
			BlockPos minPos = new BlockPos(
					spike.getCenterX() - PILLAR_CLEAR_RADIUS,
					spike.getHeight() - PILLAR_CLEAR_RADIUS,
					spike.getCenterZ() - PILLAR_CLEAR_RADIUS
			);
			BlockPos maxPos = new BlockPos(
					spike.getCenterX() + PILLAR_CLEAR_RADIUS,
					spike.getHeight() + PILLAR_CLEAR_RADIUS,
					spike.getCenterZ() + PILLAR_CLEAR_RADIUS
			);
			for (BlockPos blockPos : BlockPos.iterate(minPos, maxPos)) {
				world.removeBlock(blockPos, false);
			}

			world.createExplosion(
					null,
					spike.getCenterX() + 0.5F,
					spike.getHeight(),
					spike.getCenterZ() + 0.5F,
					PILLAR_EXPLOSION_RADIUS,
					World.ExplosionSourceType.BLOCK
			);

			EndSpikeFeatureConfig config = new EndSpikeFeatureConfig(
					true,
					ImmutableList.of(spike),
					new BlockPos(0, BEAM_TARGET_Y, 0)
			);
			Feature.END_SPIKE.generateIfValid(
					config,
					world,
					world.getChunkManager().getChunkGenerator(),
					Random.create(),
					new BlockPos(spike.getCenterX(), PILLAR_GENERATE_Y, spike.getCenterZ())
			);
		}
	},

	SUMMONING_DRAGON {
		@Override
		public void run(ServerWorld world, EnderDragonFight fight, List<EndCrystalEntity> crystals, int tick, BlockPos pos) {
			if (tick >= DRAGON_SUMMON_DURATION) {
				fight.setSpawnState(END);
				fight.resetEndCrystals();
				for (EndCrystalEntity crystal : crystals) {
					crystal.setBeamTarget(null);
					world.createExplosion(
							crystal,
							crystal.getX(), crystal.getY(), crystal.getZ(),
							CRYSTAL_EXPLOSION_RADIUS,
							World.ExplosionSourceType.NONE
					);
					crystal.discard();
				}
			} else if (tick >= DRAGON_BEAM_START_TICK) {
				world.syncWorldEvent(WORLD_EVENT_BEAM, new BlockPos(0, BEAM_TARGET_Y, 0), 0);
			} else if (tick == 0) {
				BlockPos beamTarget = new BlockPos(0, BEAM_TARGET_Y, 0);
				for (EndCrystalEntity crystal : crystals) {
					crystal.setBeamTarget(beamTarget);
				}
			} else if (tick < DRAGON_EARLY_BEAM_END_TICK) {
				world.syncWorldEvent(WORLD_EVENT_BEAM, new BlockPos(0, BEAM_TARGET_Y, 0), 0);
			}
		}
	},

	END {
		@Override
		public void run(ServerWorld world, EnderDragonFight fight, List<EndCrystalEntity> crystals, int tick, BlockPos pos) {
		}
	};

	private static final int BEAM_TARGET_Y = 128;
	private static final int WORLD_EVENT_BEAM = 3001;
	private static final int PREPARING_DURATION = 100;
	private static final int TICKS_PER_PILLAR = 40;
	private static final int PILLAR_CLEAR_RADIUS = 10;
	private static final float PILLAR_EXPLOSION_RADIUS = 5.0F;
	private static final int PILLAR_GENERATE_Y = 45;
	private static final int DRAGON_SUMMON_DURATION = 100;
	private static final int DRAGON_BEAM_START_TICK = 80;
	private static final int DRAGON_EARLY_BEAM_END_TICK = 5;
	private static final float CRYSTAL_EXPLOSION_RADIUS = 6.0F;

	public abstract void run(
			ServerWorld world,
			EnderDragonFight fight,
			List<EndCrystalEntity> crystals,
			int tick,
			BlockPos pos
	);
}
