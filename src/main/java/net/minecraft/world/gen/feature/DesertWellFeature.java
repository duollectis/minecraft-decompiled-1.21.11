package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.loot.LootTables;
import net.minecraft.predicate.block.BlockStatePredicate;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.List;

/** Генерирует пустынный колодец из песчаника с водой внутри и подозрительным песком на дне. */
public class DesertWellFeature extends Feature<DefaultFeatureConfig> {

	private static final BlockStatePredicate CAN_GENERATE = BlockStatePredicate.forBlock(Blocks.SAND);
	private final BlockState sand = Blocks.SAND.getDefaultState();
	private final BlockState slab = Blocks.SANDSTONE_SLAB.getDefaultState();
	private final BlockState wall = Blocks.SANDSTONE.getDefaultState();
	private final BlockState fluidInside = Blocks.WATER.getDefaultState();

	public DesertWellFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos pos = context.getOrigin().up();

		while (world.isAir(pos) && pos.getY() > world.getBottomY() + 2) {
			pos = pos.down();
		}

		if (!CAN_GENERATE.test(world.getBlockState(pos))) {
			return false;
		}

		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (world.isAir(pos.add(dx, -1, dz)) && world.isAir(pos.add(dx, -2, dz))) {
					return false;
				}
			}
		}

		for (int dy = -2; dy <= 0; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					world.setBlockState(pos.add(dx, dy, dz), wall, 2);
				}
			}
		}

		world.setBlockState(pos, fluidInside, 2);

		for (Direction direction : Direction.Type.HORIZONTAL) {
			world.setBlockState(pos.offset(direction), fluidInside, 2);
		}

		BlockPos bottom = pos.down();
		world.setBlockState(bottom, sand, 2);

		for (Direction direction : Direction.Type.HORIZONTAL) {
			world.setBlockState(bottom.offset(direction), sand, 2);
		}

		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (dx == -2 || dx == 2 || dz == -2 || dz == 2) {
					world.setBlockState(pos.add(dx, 1, dz), wall, 2);
				}
			}
		}

		world.setBlockState(pos.add(2, 1, 0), slab, 2);
		world.setBlockState(pos.add(-2, 1, 0), slab, 2);
		world.setBlockState(pos.add(0, 1, 2), slab, 2);
		world.setBlockState(pos.add(0, 1, -2), slab, 2);

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockState roofBlock = (dx == 0 && dz == 0) ? wall : slab;
				world.setBlockState(pos.add(dx, 4, dz), roofBlock, 2);
			}
		}

		for (int dy = 1; dy <= 3; dy++) {
			world.setBlockState(pos.add(-1, dy, -1), wall, 2);
			world.setBlockState(pos.add(-1, dy, 1), wall, 2);
			world.setBlockState(pos.add(1, dy, -1), wall, 2);
			world.setBlockState(pos.add(1, dy, 1), wall, 2);
		}

		List<BlockPos> sandCandidates = List.of(pos, pos.east(), pos.south(), pos.west(), pos.north());
		Random random = context.getRandom();
		generateSuspiciousSand(world, Util.getRandom(sandCandidates, random).down(1));
		generateSuspiciousSand(world, Util.getRandom(sandCandidates, random).down(2));

		return true;
	}

	private static void generateSuspiciousSand(StructureWorldAccess world, BlockPos pos) {
		world.setBlockState(pos, Blocks.SUSPICIOUS_SAND.getDefaultState(), 3);
		world.getBlockEntity(pos, BlockEntityType.BRUSHABLE_BLOCK)
		     .ifPresent(blockEntity -> blockEntity.setLootTable(LootTables.DESERT_WELL_ARCHAEOLOGY, pos.asLong()));
	}
}
