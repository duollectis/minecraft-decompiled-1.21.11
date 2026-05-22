package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.floatprovider.FloatProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.CaveSurface;
import net.minecraft.world.gen.feature.util.DripstoneHelper;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Генерирует большую сталактит/сталагмит пару из блоков дрипстоуна.
 * Оба элемента могут быть смещены горизонтально «ветром» для органичного вида.
 * Радиус колонны ограничен соотношением высоты пещеры к конфигурационному параметру.
 */
public class LargeDripstoneFeature extends Feature<LargeDripstoneFeatureConfig> {

	private static final int MIN_CAVE_HEIGHT = 4;

	public LargeDripstoneFeature(Codec<LargeDripstoneFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<LargeDripstoneFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		LargeDripstoneFeatureConfig config = context.getConfig();
		Random random = context.getRandom();

		if (!DripstoneHelper.canGenerate(world, origin)) {
			return false;
		}

		Optional<CaveSurface> surfaceOpt = CaveSurface.create(
			world,
			origin,
			config.floorToCeilingSearchRange,
			DripstoneHelper::canGenerate,
			DripstoneHelper::canReplaceOrLava
		);

		if (surfaceOpt.isEmpty() || !(surfaceOpt.get() instanceof CaveSurface.Bounded bounded)) {
			return false;
		}

		if (bounded.getHeight() < MIN_CAVE_HEIGHT) {
			return false;
		}

		int maxRadius = (int) (bounded.getHeight() * config.maxColumnRadiusToCaveHeightRatio);
		int clampedRadius = MathHelper.clamp(maxRadius, config.columnRadius.getMin(), config.columnRadius.getMax());
		int radius = MathHelper.nextBetween(random, config.columnRadius.getMin(), clampedRadius);

		DripstoneGenerator stalactite = createGenerator(
			origin.withY(bounded.getCeiling() - 1),
			false,
			random,
			radius,
			config.stalactiteBluntness,
			config.heightScale
		);
		DripstoneGenerator stalagmite = createGenerator(
			origin.withY(bounded.getFloor() + 1),
			true,
			random,
			radius,
			config.stalagmiteBluntness,
			config.heightScale
		);

		WindModifier wind = (stalactite.generateWind(config) && stalagmite.generateWind(config))
			? new WindModifier(origin.getY(), random, config.windSpeed)
			: WindModifier.create();

		if (stalactite.canGenerate(world, wind)) {
			stalactite.generate(world, random, wind);
		}

		if (stalagmite.canGenerate(world, wind)) {
			stalagmite.generate(world, random, wind);
		}

		if (SharedConstants.LARGE_DRIPSTONE) {
			testGeneration(world, origin, bounded, wind);
		}

		return true;
	}

	private static DripstoneGenerator createGenerator(
		BlockPos pos,
		boolean isStalagmite,
		Random random,
		int scale,
		FloatProvider bluntness,
		FloatProvider heightScale
	) {
		return new DripstoneGenerator(pos, isStalagmite, scale, bluntness.get(random), heightScale.get(random));
	}

	private void testGeneration(
		StructureWorldAccess world,
		BlockPos pos,
		CaveSurface.Bounded surface,
		WindModifier wind
	) {
		world.setBlockState(wind.modify(pos.withY(surface.getCeiling() - 1)), Blocks.DIAMOND_BLOCK.getDefaultState(), 2);
		world.setBlockState(wind.modify(pos.withY(surface.getFloor() + 1)), Blocks.GOLD_BLOCK.getDefaultState(), 2);

		for (BlockPos.Mutable mutable = pos.withY(surface.getFloor() + 2).mutableCopy();
			mutable.getY() < surface.getCeiling() - 1;
			mutable.move(Direction.UP)
		) {
			BlockPos modified = wind.modify(mutable);

			if (DripstoneHelper.canGenerate(world, modified) || world.getBlockState(modified).isOf(Blocks.DRIPSTONE_BLOCK)) {
				world.setBlockState(modified, Blocks.CREEPER_HEAD.getDefaultState(), 2);
			}
		}
	}

	/**
	 * Генератор одного элемента пары (сталактит или сталагмит).
	 * Итеративно уменьшает масштаб, если не удаётся найти подходящую позицию.
	 */
	static final class DripstoneGenerator {

		private BlockPos pos;
		private final boolean isStalagmite;
		private int scale;
		private final double bluntness;
		private final double heightScale;

		DripstoneGenerator(BlockPos pos, boolean isStalagmite, int scale, double bluntness, double heightScale) {
			this.pos = pos;
			this.isStalagmite = isStalagmite;
			this.scale = scale;
			this.bluntness = bluntness;
			this.heightScale = heightScale;
		}

		private int getBaseScale() {
			return scale(0.0F);
		}

		private int getBottomY() {
			return isStalagmite ? pos.getY() : pos.getY() - getBaseScale();
		}

		private int getTopY() {
			return !isStalagmite ? pos.getY() : pos.getY() + getBaseScale();
		}

		boolean canGenerate(StructureWorldAccess world, WindModifier wind) {
			while (scale > 1) {
				BlockPos.Mutable mutable = pos.mutableCopy();
				int searchDepth = Math.min(10, getBaseScale());

				for (int step = 0; step < searchDepth; step++) {
					if (world.getBlockState(mutable).isOf(Blocks.LAVA)) {
						return false;
					}

					if (DripstoneHelper.canGenerateBase(world, wind.modify(mutable), scale)) {
						pos = mutable;
						return true;
					}

					mutable.move(isStalagmite ? Direction.DOWN : Direction.UP);
				}

				scale /= 2;
			}

			return false;
		}

		private int scale(float height) {
			return (int) DripstoneHelper.scaleHeightFromRadius((double) height, (double) scale, heightScale, bluntness);
		}

		void generate(StructureWorldAccess world, Random random, WindModifier wind) {
			for (int dx = -scale; dx <= scale; dx++) {
				for (int dz = -scale; dz <= scale; dz++) {
					float dist = MathHelper.sqrt(dx * dx + dz * dz);

					if (dist > scale) {
						continue;
					}

					int height = scale(dist);

					if (height <= 0) {
						continue;
					}

					if (random.nextFloat() < 0.2) {
						height = (int) (height * MathHelper.nextBetween(random, 0.8F, 1.0F));
					}

					BlockPos.Mutable mutable = pos.add(dx, 0, dz).mutableCopy();
					boolean started = false;
					int surfaceLimit = isStalagmite
						? world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, mutable.getX(), mutable.getZ())
						: Integer.MAX_VALUE;

					for (int step = 0; step < height && mutable.getY() < surfaceLimit; step++) {
						BlockPos modified = wind.modify(mutable);

						if (DripstoneHelper.canGenerateOrLava(world, modified)) {
							started = true;
							Block block = SharedConstants.LARGE_DRIPSTONE ? Blocks.GLASS : Blocks.DRIPSTONE_BLOCK;
							world.setBlockState(modified, block.getDefaultState(), 2);
						} else if (started && world.getBlockState(modified).isIn(BlockTags.BASE_STONE_OVERWORLD)) {
							break;
						}

						mutable.move(isStalagmite ? Direction.UP : Direction.DOWN);
					}
				}
			}
		}

		boolean generateWind(LargeDripstoneFeatureConfig config) {
			return scale >= config.minRadiusForWind && bluntness >= config.minBluntnessForWind;
		}
	}

	/**
	 * Применяет горизонтальное смещение («ветер») к позиции блока
	 * в зависимости от расстояния до базовой высоты.
	 */
	static final class WindModifier {

		private final int baseY;
		private final @Nullable Vec3d wind;

		WindModifier(int baseY, Random random, FloatProvider windProvider) {
			this.baseY = baseY;
			float strength = windProvider.get(random);
			float angle = MathHelper.nextBetween(random, 0.0F, (float) Math.PI);
			this.wind = new Vec3d(MathHelper.cos(angle) * strength, 0.0, MathHelper.sin(angle) * strength);
		}

		private WindModifier() {
			this.baseY = 0;
			this.wind = null;
		}

		static WindModifier create() {
			return new WindModifier();
		}

		BlockPos modify(BlockPos pos) {
			if (wind == null) {
				return pos;
			}

			int distFromBase = baseY - pos.getY();
			Vec3d offset = wind.multiply(distFromBase);

			return pos.add(MathHelper.floor(offset.x), 0, MathHelper.floor(offset.z));
		}
	}
}
