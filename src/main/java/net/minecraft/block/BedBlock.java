package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.Dismounting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.CollisionView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.tick.ScheduledTickView;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Блок кровати — двухблочный объект для сна и установки точки возрождения.
 * <p>
 * Состоит из двух частей: {@link BedPart#FOOT} (ножная) и {@link BedPart#HEAD} (головная).
 * При размещении автоматически создаёт вторую часть в направлении взгляда игрока.
 * В измерениях без правила сна ({@link BedRule#explodes()}) взрыв заменяет сон.
 * Поддерживает отскок сущностей при падении (коэффициент {@link #BOUNCE_MULTIPLIER}).
 */
public class BedBlock extends HorizontalFacingBlock implements BlockEntityProvider {

	public static final MapCodec<BedBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(DyeColor.CODEC.fieldOf("color").forGetter(BedBlock::getColor), createSettingsCodec())
			.apply(instance, BedBlock::new)
	);
	public static final EnumProperty<BedPart> PART = Properties.BED_PART;
	public static final BooleanProperty OCCUPIED = Properties.OCCUPIED;
	/** Множитель отскока по оси Y при падении на кровать. */
	private static final float BOUNCE_MULTIPLIER = 0.66f;
	/** Множитель отскока для живых существ (выше, чем для предметов). */
	private static final double LIVING_BOUNCE_FACTOR = 1.0;
	/** Множитель отскока для неживых сущностей. */
	private static final double NONLIVING_BOUNCE_FACTOR = 0.8;
	/** Флаги обновления блока при разрушении второй части кровати. */
	private static final int BREAK_UPDATE_FLAGS = 35;
	/** Сила взрыва кровати в измерениях без правила сна. */
	private static final float BED_EXPLOSION_POWER = 5.0f;
	private static final Map<Direction, VoxelShape> SHAPES_BY_DIRECTION = Util.make(() -> {
		VoxelShape legShape = Block.createCuboidShape(0.0, 0.0, 0.0, 3.0, 3.0, 3.0);
		VoxelShape legShapeRotated = VoxelShapes.transform(legShape, DirectionTransformation.ROT_90_Z_POS);
		return VoxelShapes.createHorizontalFacingShapeMap(VoxelShapes.union(
			Block.createColumnShape(16.0, 3.0, 9.0),
			legShape,
			legShapeRotated
		));
	});
	private final DyeColor color;

	@Override
	public MapCodec<BedBlock> getCodec() {
		return CODEC;
	}

	public BedBlock(DyeColor color, AbstractBlock.Settings settings) {
		super(settings);
		this.color = color;
		setDefaultState(stateManager.getDefaultState().with(PART, BedPart.FOOT).with(OCCUPIED, false));
	}

	public static @Nullable Direction getDirection(BlockView world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		return blockState.getBlock() instanceof BedBlock ? blockState.get(FACING) : null;
	}

	/**
	 * Обрабатывает взаимодействие с кроватью: сон, пробуждение жителя или взрыв
	 * в измерениях, где сон запрещён правилом {@link BedRule}.
	 */
	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS_SERVER;
		}

		if (state.get(PART) != BedPart.HEAD) {
			pos = pos.offset(state.get(FACING));
			state = world.getBlockState(pos);

			if (state.isOf(this) == false) {
				return ActionResult.CONSUME;
			}
		}

		BedRule bedRule = world.getEnvironmentAttributes()
			.getAttributeValue(EnvironmentAttributes.BED_RULE_GAMEPLAY, pos);

		if (bedRule.explodes()) {
			bedRule.errorMessage().ifPresent(message -> player.sendMessage(message, true));
			world.removeBlock(pos, false);

			BlockPos footPos = pos.offset(state.get(FACING).getOpposite());

			if (world.getBlockState(footPos).isOf(this)) {
				world.removeBlock(footPos, false);
			}

			Vec3d center = pos.toCenterPos();
			world.createExplosion(
				null,
				world.getDamageSources().badRespawnPoint(center),
				null,
				center,
				BED_EXPLOSION_POWER,
				true,
				World.ExplosionSourceType.BLOCK
			);

			return ActionResult.SUCCESS_SERVER;
		}

		if (state.get(OCCUPIED)) {
			if (wakeVillager(world, pos) == false) {
				player.sendMessage(Text.translatable("block.minecraft.bed.occupied"), true);
			}

			return ActionResult.SUCCESS_SERVER;
		}

		player.trySleep(pos).ifLeft(reason -> {
			if (reason.message() != null) {
				player.sendMessage(reason.message(), true);
			}
		});

		return ActionResult.SUCCESS_SERVER;
	}

	private boolean wakeVillager(World world, BlockPos pos) {
		List<VillagerEntity> sleepingVillagers = world.getEntitiesByClass(
			VillagerEntity.class,
			new Box(pos),
			LivingEntity::isSleeping
		);

		if (sleepingVillagers.isEmpty()) {
			return false;
		}

		sleepingVillagers.get(0).wakeUp();
		return true;
	}

	@Override
	public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		super.onLandedUpon(world, state, pos, entity, fallDistance * 0.5);
	}

	@Override
	public void onEntityLand(BlockView world, Entity entity) {
		if (entity.bypassesLandingEffects()) {
			super.onEntityLand(world, entity);
		} else {
			bounceEntity(entity);
		}
	}

	private void bounceEntity(Entity entity) {
		Vec3d velocity = entity.getVelocity();

		if (velocity.y < 0.0) {
			double bounceFactor = entity instanceof LivingEntity ? LIVING_BOUNCE_FACTOR : NONLIVING_BOUNCE_FACTOR;
			entity.setVelocity(velocity.x, -velocity.y * BOUNCE_MULTIPLIER * bounceFactor, velocity.z);
		}
	}

	@Override
	protected BlockState getStateForNeighborUpdate(
		BlockState state,
		WorldView world,
		ScheduledTickView tickView,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		Random random
	) {
		if (direction != getDirectionTowardsOtherPart(state.get(PART), state.get(FACING))) {
			return super.getStateForNeighborUpdate(
				state,
				world,
				tickView,
				pos,
				direction,
				neighborPos,
				neighborState,
				random
			);
		}

		return neighborState.isOf(this) && neighborState.get(PART) != state.get(PART)
			? state.with(OCCUPIED, neighborState.get(OCCUPIED))
			: Blocks.AIR.getDefaultState();
	}

	private static Direction getDirectionTowardsOtherPart(BedPart part, Direction direction) {
		return part == BedPart.FOOT ? direction : direction.getOpposite();
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (world.isClient() == false && player.shouldSkipBlockDrops()) {
			BedPart bedPart = state.get(PART);

			if (bedPart == BedPart.FOOT) {
				BlockPos headPos = pos.offset(getDirectionTowardsOtherPart(bedPart, state.get(FACING)));
				BlockState headState = world.getBlockState(headPos);

				if (headState.isOf(this) && headState.get(PART) == BedPart.HEAD) {
					world.setBlockState(headPos, Blocks.AIR.getDefaultState(), BREAK_UPDATE_FLAGS);
					world.syncWorldEvent(player, 2001, headPos, Block.getRawIdFromState(headState));
				}
			}
		}

		return super.onBreak(world, pos, state, player);
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction direction = ctx.getHorizontalPlayerFacing();
		BlockPos footPos = ctx.getBlockPos();
		BlockPos headPos = footPos.offset(direction);
		World world = ctx.getWorld();

		return world.getBlockState(headPos).canReplace(ctx) && world.getWorldBorder().contains(headPos)
			? getDefaultState().with(FACING, direction)
			: null;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_DIRECTION.get(getOppositePartDirection(state).getOpposite());
	}

	public static Direction getOppositePartDirection(BlockState state) {
		Direction direction = state.get(FACING);
		return state.get(PART) == BedPart.HEAD ? direction.getOpposite() : direction;
	}

	public static DoubleBlockProperties.Type getBedPart(BlockState state) {
		BedPart bedPart = state.get(PART);
		return bedPart == BedPart.HEAD ? DoubleBlockProperties.Type.FIRST : DoubleBlockProperties.Type.SECOND;
	}

	private static boolean isBedBelow(BlockView world, BlockPos pos) {
		return world.getBlockState(pos.down()).getBlock() instanceof BedBlock;
	}

	/**
	 * Ищет позицию пробуждения вокруг кровати с учётом направления кровати и угла спауна.
	 * Если под кроватью есть другая кровать, делегирует поиск с учётом этого.
	 *
	 * @param type тип сущности, которая просыпается
	 * @param world мир
	 * @param pos позиция головной части кровати
	 * @param bedDirection направление кровати
	 * @param spawnAngle угол спауна игрока
	 * @return позиция пробуждения или {@link Optional#empty()}
	 */
	public static Optional<Vec3d> findWakeUpPosition(
		EntityType<?> type,
		CollisionView world,
		BlockPos pos,
		Direction bedDirection,
		float spawnAngle
	) {
		Direction sideDirection = bedDirection.rotateYClockwise();
		Direction respawnDirection = sideDirection.pointsTo(spawnAngle) ? sideDirection.getOpposite() : sideDirection;

		if (isBedBelow(world, pos)) {
			return findWakeUpPosition(type, world, pos, bedDirection, respawnDirection);
		}

		int[][] offsets = getAroundAndOnBedOffsets(bedDirection, respawnDirection);
		Optional<Vec3d> result = findWakeUpPosition(type, world, pos, offsets, true);
		return result.isPresent() ? result : findWakeUpPosition(type, world, pos, offsets, false);
	}

	private static Optional<Vec3d> findWakeUpPosition(
		EntityType<?> type,
		CollisionView world,
		BlockPos pos,
		Direction bedDirection,
		Direction respawnDirection
	) {
		int[][] aroundOffsets = getAroundBedOffsets(bedDirection, respawnDirection);
		int[][] onOffsets = getOnBedOffsets(bedDirection);
		BlockPos belowPos = pos.down();

		Optional<Vec3d> result = findWakeUpPosition(type, world, pos, aroundOffsets, true);
		if (result.isPresent()) {
			return result;
		}

		result = findWakeUpPosition(type, world, belowPos, aroundOffsets, true);
		if (result.isPresent()) {
			return result;
		}

		result = findWakeUpPosition(type, world, pos, onOffsets, true);
		if (result.isPresent()) {
			return result;
		}

		result = findWakeUpPosition(type, world, pos, aroundOffsets, false);
		if (result.isPresent()) {
			return result;
		}

		result = findWakeUpPosition(type, world, belowPos, aroundOffsets, false);
		return result.isPresent() ? result : findWakeUpPosition(type, world, pos, onOffsets, false);
	}

	private static Optional<Vec3d> findWakeUpPosition(
		EntityType<?> type,
		CollisionView world,
		BlockPos pos,
		int[][] possibleOffsets,
		boolean ignoreInvalidPos
	) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int[] offset : possibleOffsets) {
			mutable.set(pos.getX() + offset[0], pos.getY(), pos.getZ() + offset[1]);
			Vec3d respawnPos = Dismounting.findRespawnPos(type, world, mutable, ignoreInvalidPos);

			if (respawnPos != null) {
				return Optional.of(respawnPos);
			}
		}

		return Optional.empty();
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART, OCCUPIED);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BedBlockEntity(pos, state, color);
	}

	@Override
	public void onPlaced(
		World world,
		BlockPos pos,
		BlockState state,
		@Nullable LivingEntity placer,
		ItemStack itemStack
	) {
		super.onPlaced(world, pos, state, placer, itemStack);

		if (world.isClient() == false) {
			BlockPos headPos = pos.offset(state.get(FACING));
			world.setBlockState(headPos, state.with(PART, BedPart.HEAD), 3);
			world.updateNeighbors(pos, Blocks.AIR);
			state.updateNeighbors(world, pos, 3);
		}
	}

	public DyeColor getColor() {
		return color;
	}

	@Override
	protected long getRenderingSeed(BlockState state, BlockPos pos) {
		BlockPos seedPos = pos.offset(state.get(FACING), state.get(PART) == BedPart.HEAD ? 0 : 1);
		return MathHelper.hashCode(seedPos.getX(), pos.getY(), seedPos.getZ());
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	private static int[][] getAroundAndOnBedOffsets(Direction bedDirection, Direction respawnDirection) {
		return (int[][]) ArrayUtils.addAll(
			getAroundBedOffsets(bedDirection, respawnDirection),
			getOnBedOffsets(bedDirection)
		);
	}

	private static int[][] getAroundBedOffsets(Direction bedDirection, Direction respawnDirection) {
		return new int[][]{
			{respawnDirection.getOffsetX(), respawnDirection.getOffsetZ()},
			{
				respawnDirection.getOffsetX() - bedDirection.getOffsetX(),
				respawnDirection.getOffsetZ() - bedDirection.getOffsetZ()
			},
			{
				respawnDirection.getOffsetX() - bedDirection.getOffsetX() * 2,
				respawnDirection.getOffsetZ() - bedDirection.getOffsetZ() * 2
			},
			{-bedDirection.getOffsetX() * 2, -bedDirection.getOffsetZ() * 2},
			{
				-respawnDirection.getOffsetX() - bedDirection.getOffsetX() * 2,
				-respawnDirection.getOffsetZ() - bedDirection.getOffsetZ() * 2
			},
			{
				-respawnDirection.getOffsetX() - bedDirection.getOffsetX(),
				-respawnDirection.getOffsetZ() - bedDirection.getOffsetZ()
			},
			{-respawnDirection.getOffsetX(), -respawnDirection.getOffsetZ()},
			{
				-respawnDirection.getOffsetX() + bedDirection.getOffsetX(),
				-respawnDirection.getOffsetZ() + bedDirection.getOffsetZ()
			},
			{bedDirection.getOffsetX(), bedDirection.getOffsetZ()},
			{
				respawnDirection.getOffsetX() + bedDirection.getOffsetX(),
				respawnDirection.getOffsetZ() + bedDirection.getOffsetZ()
			}
		};
	}

	private static int[][] getOnBedOffsets(Direction bedDirection) {
		return new int[][]{{0, 0}, {-bedDirection.getOffsetX(), -bedDirection.getOffsetZ()}};
	}
}
