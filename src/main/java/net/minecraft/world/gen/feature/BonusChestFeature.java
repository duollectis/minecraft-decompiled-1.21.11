package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.loot.LootTables;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.stream.IntStream;

/** Размещает бонусный сундук с факелами вокруг него на поверхности стартового чанка. */
public class BonusChestFeature extends Feature<DefaultFeatureConfig> {

	public BonusChestFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		Random random = context.getRandom();
		StructureWorldAccess world = context.getWorld();
		ChunkPos chunkPos = new ChunkPos(context.getOrigin());
		IntArrayList xCoords = Util.shuffle(IntStream.rangeClosed(chunkPos.getStartX(), chunkPos.getEndX()), random);
		IntArrayList zCoords = Util.shuffle(IntStream.rangeClosed(chunkPos.getStartZ(), chunkPos.getEndZ()), random);
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int x : xCoords) {
			for (int z : zCoords) {
				mutable.set(x, 0, z);
				BlockPos surfacePos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, mutable);

				if (!world.isAir(surfacePos)
						&& !world.getBlockState(surfacePos).getCollisionShape(world, surfacePos).isEmpty()) {
					continue;
				}

				world.setBlockState(surfacePos, Blocks.CHEST.getDefaultState(), 2);
				LootableInventory.setLootTable(world, random, surfacePos, LootTables.SPAWN_BONUS_CHEST);

				BlockState torchState = Blocks.TORCH.getDefaultState();

				for (Direction direction : Direction.Type.HORIZONTAL) {
					BlockPos torchPos = surfacePos.offset(direction);

					if (torchState.canPlaceAt(world, torchPos)) {
						world.setBlockState(torchPos, torchState, 2);
					}
				}

				return true;
			}
		}

		return false;
	}
}
