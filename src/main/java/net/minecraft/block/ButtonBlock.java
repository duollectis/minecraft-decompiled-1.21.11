package net.minecraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.block.OrientationHelper;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Блок кнопки. При нажатии игроком или стрелой (если разрешено типом блока)
 * подаёт редстоун-сигнал на {@link #pressTicks} тиков, после чего автоматически отпускается.
 */
public class ButtonBlock extends WallMountedBlock {

	public static final MapCodec<ButtonBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			BlockSetType.CODEC.fieldOf("block_set_type").forGetter(block -> block.blockSetType),
			Codec.intRange(1, 1024).fieldOf("ticks_to_stay_pressed").forGetter(block -> block.pressTicks),
			createSettingsCodec()
		).apply(instance, ButtonBlock::new)
	);
	public static final BooleanProperty POWERED = Properties.POWERED;
	private final BlockSetType blockSetType;
	private final int pressTicks;
	private final Function<BlockState, VoxelShape> shapeFunction;

	@Override
	public MapCodec<ButtonBlock> getCodec() {
		return CODEC;
	}

	public ButtonBlock(BlockSetType blockSetType, int pressTicks, AbstractBlock.Settings settings) {
		super(settings.sounds(blockSetType.soundType()));
		this.blockSetType = blockSetType;
		this.pressTicks = pressTicks;
		setDefaultState(
			stateManager
				.getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(POWERED, false)
				.with(FACE, BlockFace.WALL)
		);
		shapeFunction = createShapeFunction();
	}

	private Function<BlockState, VoxelShape> createShapeFunction() {
		VoxelShape pressedShape = Block.createCubeShape(14.0);
		VoxelShape unpressedShape = Block.createCubeShape(12.0);
		Map<BlockFace, Map<Direction, VoxelShape>> faceShapes =
			VoxelShapes.createBlockFaceHorizontalFacingShapeMap(Block.createCuboidZShape(6.0, 4.0, 8.0, 16.0));

		return createShapeFunction(
			state -> VoxelShapes.combineAndSimplify(
				faceShapes.get(state.get(FACE)).get(state.get(FACING)),
				state.get(POWERED) ? pressedShape : unpressedShape,
				BooleanBiFunction.ONLY_FIRST
			)
		);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapeFunction.apply(state);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (state.get(POWERED)) {
			return ActionResult.CONSUME;
		}

		powerOn(state, world, pos, player);
		return ActionResult.SUCCESS;
	}

	@Override
	protected void onExploded(
		BlockState state,
		ServerWorld world,
		BlockPos pos,
		Explosion explosion,
		BiConsumer<ItemStack, BlockPos> stackMerger
	) {
		if (explosion.canTriggerBlocks() && state.get(POWERED) == false) {
			powerOn(state, world, pos, null);
		}

		super.onExploded(state, world, pos, explosion, stackMerger);
	}

	/**
	 * Активирует кнопку: устанавливает {@link #POWERED} в {@code true},
	 * уведомляет соседей и планирует автоматическое отпускание через {@link #pressTicks} тиков.
	 */
	public void powerOn(BlockState state, World world, BlockPos pos, @Nullable PlayerEntity player) {
		world.setBlockState(pos, state.with(POWERED, true), 3);
		updateNeighbors(state, world, pos);
		world.scheduleBlockTick(pos, this, pressTicks);
		playClickSound(player, world, pos, true);
		world.emitGameEvent(player, GameEvent.BLOCK_ACTIVATE, pos);
	}

	protected void playClickSound(@Nullable PlayerEntity player, WorldAccess world, BlockPos pos, boolean powered) {
		world.playSound(powered ? player : null, pos, getClickSound(powered), SoundCategory.BLOCKS);
	}

	protected SoundEvent getClickSound(boolean powered) {
		return powered ? blockSetType.buttonClickOn() : blockSetType.buttonClickOff();
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		if (moved == false && state.get(POWERED)) {
			updateNeighbors(state, world, pos);
		}
	}

	@Override
	protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return state.get(POWERED) ? 15 : 0;
	}

	@Override
	protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return state.get(POWERED) && getDirection(state) == direction ? 15 : 0;
	}


	@Override
	protected boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(POWERED)) {
			tryPowerWithProjectiles(state, world, pos);
		}
	}

	@Override
	protected void onEntityCollision(
		BlockState state,
		World world,
		BlockPos pos,
		Entity entity,
		EntityCollisionHandler handler,
		boolean isAboveSurface
	) {
		if (world.isClient() == false
			&& blockSetType.canButtonBeActivatedByArrows()
			&& state.get(POWERED) == false
		) {
			tryPowerWithProjectiles(state, world, pos);
		}
	}

	/**
	 * Проверяет наличие стрелы в зоне кнопки и обновляет состояние {@link #POWERED}.
	 * Если стрела найдена и кнопка не была нажата — активирует её и планирует отпускание.
	 */
	protected void tryPowerWithProjectiles(BlockState state, World world, BlockPos pos) {
		PersistentProjectileEntity arrow = blockSetType.canButtonBeActivatedByArrows()
			? world
				.getNonSpectatingEntities(
					PersistentProjectileEntity.class,
					state.getOutlineShape(world, pos).getBoundingBox().offset(pos)
				)
				.stream()
				.findFirst()
				.orElse(null)
			: null;

		boolean hasArrow = arrow != null;
		boolean wasPowered = state.get(POWERED);

		if (hasArrow != wasPowered) {
			world.setBlockState(pos, state.with(POWERED, hasArrow), 3);
			updateNeighbors(state, world, pos);
			playClickSound(null, world, pos, hasArrow);
			world.emitGameEvent(
				arrow,
				hasArrow ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE,
				pos
			);
		}

		if (hasArrow) {
			world.scheduleBlockTick(new BlockPos(pos), this, pressTicks);
		}
	}

	private void updateNeighbors(BlockState state, World world, BlockPos pos) {
		Direction direction = getDirection(state).getOpposite();
		WireOrientation wireOrientation = OrientationHelper.getEmissionOrientation(
			world, direction, direction.getAxis().isHorizontal() ? Direction.UP : state.get(FACING)
		);
		world.updateNeighborsAlways(pos, this, wireOrientation);
		world.updateNeighborsAlways(pos.offset(direction), this, wireOrientation);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, FACE);
	}
}
