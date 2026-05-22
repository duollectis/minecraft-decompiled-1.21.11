package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Блок бочки — контейнер с инвентарём, ориентированный по любой из 6 сторон.
 * Поддерживает компаратор (выдаёт сигнал в зависимости от заполненности),
 * уведомляет пиглинов при взаимодействии и рассыпает содержимое при разрушении.
 */
public class BarrelBlock extends BlockWithEntity {

	public static final MapCodec<BarrelBlock> CODEC = createCodec(BarrelBlock::new);
	public static final EnumProperty<Direction> FACING = Properties.FACING;
	public static final BooleanProperty OPEN = Properties.OPEN;

	@Override
	public MapCodec<BarrelBlock> getCodec() {
		return CODEC;
	}

	public BarrelBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false));
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world instanceof ServerWorld serverWorld
			&& world.getBlockEntity(pos) instanceof BarrelBlockEntity barrelBlockEntity
		) {
			player.openHandledScreen(barrelBlockEntity);
			player.incrementStat(Stats.OPEN_BARREL);
			PiglinBrain.onGuardedBlockInteracted(serverWorld, player, true);
		}

		return ActionResult.SUCCESS;
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		ItemScatterer.onStateReplaced(state, world, pos);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getBlockEntity(pos) instanceof BarrelBlockEntity barrelBlockEntity) {
			barrelBlockEntity.tick();
		}
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BarrelBlockEntity(pos, state);
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
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
		builder.add(FACING, OPEN);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
	}
}
