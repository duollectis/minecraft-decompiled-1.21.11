package net.minecraft.world.gen.feature;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует портал Края: круглое основание из бедрока с порталом или камнем Края внутри, и факелами на уровне 2. */
public class EndPortalFeature extends Feature<DefaultFeatureConfig> {

	public static final int PORTAL_RADIUS = 4;
	public static final int PORTAL_HEIGHT = 4;
	public static final int TORCH_HEIGHT_OFFSET = 1;
	public static final float INNER_RADIUS = 0.5F;
	private static final BlockPos ORIGIN = BlockPos.ORIGIN;
	private final boolean open;

	public static BlockPos offsetOrigin(BlockPos pos) {
		return ORIGIN.add(pos);
	}

	public EndPortalFeature(boolean open) {
		super(DefaultFeatureConfig.CODEC);
		this.open = open;
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();

		for (BlockPos pos : BlockPos.iterate(
				new BlockPos(origin.getX() - PORTAL_RADIUS, origin.getY() - 1, origin.getZ() - PORTAL_RADIUS),
				new BlockPos(origin.getX() + PORTAL_RADIUS, origin.getY() + 32, origin.getZ() + PORTAL_RADIUS)
		)) {
			boolean isInner = pos.isWithinDistance(origin, 2.5);

			if (!isInner && !pos.isWithinDistance(origin, 3.5)) {
				continue;
			}

			if (pos.getY() < origin.getY()) {
				if (isInner) {
					setBlockState(world, pos, Blocks.BEDROCK.getDefaultState());
				} else {
					placeOrBreak(world, pos, Blocks.END_STONE);
				}
			} else if (pos.getY() > origin.getY()) {
				placeOrBreak(world, pos, Blocks.AIR);
			} else if (!isInner) {
				setBlockState(world, pos, Blocks.BEDROCK.getDefaultState());
			} else {
				placeOrBreak(world, new BlockPos(pos), Blocks.END_PORTAL);
			}
		}

		for (int dy = 0; dy < PORTAL_HEIGHT; dy++) {
			setBlockState(world, origin.up(dy), Blocks.BEDROCK.getDefaultState());
		}

		BlockPos torchBase = origin.up(TORCH_HEIGHT_OFFSET + 1);

		for (Direction direction : Direction.Type.HORIZONTAL) {
			setBlockState(
					world,
					torchBase.offset(direction),
					Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, direction)
			);
		}

		return true;
	}

	private void placeOrBreak(StructureWorldAccess world, BlockPos pos, Block block) {
		if (open) {
			if (!world.getBlockState(pos).isOf(block)) {
				world.breakBlock(pos, true, null);
				setBlockState(world, pos, block.getDefaultState());
			}
		} else {
			setBlockState(world, pos, block.getDefaultState());
		}
	}
}
