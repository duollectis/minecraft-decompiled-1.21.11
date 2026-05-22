package net.minecraft.world.gen.feature;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.world.ModifiableWorld;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.foliage.FoliagePlacer;
import net.minecraft.world.gen.treedecorator.TreeDecorator;

import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Генерирует дерево: ствол, корни, листву и декораторы.
 * Использует {@link TreeFeatureConfig} для конфигурации всех компонентов.
 */
public class TreeFeature extends Feature<TreeFeatureConfig> {

	@Block.SetBlockStateFlag
	private static final int FORCE_STATE_AND_NOTIFY_ALL = 19;

	/** Максимальное расстояние от бревна для листьев (BFS-уровни). */
	private static final int MAX_LEAF_DISTANCE = 7;

	public TreeFeature(Codec<TreeFeatureConfig> codec) {
		super(codec);
	}

	public static boolean isVine(TestableWorld world, BlockPos pos) {
		return world.testBlockState(pos, state -> state.isOf(Blocks.VINE));
	}

	public static boolean isAirOrLeaves(TestableWorld world, BlockPos pos) {
		return world.testBlockState(pos, state -> state.isAir() || state.isIn(BlockTags.LEAVES));
	}

	public static boolean canReplace(TestableWorld world, BlockPos pos) {
		return world.testBlockState(pos, state -> state.isAir() || state.isIn(BlockTags.REPLACEABLE_BY_TREES));
	}

	private static void setBlockStateWithoutUpdatingNeighbors(ModifiableWorld world, BlockPos pos, BlockState state) {
		world.setBlockState(pos, state, FORCE_STATE_AND_NOTIFY_ALL);
	}

	/**
	 * Внутренняя генерация дерева: вычисляет высоту, проверяет границы мира,
	 * размещает корни, ствол и листву через соответствующие плейсеры.
	 */
	private boolean generate(
		StructureWorldAccess world,
		Random random,
		BlockPos pos,
		BiConsumer<BlockPos, BlockState> rootPlacer,
		BiConsumer<BlockPos, BlockState> trunkPlacer,
		FoliagePlacer.BlockPlacer foliagePlacer,
		TreeFeatureConfig config
	) {
		int trunkHeight = config.trunkPlacer.getHeight(random);
		int foliageHeight = config.foliagePlacer.getRandomHeight(random, trunkHeight, config);
		int foliageOffset = trunkHeight - foliageHeight;
		int foliageRadius = config.foliagePlacer.getRandomRadius(random, foliageOffset);
		BlockPos trunkBase = config.rootPlacer
			.<BlockPos>map(rp -> rp.trunkOffset(pos, random))
			.orElse(pos);

		int minY = Math.min(pos.getY(), trunkBase.getY());
		int maxY = Math.max(pos.getY(), trunkBase.getY()) + trunkHeight + 1;

		if (minY < world.getBottomY() + 1 || maxY > world.getTopYInclusive() + 1) {
			return false;
		}

		OptionalInt minClippedHeight = config.minimumSize.getMinClippedHeight();
		int actualHeight = getTopPosition(world, trunkHeight, trunkBase, config);

		if (actualHeight < trunkHeight && (minClippedHeight.isEmpty() || actualHeight < minClippedHeight.getAsInt())) {
			return false;
		}

		if (config.rootPlacer.isPresent()
			&& !config.rootPlacer.get().generate(world, rootPlacer, random, pos, trunkBase, config)
		) {
			return false;
		}

		List<FoliagePlacer.TreeNode> treeNodes = config.trunkPlacer.generate(world, trunkPlacer, random, actualHeight, trunkBase, config);
		treeNodes.forEach(node -> config.foliagePlacer.generate(world, foliagePlacer, random, config, actualHeight, node, foliageHeight, foliageRadius));
		return true;
	}

	/**
	 * Определяет реальную высоту ствола с учётом препятствий.
	 * Возвращает высоту, на которой встречается первый непроходимый блок, минус 2.
	 */
	private int getTopPosition(TestableWorld world, int height, BlockPos pos, TreeFeatureConfig config) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int dy = 0; dy <= height + 1; dy++) {
			int radius = config.minimumSize.getRadius(height, dy);

			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					mutable.set(pos, dx, dy, dz);

					if (!config.trunkPlacer.canReplaceOrIsLog(world, mutable)
						|| (!config.ignoreVines && isVine(world, mutable))
					) {
						return dy - 2;
					}
				}
			}
		}

		return height;
	}

	@Override
	protected void setBlockState(ModifiableWorld world, BlockPos pos, BlockState state) {
		setBlockStateWithoutUpdatingNeighbors(world, pos, state);
	}

	@Override
	public final boolean generate(FeatureContext<TreeFeatureConfig> context) {
		final StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		BlockPos origin = context.getOrigin();
		TreeFeatureConfig config = context.getConfig();

		Set<BlockPos> rootPositions = Sets.newHashSet();
		Set<BlockPos> trunkPositions = Sets.newHashSet();
		final Set<BlockPos> foliagePositions = Sets.newHashSet();
		Set<BlockPos> decoratorPositions = Sets.newHashSet();

		BiConsumer<BlockPos, BlockState> rootPlacer = (pos, state) -> {
			rootPositions.add(pos.toImmutable());
			world.setBlockState(pos, state, FORCE_STATE_AND_NOTIFY_ALL);
		};
		BiConsumer<BlockPos, BlockState> trunkPlacer = (pos, state) -> {
			trunkPositions.add(pos.toImmutable());
			world.setBlockState(pos, state, FORCE_STATE_AND_NOTIFY_ALL);
		};
		FoliagePlacer.BlockPlacer foliagePlacer = new FoliagePlacer.BlockPlacer() {
			@Override
			public void placeBlock(BlockPos pos, BlockState state) {
				foliagePositions.add(pos.toImmutable());
				world.setBlockState(pos, state, FORCE_STATE_AND_NOTIFY_ALL);
			}

			@Override
			public boolean hasPlacedBlock(BlockPos pos) {
				return foliagePositions.contains(pos);
			}
		};
		BiConsumer<BlockPos, BlockState> decoratorPlacer = (pos, state) -> {
			decoratorPositions.add(pos.toImmutable());
			world.setBlockState(pos, state, FORCE_STATE_AND_NOTIFY_ALL);
		};

		boolean generated = generate(world, random, origin, rootPlacer, trunkPlacer, foliagePlacer, config);

		if (!generated || (trunkPositions.isEmpty() && foliagePositions.isEmpty())) {
			return false;
		}

		if (!config.decorators.isEmpty()) {
			TreeDecorator.Generator decoratorGenerator = new TreeDecorator.Generator(
				world,
				decoratorPlacer,
				random,
				trunkPositions,
				foliagePositions,
				rootPositions
			);
			config.decorators.forEach(decorator -> decorator.generate(decoratorGenerator));
		}

		return BlockBox.encompassPositions(Iterables.concat(rootPositions, trunkPositions, foliagePositions, decoratorPositions))
			.map(box -> {
				VoxelSet voxelSet = placeLogsAndLeaves(world, box, trunkPositions, decoratorPositions, rootPositions);
				StructureTemplate.updateCorner(world, 3, voxelSet, box.getMinX(), box.getMinY(), box.getMinZ());
				return true;
			})
			.orElse(false);
	}

	/**
	 * Обновляет расстояние листьев от ближайшего бревна (BFS по уровням 0–6).
	 * Возвращает VoxelSet всех затронутых позиций для последующего обновления соседей.
	 */
	private static VoxelSet placeLogsAndLeaves(
		WorldAccess world,
		BlockBox box,
		Set<BlockPos> trunkPositions,
		Set<BlockPos> decoratorPositions,
		Set<BlockPos> rootPositions
	) {
		VoxelSet voxelSet = new BitSetVoxelSet(box.getBlockCountX(), box.getBlockCountY(), box.getBlockCountZ());
		List<Set<BlockPos>> distanceBuckets = Lists.newArrayList();

		for (int level = 0; level < MAX_LEAF_DISTANCE; level++) {
			distanceBuckets.add(Sets.newHashSet());
		}

		for (BlockPos pos : Lists.newArrayList(Sets.union(decoratorPositions, rootPositions))) {
			if (box.contains(pos)) {
				voxelSet.set(pos.getX() - box.getMinX(), pos.getY() - box.getMinY(), pos.getZ() - box.getMinZ());
			}
		}

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int currentLevel = 0;
		distanceBuckets.get(0).addAll(trunkPositions);

		while (true) {
			while (currentLevel >= MAX_LEAF_DISTANCE || !distanceBuckets.get(currentLevel).isEmpty()) {
				if (currentLevel >= MAX_LEAF_DISTANCE) {
					return voxelSet;
				}

				Iterator<BlockPos> iterator = distanceBuckets.get(currentLevel).iterator();
				BlockPos current = iterator.next();
				iterator.remove();

				if (!box.contains(current)) {
					continue;
				}

				if (currentLevel != 0) {
					BlockState state = world.getBlockState(current);
					setBlockStateWithoutUpdatingNeighbors(world, current, state.with(Properties.DISTANCE_1_7, currentLevel));
				}

				voxelSet.set(current.getX() - box.getMinX(), current.getY() - box.getMinY(), current.getZ() - box.getMinZ());

				for (Direction direction : Direction.values()) {
					mutable.set(current, direction);

					if (!box.contains(mutable)) {
						continue;
					}

					int relX = mutable.getX() - box.getMinX();
					int relY = mutable.getY() - box.getMinY();
					int relZ = mutable.getZ() - box.getMinZ();

					if (voxelSet.contains(relX, relY, relZ)) {
						continue;
					}

					BlockState neighborState = world.getBlockState(mutable);
					OptionalInt leafDistance = LeavesBlock.getOptionalDistanceFromLog(neighborState);

					if (leafDistance.isEmpty()) {
						continue;
					}

					int nextLevel = Math.min(leafDistance.getAsInt(), currentLevel + 1);

					if (nextLevel < MAX_LEAF_DISTANCE) {
						distanceBuckets.get(nextLevel).add(mutable.toImmutable());
						currentLevel = Math.min(currentLevel, nextLevel);
					}
				}
			}

			currentLevel++;
		}
	}

	public static List<BlockPos> getLeafLitterPositions(TreeDecorator.Generator decoratorGenerator) {
		List<BlockPos> result = Lists.newArrayList();
		List<BlockPos> rootPositions = decoratorGenerator.getRootPositions();
		List<BlockPos> logPositions = decoratorGenerator.getLogPositions();

		if (rootPositions.isEmpty()) {
			result.addAll(logPositions);
		} else if (!logPositions.isEmpty() && rootPositions.get(0).getY() == logPositions.get(0).getY()) {
			result.addAll(logPositions);
			result.addAll(rootPositions);
		} else {
			result.addAll(rootPositions);
		}

		return result;
	}
}
