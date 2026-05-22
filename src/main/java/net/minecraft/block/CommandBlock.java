package net.minecraft.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.CommandBlockExecutor;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Блок команды — выполняет команды при получении сигнала редстоуна или в автоматическом режиме.
 * Поддерживает три типа: обычный (REDSTONE), цепочечный (SEQUENCE) и автоматический (AUTO).
 * Доступен только операторам (OperatorBlock).
 */
public class CommandBlock extends BlockWithEntity implements OperatorBlock {

	public static final MapCodec<CommandBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codec.BOOL.fieldOf("automatic").forGetter(block -> block.auto), createSettingsCodec())
					.apply(instance, CommandBlock::new)
	);
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final EnumProperty<Direction> FACING = FacingBlock.FACING;
	public static final BooleanProperty CONDITIONAL = Properties.CONDITIONAL;
	private final boolean auto;

	@Override
	public MapCodec<CommandBlock> getCodec() {
		return CODEC;
	}

	public CommandBlock(boolean auto, AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(
				stateManager
						.getDefaultState()
						.with(FACING, Direction.NORTH)
						.with(CONDITIONAL, false)
		);
		this.auto = auto;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		CommandBlockBlockEntity blockEntity = new CommandBlockBlockEntity(pos, state);
		blockEntity.setAuto(auto);
		return blockEntity;
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
		if (world.isClient()) {
			return;
		}

		if (world.getBlockEntity(pos) instanceof CommandBlockBlockEntity commandBlock) {
			update(world, pos, commandBlock, world.isReceivingRedstonePower(pos));
		}
	}

	/**
	 * Обновляет состояние питания блока команды и при необходимости планирует выполнение.
	 * Цепочечные и автоматические блоки не реагируют на фронт сигнала — они управляются иначе.
	 */
	private void update(World world, BlockPos pos, CommandBlockBlockEntity blockEntity, boolean powered) {
		boolean wasPowered = blockEntity.isPowered();

		if (powered == wasPowered) {
			return;
		}

		blockEntity.setPowered(powered);

		if (powered
				&& !blockEntity.isAuto()
				&& blockEntity.getCommandBlockType() != CommandBlockBlockEntity.Type.SEQUENCE
		) {
			blockEntity.updateConditionMet();
			world.scheduleBlockTick(pos, this, 1);
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!(world.getBlockEntity(pos) instanceof CommandBlockBlockEntity commandBlock)) {
			return;
		}

		CommandBlockExecutor executor = commandBlock.getCommandExecutor();
		boolean hasCommand = !StringHelper.isEmpty(executor.getCommand());
		CommandBlockBlockEntity.Type type = commandBlock.getCommandBlockType();
		boolean conditionMet = commandBlock.isConditionMet();

		if (type == CommandBlockBlockEntity.Type.AUTO) {
			commandBlock.updateConditionMet();

			if (conditionMet) {
				execute(state, world, pos, executor, hasCommand);
			} else if (commandBlock.isConditionalCommandBlock()) {
				executor.setSuccessCount(0);
			}

			if (commandBlock.isPowered() || commandBlock.isAuto()) {
				world.scheduleBlockTick(pos, this, 1);
			}
		} else if (type == CommandBlockBlockEntity.Type.REDSTONE) {
			if (conditionMet) {
				execute(state, world, pos, executor, hasCommand);
			} else if (commandBlock.isConditionalCommandBlock()) {
				executor.setSuccessCount(0);
			}
		}

		world.updateComparators(pos, this);
	}

	private void execute(
			BlockState state,
			ServerWorld world,
			BlockPos pos,
			CommandBlockExecutor executor,
			boolean hasCommand
	) {
		if (hasCommand) {
			executor.execute(world);
		} else {
			executor.setSuccessCount(0);
		}

		executeCommandChain(world, pos, state.get(FACING));
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof CommandBlockBlockEntity commandBlock)
				|| !player.isCreativeLevelTwoOp()
		) {
			return ActionResult.PASS;
		}

		player.openCommandBlockScreen(commandBlock);

		return ActionResult.SUCCESS;
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		return world.getBlockEntity(pos) instanceof CommandBlockBlockEntity commandBlock
				? commandBlock.getCommandExecutor().getSuccessCount()
				: 0;
	}

	@Override
	public void onPlaced(
			World world,
			BlockPos pos,
			BlockState state,
			@Nullable LivingEntity placer,
			ItemStack itemStack
	) {
		if (!(world.getBlockEntity(pos) instanceof CommandBlockBlockEntity commandBlock)) {
			return;
		}

		CommandBlockExecutor executor = commandBlock.getCommandExecutor();

		if (world instanceof ServerWorld serverWorld) {
			if (!itemStack.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) {
				executor.setTrackOutput(
						serverWorld.getGameRules().getValue(GameRules.SEND_COMMAND_FEEDBACK)
				);
				commandBlock.setAuto(auto);
			}

			update(world, pos, commandBlock, world.isReceivingRedstonePower(pos));
		}
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, CONDITIONAL);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
	}

	private static void executeCommandChain(ServerWorld world, BlockPos pos, Direction facing) {
		BlockPos.Mutable mutable = pos.mutableCopy();
		GameRules gameRules = world.getGameRules();
		int i = gameRules.getValue(GameRules.MAX_COMMAND_SEQUENCE_LENGTH);

		while (i-- > 0) {
			mutable.move(facing);
			BlockState blockState = world.getBlockState(mutable);
			Block block = blockState.getBlock();
			if (!blockState.isOf(Blocks.CHAIN_COMMAND_BLOCK)
					|| !(world.getBlockEntity(mutable) instanceof CommandBlockBlockEntity commandBlockBlockEntity)
					|| commandBlockBlockEntity.getCommandBlockType() != CommandBlockBlockEntity.Type.SEQUENCE) {
				break;
			}

			if (commandBlockBlockEntity.isPowered() || commandBlockBlockEntity.isAuto()) {
				CommandBlockExecutor commandBlockExecutor = commandBlockBlockEntity.getCommandExecutor();
				if (commandBlockBlockEntity.updateConditionMet()) {
					if (!commandBlockExecutor.execute(world)) {
						break;
					}

					world.updateComparators(mutable, block);
				}
				else if (commandBlockBlockEntity.isConditionalCommandBlock()) {
					commandBlockExecutor.setSuccessCount(0);
				}
			}

			facing = blockState.get(FACING);
		}

		if (i <= 0) {
			int j = Math.max(gameRules.getValue(GameRules.MAX_COMMAND_SEQUENCE_LENGTH), 0);
			LOGGER.warn("Command Block chain tried to execute more than {} steps!", j);
		}
	}
}
