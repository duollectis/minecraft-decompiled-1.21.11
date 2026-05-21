package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.TestInstanceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * {@code TestInstanceBlock}.
 */
public class TestInstanceBlock extends BlockWithEntity implements OperatorBlock {

	public static final MapCodec<TestInstanceBlock> CODEC = createCodec(TestInstanceBlock::new);

	public TestInstanceBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new TestInstanceBlockEntity(pos, state);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.getBlockEntity(pos) instanceof TestInstanceBlockEntity testInstanceBlockEntity) {
			if (!player.isCreativeLevelTwoOp()) {
				return ActionResult.PASS;
			}
			else {
				if (player.getEntityWorld().isClient()) {
					player.openTestInstanceBlockScreen(testInstanceBlockEntity);
				}

				return ActionResult.SUCCESS;
			}
		}
		else {
			return ActionResult.PASS;
		}
	}

	@Override
	protected MapCodec<TestInstanceBlock> getCodec() {
		return CODEC;
	}
}
