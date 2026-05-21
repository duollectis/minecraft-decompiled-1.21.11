package net.minecraft.world.gen.feature;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * {@code EndPortalFeature}.
 */
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
		BlockPos blockPos = context.getOrigin();
		StructureWorldAccess structureWorldAccess = context.getWorld();

		for (BlockPos blockPos2 : BlockPos.iterate(
				new BlockPos(blockPos.getX() - 4, blockPos.getY() - 1, blockPos.getZ() - 4),
				new BlockPos(blockPos.getX() + 4, blockPos.getY() + 32, blockPos.getZ() + 4)
		)) {
			boolean bl = blockPos2.isWithinDistance(blockPos, 2.5);
			if (bl || blockPos2.isWithinDistance(blockPos, 3.5)) {
				if (blockPos2.getY() < blockPos.getY()) {
					if (bl) {
						this.setBlockState(structureWorldAccess, blockPos2, Blocks.BEDROCK.getDefaultState());
					}
					else if (blockPos2.getY() < blockPos.getY()) {
						if (this.open) {
							this.place(structureWorldAccess, blockPos2, Blocks.END_STONE);
						}
						else {
							this.setBlockState(structureWorldAccess, blockPos2, Blocks.END_STONE.getDefaultState());
						}
					}
				}
				else if (blockPos2.getY() > blockPos.getY()) {
					if (this.open) {
						this.place(structureWorldAccess, blockPos2, Blocks.AIR);
					}
					else {
						this.setBlockState(structureWorldAccess, blockPos2, Blocks.AIR.getDefaultState());
					}
				}
				else if (!bl) {
					this.setBlockState(structureWorldAccess, blockPos2, Blocks.BEDROCK.getDefaultState());
				}
				else if (this.open) {
					this.place(structureWorldAccess, new BlockPos(blockPos2), Blocks.END_PORTAL);
				}
				else {
					this.setBlockState(structureWorldAccess, new BlockPos(blockPos2), Blocks.AIR.getDefaultState());
				}
			}
		}

		for (int i = 0; i < 4; i++) {
			this.setBlockState(structureWorldAccess, blockPos.up(i), Blocks.BEDROCK.getDefaultState());
		}

		BlockPos blockPos3 = blockPos.up(2);

		for (Direction direction : Direction.Type.HORIZONTAL) {
			this.setBlockState(
					structureWorldAccess,
					blockPos3.offset(direction),
					Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, direction)
			);
		}

		return true;
	}

	private void place(StructureWorldAccess world, BlockPos pos, Block block) {
		if (!world.getBlockState(pos).isOf(block)) {
			world.breakBlock(pos, true, null);
			this.setBlockState(world, pos, block.getDefaultState());
		}
	}
}
