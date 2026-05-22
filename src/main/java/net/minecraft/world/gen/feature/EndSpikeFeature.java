package net.minecraft.world.gen.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/** Генерирует 10 обсидиановых шипов Края с кристаллами Края на вершинах; кэширует позиции шипов по сиду мира. */
public class EndSpikeFeature extends Feature<EndSpikeFeatureConfig> {

	public static final int COUNT = 10;
	private static final int DISTANCE_FROM_ORIGIN = 42;
	private static final int CACHE_EXPIRE_MINUTES = 5;
	private static final int SPIKE_AIR_CLEAR_ABOVE_Y = 65;
	private static final LoadingCache<Long, List<EndSpikeFeature.Spike>> CACHE = CacheBuilder.newBuilder()
			.expireAfterWrite(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
			.build(new EndSpikeFeature.SpikeCache());

	public EndSpikeFeature(Codec<EndSpikeFeatureConfig> codec) {
		super(codec);
	}

	public static List<EndSpikeFeature.Spike> getSpikes(StructureWorldAccess world) {
		Random random = Random.create(world.getSeed());
		long cacheKey = random.nextLong() & 65535L;
		return CACHE.getUnchecked(cacheKey);
	}

	@Override
	public boolean generate(FeatureContext<EndSpikeFeatureConfig> context) {
		EndSpikeFeatureConfig config = context.getConfig();
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		BlockPos origin = context.getOrigin();
		List<EndSpikeFeature.Spike> spikes = config.getSpikes();

		if (spikes.isEmpty()) {
			spikes = getSpikes(world);
		}

		for (EndSpikeFeature.Spike spike : spikes) {
			if (spike.isInChunk(origin)) {
				generateSpike(world, random, config, spike);
			}
		}

		return true;
	}

	private void generateSpike(
			ServerWorldAccess world,
			Random random,
			EndSpikeFeatureConfig config,
			EndSpikeFeature.Spike spike
	) {
		int radius = spike.getRadius();

		for (BlockPos pos : BlockPos.iterate(
				new BlockPos(spike.getCenterX() - radius, world.getBottomY(), spike.getCenterZ() - radius),
				new BlockPos(spike.getCenterX() + radius, spike.getHeight() + COUNT, spike.getCenterZ() + radius)
		)) {
			if (pos.getSquaredDistance(spike.getCenterX(), pos.getY(), spike.getCenterZ()) <= radius * radius + 1
					&& pos.getY() < spike.getHeight()) {
				setBlockState(world, pos, Blocks.OBSIDIAN.getDefaultState());
			} else if (pos.getY() > SPIKE_AIR_CLEAR_ABOVE_Y) {
				setBlockState(world, pos, Blocks.AIR.getDefaultState());
			}
		}

		if (spike.isGuarded()) {
			BlockPos.Mutable mutable = new BlockPos.Mutable();

			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					for (int dy = 0; dy <= 3; dy++) {
						boolean onXEdge = MathHelper.abs(dx) == 2;
						boolean onZEdge = MathHelper.abs(dz) == 2;
						boolean onTop = dy == 3;

						if (!onXEdge && !onZEdge && !onTop) {
							continue;
						}

						boolean connectNorth = (dx == -2 || dx == 2 || onTop) && dz != -2;
						boolean connectSouth = (dx == -2 || dx == 2 || onTop) && dz != 2;
						boolean connectWest = (dz == -2 || dz == 2 || onTop) && dx != -2;
						boolean connectEast = (dz == -2 || dz == 2 || onTop) && dx != 2;

						BlockState bars = Blocks.IRON_BARS.getDefaultState()
								.with(PaneBlock.NORTH, connectNorth)
								.with(PaneBlock.SOUTH, connectSouth)
								.with(PaneBlock.WEST, connectWest)
								.with(PaneBlock.EAST, connectEast);

						setBlockState(
								world,
								mutable.set(spike.getCenterX() + dx, spike.getHeight() + dy, spike.getCenterZ() + dz),
								bars
						);
					}
				}
			}
		}

		EndCrystalEntity crystal = EntityType.END_CRYSTAL.create(world.toServerWorld(), SpawnReason.STRUCTURE);

		if (crystal == null) {
			return;
		}

		crystal.setBeamTarget(config.getPos());
		crystal.setInvulnerable(config.isCrystalInvulnerable());
		crystal.refreshPositionAndAngles(
				spike.getCenterX() + 0.5,
				spike.getHeight() + 1,
				spike.getCenterZ() + 0.5,
				random.nextFloat() * 360.0F,
				0.0F
		);
		world.spawnEntity(crystal);

		BlockPos crystalPos = crystal.getBlockPos();
		setBlockState(world, crystalPos.down(), Blocks.BEDROCK.getDefaultState());
		setBlockState(world, crystalPos, FireBlock.getState(world, crystalPos));
	}

	/** Описывает один обсидиановый шип: позицию центра, радиус, высоту и наличие железной клетки. */
	public static class Spike {

		public static final Codec<EndSpikeFeature.Spike> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codec.INT.fieldOf("centerX").orElse(0).forGetter(spike -> spike.centerX),
						                    Codec.INT.fieldOf("centerZ").orElse(0).forGetter(spike -> spike.centerZ),
						                    Codec.INT.fieldOf("radius").orElse(0).forGetter(spike -> spike.radius),
						                    Codec.INT.fieldOf("height").orElse(0).forGetter(spike -> spike.height),
						                    Codec.BOOL.fieldOf("guarded").orElse(false).forGetter(spike -> spike.guarded)
				                    )
				                    .apply(instance, EndSpikeFeature.Spike::new)
		);
		private final int centerX;
		private final int centerZ;
		private final int radius;
		private final int height;
		private final boolean guarded;
		private final Box boundingBox;

		public Spike(int centerX, int centerZ, int radius, int height, boolean guarded) {
			this.centerX = centerX;
			this.centerZ = centerZ;
			this.radius = radius;
			this.height = height;
			this.guarded = guarded;
			this.boundingBox = new Box(
					centerX - radius,
					DimensionType.MIN_HEIGHT,
					centerZ - radius,
					centerX + radius,
					DimensionType.MAX_COLUMN_HEIGHT,
					centerZ + radius
			);
		}

		public boolean isInChunk(BlockPos pos) {
			return ChunkSectionPos.getSectionCoord(pos.getX()) == ChunkSectionPos.getSectionCoord(this.centerX)
					&& ChunkSectionPos.getSectionCoord(pos.getZ()) == ChunkSectionPos.getSectionCoord(this.centerZ);
		}

		public int getCenterX() {
			return this.centerX;
		}

		public int getCenterZ() {
			return this.centerZ;
		}

		public int getRadius() {
			return this.radius;
		}

		public int getHeight() {
			return this.height;
		}

		public boolean isGuarded() {
			return this.guarded;
		}

		public Box getBoundingBox() {
			return this.boundingBox;
		}
	}

	/** Загрузчик кэша: вычисляет позиции 10 шипов по кругу радиуса 42 блока вокруг центра Края. */
	static class SpikeCache extends CacheLoader<Long, List<EndSpikeFeature.Spike>> {

		@Override
		public List<EndSpikeFeature.Spike> load(Long seed) {
			IntArrayList shuffled = Util.shuffle(IntStream.range(0, COUNT), Random.create(seed));
			List<EndSpikeFeature.Spike> spikes = Lists.newArrayList();

			for (int idx = 0; idx < COUNT; idx++) {
				double angle = 2.0 * (-Math.PI + (Math.PI / COUNT) * idx);
				int centerX = MathHelper.floor(DISTANCE_FROM_ORIGIN * Math.cos(angle));
				int centerZ = MathHelper.floor(DISTANCE_FROM_ORIGIN * Math.sin(angle));
				int shuffleVal = shuffled.get(idx);
				int radius = 2 + shuffleVal / 3;
				int height = 76 + shuffleVal * 3;
				boolean guarded = shuffleVal == 1 || shuffleVal == 2;
				spikes.add(new EndSpikeFeature.Spike(centerX, centerZ, radius, height, guarded));
			}

			return spikes;
		}
	}
}
