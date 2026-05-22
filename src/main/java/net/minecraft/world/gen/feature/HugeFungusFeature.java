package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует огромный гриб Нижнего мира (варпед или алый).
 * Ствол может быть толстым (2×2) с 6% шансом, шляпка формируется
 * многоуровневым куполом с убывающим радиусом снизу вверх.
 * При посадке вручную (planted=true) ломает блоки на пути ствола.
 */
public class HugeFungusFeature extends Feature<HugeFungusFeatureConfig> {

	private static final float DECORATION_CHANCE = 0.06F;
	private static final float THICK_STEM_CORNER_CHANCE = 0.1F;
	private static final float VINE_CHANCE_INNER = 0.15F;
	private static final int STEM_HEIGHT_MIN = 4;
	private static final int STEM_HEIGHT_MAX = 13;
	private static final int STEM_HEIGHT_RARE_MULTIPLIER = 12;

	public HugeFungusFeature(Codec<HugeFungusFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<HugeFungusFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();
		ChunkGenerator generator = context.getGenerator();
		HugeFungusFeatureConfig config = context.getConfig();

		Block validBase = config.validBaseBlock.getBlock();
		BlockState baseState = world.getBlockState(origin.down());

		if (baseState.isOf(validBase) == false) {
			return false;
		}

		int stemHeight = MathHelper.nextInt(random, STEM_HEIGHT_MIN, STEM_HEIGHT_MAX);

		if (random.nextInt(STEM_HEIGHT_RARE_MULTIPLIER) == 0) {
			stemHeight *= 2;
		}

		if (!config.planted && origin.getY() + stemHeight + 1 >= generator.getWorldHeight()) {
			return false;
		}

		boolean thickStem = !config.planted && random.nextFloat() < DECORATION_CHANCE;

		world.setBlockState(origin, Blocks.AIR.getDefaultState(), Block.SKIP_REDRAW_AND_BLOCK_ENTITY_REPLACED_CALLBACK);
		generateStem(world, random, config, origin, stemHeight, thickStem);
		generateHat(world, random, config, origin, stemHeight, thickStem);

		return true;
	}

	private static boolean isReplaceable(
		StructureWorldAccess world,
		BlockPos pos,
		HugeFungusFeatureConfig config,
		boolean checkConfig
	) {
		if (world.testBlockState(pos, AbstractBlock.AbstractBlockState::isReplaceable)) {
			return true;
		}

		return checkConfig && config.replaceableBlocks.test(world, pos);
	}

	private void generateStem(
		StructureWorldAccess world,
		Random random,
		HugeFungusFeatureConfig config,
		BlockPos pos,
		int stemHeight,
		boolean thickStem
	) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		BlockState stemState = config.stemState;
		int halfThick = thickStem ? 1 : 0;

		for (int dx = -halfThick; dx <= halfThick; dx++) {
			for (int dz = -halfThick; dz <= halfThick; dz++) {
				boolean isCorner = thickStem && MathHelper.abs(dx) == halfThick && MathHelper.abs(dz) == halfThick;

				for (int dy = 0; dy < stemHeight; dy++) {
					mutable.set(pos, dx, dy, dz);

					if (!isReplaceable(world, mutable, config, true)) {
						continue;
					}

					if (config.planted) {
						if (!world.getBlockState(mutable.down()).isAir()) {
							world.breakBlock(mutable, true);
						}

						world.setBlockState(mutable, stemState, 3);
					} else if (isCorner) {
						if (random.nextFloat() < THICK_STEM_CORNER_CHANCE) {
							setBlockState(world, mutable, stemState);
						}
					} else {
						setBlockState(world, mutable, stemState);
					}
				}
			}
		}
	}

	private void generateHat(
		StructureWorldAccess world,
		Random random,
		HugeFungusFeatureConfig config,
		BlockPos pos,
		int hatHeight,
		boolean thickStem
	) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		boolean isWartBlock = config.hatState.isOf(Blocks.NETHER_WART_BLOCK);
		int hatStartOffset = Math.min(random.nextInt(1 + hatHeight / 3) + 5, hatHeight);
		int hatStartY = hatHeight - hatStartOffset;

		for (int dy = hatStartY; dy <= hatHeight; dy++) {
			int radius = dy < hatHeight - random.nextInt(3) ? 2 : 1;

			if (hatStartOffset > 8 && dy < hatStartY + 4) {
				radius = 3;
			}

			if (thickStem) {
				radius++;
			}

			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					boolean onXEdge = dx == -radius || dx == radius;
					boolean onZEdge = dz == -radius || dz == radius;
					boolean isInterior = !onXEdge && !onZEdge && dy != hatHeight;
					boolean isCorner = onXEdge && onZEdge;
					boolean isLowerSection = dy < hatStartY + 3;

					mutable.set(pos, dx, dy, dz);

					if (!isReplaceable(world, mutable, config, false)) {
						continue;
					}

					if (config.planted && !world.getBlockState(mutable.down()).isAir()) {
						world.breakBlock(mutable, true);
					}

					if (isLowerSection) {
						if (!isInterior) {
							placeWithOptionalVines(world, random, mutable, config.hatState, isWartBlock);
						}
					} else if (isInterior) {
						placeHatBlock(world, random, config, mutable, 0.1F, 0.2F, isWartBlock ? 0.1F : 0.0F);
					} else if (isCorner) {
						placeHatBlock(world, random, config, mutable, 0.01F, 0.7F, isWartBlock ? 0.083F : 0.0F);
					} else {
						placeHatBlock(world, random, config, mutable, 5.0E-4F, 0.98F, isWartBlock ? 0.07F : 0.0F);
					}
				}
			}
		}
	}

	private void placeHatBlock(
		WorldAccess world,
		Random random,
		HugeFungusFeatureConfig config,
		BlockPos.Mutable pos,
		float decorationChance,
		float generationChance,
		float vineChance
	) {
		if (random.nextFloat() < decorationChance) {
			setBlockState(world, pos, config.decorationState);
		} else if (random.nextFloat() < generationChance) {
			setBlockState(world, pos, config.hatState);

			if (random.nextFloat() < vineChance) {
				generateVines(pos, world, random);
			}
		}
	}

	private void placeWithOptionalVines(
		WorldAccess world,
		Random random,
		BlockPos pos,
		BlockState state,
		boolean vines
	) {
		if (world.getBlockState(pos.down()).isOf(state.getBlock())) {
			setBlockState(world, pos, state);
		} else if (random.nextFloat() < VINE_CHANCE_INNER) {
			setBlockState(world, pos, state);

			if (vines && random.nextInt(11) == 0) {
				generateVines(pos, world, random);
			}
		}
	}

	private static void generateVines(BlockPos pos, WorldAccess world, Random random) {
		BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.DOWN);

		if (world.isAir(mutable) == false) {
			return;
		}

		int vineLength = MathHelper.nextInt(random, 1, 5);

		if (random.nextInt(7) == 0) {
			vineLength *= 2;
		}

		WeepingVinesFeature.generateVineColumn(world, random, mutable, vineLength, 23, 25);
	}
}
