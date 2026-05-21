package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * {@code AnvilBlock}.
 */
public class AnvilBlock extends FallingBlock {

	public static final MapCodec<AnvilBlock> CODEC = createCodec(AnvilBlock::new);
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	private static final Map<Direction.Axis, VoxelShape> SHAPES_BY_AXIS = VoxelShapes.createHorizontalAxisShapeMap(
			VoxelShapes.union(
					Block.createColumnShape(12.0, 0.0, 4.0),
					Block.createColumnShape(8.0, 10.0, 4.0, 5.0),
					Block.createColumnShape(4.0, 8.0, 5.0, 10.0),
					Block.createColumnShape(10.0, 16.0, 10.0, 16.0)
			)
	);
	private static final Text TITLE = Text.translatable("container.repair");
	private static final float FALLING_BLOCK_ENTITY_DAMAGE_MULTIPLIER = 2.0F;
	private static final int FALLING_BLOCK_ENTITY_MAX_DAMAGE = 40;

	@Override
	public MapCodec<AnvilBlock> getCodec() {
		return CODEC;
	}

	public AnvilBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().rotateYClockwise());
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!world.isClient()) {
			player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
			player.incrementStat(Stats.INTERACT_WITH_ANVIL);
		}

		return ActionResult.SUCCESS;
	}

	@Override
	protected @Nullable NamedScreenHandlerFactory createScreenHandlerFactory(
			BlockState state,
			World world,
			BlockPos pos
	) {
		return new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, player) -> new AnvilScreenHandler(
						syncId,
						inventory,
						ScreenHandlerContext.create(world, pos)
				), TITLE
		);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_AXIS.get(state.get(FACING).getAxis());
	}

	@Override
	protected void configureFallingBlockEntity(FallingBlockEntity entity) {
		entity.setHurtEntities(2.0F, 40);
	}

	@Override
	public void onLanding(
			World world,
			BlockPos pos,
			BlockState fallingBlockState,
			BlockState currentStateInPos,
			FallingBlockEntity fallingBlockEntity
	) {
		if (!fallingBlockEntity.isSilent()) {
			world.syncWorldEvent(1031, pos, 0);
		}
	}

	@Override
	public void onDestroyedOnLanding(World world, BlockPos pos, FallingBlockEntity fallingBlockEntity) {
		if (!fallingBlockEntity.isSilent()) {
			world.syncWorldEvent(1029, pos, 0);
		}
	}

	@Override
	public DamageSource getDamageSource(Entity attacker) {
		return attacker.getDamageSources().fallingAnvil(attacker);
	}

	public static @Nullable BlockState getLandingState(BlockState fallingState) {
		if (fallingState.isOf(Blocks.ANVIL)) {
			return Blocks.CHIPPED_ANVIL.getDefaultState().with(FACING, fallingState.get(FACING));
		}
		else {
			return fallingState.isOf(Blocks.CHIPPED_ANVIL) ? Blocks.DAMAGED_ANVIL
			                                                 .getDefaultState()
			                                                 .with(FACING, fallingState.get(FACING)) : null;
		}
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	@Override
	public int getColor(BlockState state, BlockView world, BlockPos pos) {
		return state.getMapColor(world, pos).color;
	}
}
