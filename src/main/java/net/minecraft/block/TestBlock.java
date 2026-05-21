package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.TestBlockEntity;
import net.minecraft.block.enums.TestBlockMode;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import org.jspecify.annotations.Nullable;

/**
 * {@code TestBlock}.
 */
public class TestBlock extends BlockWithEntity implements OperatorBlock {

	public static final MapCodec<TestBlock> CODEC = createCodec(TestBlock::new);
	public static final EnumProperty<TestBlockMode> MODE = Properties.TEST_BLOCK_MODE;

	public TestBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new TestBlockEntity(pos, state);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockStateComponent blockStateComponent = ctx.getStack().get(DataComponentTypes.BLOCK_STATE);
		BlockState blockState = this.getDefaultState();
		if (blockStateComponent != null) {
			TestBlockMode testBlockMode = blockStateComponent.getValue(MODE);
			if (testBlockMode != null) {
				blockState = blockState.with(MODE, testBlockMode);
			}
		}

		return blockState;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(MODE);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.getBlockEntity(pos) instanceof TestBlockEntity testBlockEntity) {
			if (!player.isCreativeLevelTwoOp()) {
				return ActionResult.PASS;
			}
			else {
				if (world.isClient()) {
					player.openTestBlockScreen(testBlockEntity);
				}

				return ActionResult.SUCCESS;
			}
		}
		else {
			return ActionResult.PASS;
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		TestBlockEntity testBlockEntity = getBlockEntityOnServer(world, pos);
		if (testBlockEntity != null) {
			testBlockEntity.reset();
		}
	}

	@Override
	protected void neighborUpdate(
			BlockState state,
			World world,
			BlockPos pos,
			Block sourceBlock,
			@Nullable WireOrientation wireOrientation,
			boolean notify
	) {
		TestBlockEntity testBlockEntity = getBlockEntityOnServer(world, pos);
		if (testBlockEntity != null) {
			if (testBlockEntity.getMode() != TestBlockMode.START) {
				boolean bl = world.isReceivingRedstonePower(pos);
				boolean bl2 = testBlockEntity.isPowered();
				if (bl && !bl2) {
					testBlockEntity.setPowered(true);
					testBlockEntity.trigger();
				}
				else if (!bl && bl2) {
					testBlockEntity.setPowered(false);
				}
			}
		}
	}

	private static @Nullable TestBlockEntity getBlockEntityOnServer(World world, BlockPos pos) {
		return world instanceof ServerWorld serverWorld
				       && serverWorld.getBlockEntity(pos) instanceof TestBlockEntity testBlockEntity ? testBlockEntity
		                                                                                             : null;
	}

	@Override
	public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		if (state.get(MODE) != TestBlockMode.START) {
			return 0;
		}
		else if (world.getBlockEntity(pos) instanceof TestBlockEntity testBlockEntity) {
			return testBlockEntity.isPowered() ? 15 : 0;
		}
		else {
			return 0;
		}
	}

	@Override
	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack itemStack = super.getPickStack(world, pos, state, includeData);
		return applyBlockStateToStack(itemStack, state.get(MODE));
	}

	/**
	 * Применяет block state to stack.
	 *
	 * @param stack stack
	 * @param mode mode
	 *
	 * @return ItemStack — результат операции
	 */
	public static ItemStack applyBlockStateToStack(ItemStack stack, TestBlockMode mode) {
		stack.set(
				DataComponentTypes.BLOCK_STATE,
				stack.getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT).with(MODE, mode)
		);
		return stack;
	}

	@Override
	protected MapCodec<TestBlock> getCodec() {
		return CODEC;
	}
}
