package net.minecraft.block.entity;

import net.minecraft.block.*;
import net.minecraft.block.enums.PistonType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.block.OrientationHelper;

import java.util.List;

/**
 * Блок-сущность движущегося поршня. Управляет анимацией выдвижения/втягивания,
 * физическим перемещением сущностей и финализацией состояния блока по завершении хода.
 */
public class PistonBlockEntity extends BlockEntity {

	private static final int PUSH_TICKS = 2;
	private static final double THRESHOLD = 0.01;
	public static final double PUSH_OFFSET = 0.51;
	private static final BlockState DEFAULT_PUSHED_BLOCK_STATE = Blocks.AIR.getDefaultState();
	private static final ThreadLocal<Direction> ENTITY_MOVEMENT_DIRECTION = ThreadLocal.withInitial(() -> null);

	private BlockState pushedBlockState = DEFAULT_PUSHED_BLOCK_STATE;
	private Direction facing;
	private boolean extending = false;
	private boolean source = false;
	private float progress = 0.0F;
	private float lastProgress = 0.0F;
	private long savedWorldTime;
	private int soundCounter;

	public PistonBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.PISTON, pos, state);
	}

	public PistonBlockEntity(
			BlockPos pos,
			BlockState state,
			BlockState pushedBlock,
			Direction facing,
			boolean extending,
			boolean source
	) {
		this(pos, state);
		this.pushedBlockState = pushedBlock;
		this.facing = facing;
		this.extending = extending;
		this.source = source;
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}

	public boolean isExtending() {
		return extending;
	}

	public Direction getFacing() {
		return facing;
	}

	public boolean isSource() {
		return source;
	}

	public float getProgress(float tickProgress) {
		float clamped = Math.min(tickProgress, 1.0F);
		return MathHelper.lerp(clamped, lastProgress, progress);
	}

	public float getRenderOffsetX(float tickProgress) {
		return facing.getOffsetX() * getAmountExtended(getProgress(tickProgress));
	}

	public float getRenderOffsetY(float tickProgress) {
		return facing.getOffsetY() * getAmountExtended(getProgress(tickProgress));
	}

	public float getRenderOffsetZ(float tickProgress) {
		return facing.getOffsetZ() * getAmountExtended(getProgress(tickProgress));
	}

	private float getAmountExtended(float prog) {
		return extending ? prog - 1.0F : 1.0F - prog;
	}

	private BlockState getHeadBlockState() {
		return !isExtending() && isSource() && pushedBlockState.getBlock() instanceof PistonBlock
			? Blocks.PISTON_HEAD
				.getDefaultState()
				.with(PistonHeadBlock.SHORT, progress > 0.25F)
				.with(
					PistonHeadBlock.TYPE,
					pushedBlockState.isOf(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT
				)
				.with(PistonHeadBlock.FACING, pushedBlockState.get(PistonBlock.FACING))
			: pushedBlockState;
	}

	private static void pushEntities(World world, BlockPos pos, float targetProgress, PistonBlockEntity blockEntity) {
		Direction direction = blockEntity.getMovementDirection();
		double delta = targetProgress - blockEntity.progress;
		VoxelShape headShape = blockEntity.getHeadBlockState().getCollisionShape(world, pos);

		if (headShape.isEmpty()) {
			return;
		}

		Box headBox = offsetHeadBox(pos, headShape.getBoundingBox(), blockEntity);
		List<Entity> entities = world.getOtherEntities(null, Boxes.stretch(headBox, direction, delta).union(headBox));

		if (entities.isEmpty()) {
			return;
		}

		List<Box> headBoxes = headShape.getBoundingBoxes();
		boolean isSlime = blockEntity.pushedBlockState.isOf(Blocks.SLIME_BLOCK);

		for (Entity entity : entities) {
			if (entity.getPistonBehavior() == PistonBehavior.IGNORE) {
				continue;
			}

			if (isSlime && !(entity instanceof ServerPlayerEntity)) {
				Vec3d velocity = entity.getVelocity();
				double velX = velocity.x;
				double velY = velocity.y;
				double velZ = velocity.z;

				switch (direction.getAxis()) {
					case X -> velX = direction.getOffsetX();
					case Y -> velY = direction.getOffsetY();
					case Z -> velZ = direction.getOffsetZ();
				}

				entity.setVelocity(velX, velY, velZ);
			}

			double pushAmount = 0.0;

			for (Box headPart : headBoxes) {
				Box stretchedPart = Boxes.stretch(offsetHeadBox(pos, headPart, blockEntity), direction, delta);
				Box entityBox = entity.getBoundingBox();

				if (stretchedPart.intersects(entityBox)) {
					pushAmount = Math.max(pushAmount, getIntersectionSize(stretchedPart, direction, entityBox));

					if (pushAmount >= delta) {
						break;
					}
				}
			}

			if (pushAmount > 0.0) {
				pushAmount = Math.min(pushAmount, delta) + THRESHOLD;
				moveEntity(direction, entity, pushAmount, direction);

				if (!blockEntity.extending && blockEntity.source) {
					push(pos, entity, direction, delta);
				}
			}
		}
	}

	private static void moveEntity(Direction direction, Entity entity, double distance, Direction movementDirection) {
		ENTITY_MOVEMENT_DIRECTION.set(direction);
		Vec3d vec3d = entity.getEntityPos();
		entity.move(
				MovementType.PISTON,
				new Vec3d(
						distance * movementDirection.getOffsetX(),
						distance * movementDirection.getOffsetY(),
						distance * movementDirection.getOffsetZ()
				)
		);
		entity.tickBlockCollision(vec3d, entity.getEntityPos());
		entity.popQueuedCollisionCheck();
		ENTITY_MOVEMENT_DIRECTION.set(null);
	}

	private static void moveEntitiesInHoneyBlock(World world, BlockPos pos, float targetProgress, PistonBlockEntity blockEntity) {
		if (!blockEntity.isPushingHoneyBlock()) {
			return;
		}

		Direction direction = blockEntity.getMovementDirection();

		if (!direction.getAxis().isHorizontal()) {
			return;
		}

		double topY = blockEntity.pushedBlockState.getCollisionShape(world, pos).getMax(Direction.Axis.Y);
		Box stickyZone = offsetHeadBox(pos, new Box(0.0, topY, 0.0, 1.0, 1.5000010000000001, 1.0), blockEntity);
		double delta = targetProgress - blockEntity.progress;

		for (Entity entity : world.getOtherEntities(null, stickyZone, entityx -> canMoveEntity(stickyZone, entityx, pos))) {
			moveEntity(direction, entity, delta, direction);
		}
	}

	private static boolean canMoveEntity(Box box, Entity entity, BlockPos pos) {
		return entity.getPistonBehavior() == PistonBehavior.NORMAL
				&& entity.isOnGround()
				&& (entity.isSupportedBy(pos)
				|| entity.getX() >= box.minX && entity.getX() <= box.maxX && entity.getZ() >= box.minZ
				&& entity.getZ() <= box.maxZ
		);
	}

	private boolean isPushingHoneyBlock() {
		return this.pushedBlockState.isOf(Blocks.HONEY_BLOCK);
	}

	public Direction getMovementDirection() {
		return this.extending ? this.facing : this.facing.getOpposite();
	}

	private static double getIntersectionSize(Box box, Direction direction, Box box2) {
		switch (direction) {
			case EAST:
				return box.maxX - box2.minX;
			case WEST:
				return box2.maxX - box.minX;
			case UP:
			default:
				return box.maxY - box2.minY;
			case DOWN:
				return box2.maxY - box.minY;
			case SOUTH:
				return box.maxZ - box2.minZ;
			case NORTH:
				return box2.maxZ - box.minZ;
		}
	}

	private static Box offsetHeadBox(BlockPos pos, Box box, PistonBlockEntity blockEntity) {
		double d = blockEntity.getAmountExtended(blockEntity.progress);
		return box.offset(
				pos.getX() + d * blockEntity.facing.getOffsetX(),
				pos.getY() + d * blockEntity.facing.getOffsetY(),
				pos.getZ() + d * blockEntity.facing.getOffsetZ()
		);
	}

	private static void push(BlockPos pos, Entity entity, Direction direction, double amount) {
		Box box = entity.getBoundingBox();
		Box box2 = VoxelShapes.fullCube().getBoundingBox().offset(pos);
		if (box.intersects(box2)) {
			Direction direction2 = direction.getOpposite();
			double d = getIntersectionSize(box2, direction2, box) + THRESHOLD;
			double e = getIntersectionSize(box2, direction2, box.intersection(box2)) + THRESHOLD;
			if (Math.abs(d - e) < THRESHOLD) {
				d = Math.min(d, amount) + THRESHOLD;
				moveEntity(direction, entity, d, direction2);
			}
		}
	}

	public BlockState getPushedBlock() {
		return pushedBlockState;
	}

	/**
	 * Завершает анимацию поршня досрочно: устанавливает прогресс в 1.0, удаляет блок-сущность
	 * и финализирует состояние блока (либо воздух для источника, либо постобработанное состояние).
	 */
	public void finish() {
		if (world == null || (lastProgress >= 1.0F && !world.isClient())) {
			return;
		}

		progress = 1.0F;
		lastProgress = progress;
		world.removeBlockEntity(pos);
		markRemoved();

		if (!world.getBlockState(pos).isOf(Blocks.MOVING_PISTON)) {
			return;
		}

		BlockState finalState = source
			? Blocks.AIR.getDefaultState()
			: Block.postProcessState(pushedBlockState, world, pos);

		world.setBlockState(pos, finalState, 3);
		world.updateNeighbor(
			pos,
			finalState.getBlock(),
			OrientationHelper.getEmissionOrientation(world, getDirection(), null)
		);
	}

	@Override
	public void onBlockReplaced(BlockPos pos, BlockState oldState) {
		finish();
	}

	public Direction getDirection() {
		return extending ? facing : facing.getOpposite();
	}

	public static void tick(World world, BlockPos pos, BlockState state, PistonBlockEntity blockEntity) {
		blockEntity.savedWorldTime = world.getTime();
		blockEntity.lastProgress = blockEntity.progress;

		if (blockEntity.lastProgress >= 1.0F) {
			if (world.isClient() && blockEntity.soundCounter < PUSH_TICKS + 3) {
				blockEntity.soundCounter++;
				return;
			}

			world.removeBlockEntity(pos);
			blockEntity.markRemoved();

			if (!world.getBlockState(pos).isOf(Blocks.MOVING_PISTON)) {
				return;
			}

			BlockState finalState = Block.postProcessState(blockEntity.pushedBlockState, world, pos);

			if (finalState.isAir()) {
				world.setBlockState(pos, blockEntity.pushedBlockState, 340);
				Block.replace(blockEntity.pushedBlockState, finalState, world, pos, 3);
				return;
			}

			if (finalState.contains(Properties.WATERLOGGED) && finalState.get(Properties.WATERLOGGED)) {
				finalState = finalState.with(Properties.WATERLOGGED, false);
			}

			world.setBlockState(pos, finalState, 67);
			world.updateNeighbor(
				pos,
				finalState.getBlock(),
				OrientationHelper.getEmissionOrientation(world, blockEntity.getDirection(), null)
			);

			return;
		}

		float nextProgress = blockEntity.progress + 0.5F;
		pushEntities(world, pos, nextProgress, blockEntity);
		moveEntitiesInHoneyBlock(world, pos, nextProgress, blockEntity);
		blockEntity.progress = Math.min(nextProgress, 1.0F);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		pushedBlockState = view.<BlockState>read("blockState", BlockState.CODEC).orElse(DEFAULT_PUSHED_BLOCK_STATE);
		facing = view.<Direction>read("facing", Direction.INDEX_CODEC).orElse(Direction.DOWN);
		progress = view.getFloat("progress", 0.0F);
		lastProgress = progress;
		extending = view.getBoolean("extending", false);
		source = view.getBoolean("source", false);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.put("blockState", BlockState.CODEC, pushedBlockState);
		view.put("facing", Direction.INDEX_CODEC, facing);
		view.putFloat("progress", lastProgress);
		view.putBoolean("extending", extending);
		view.putBoolean("source", source);
	}

	public VoxelShape getCollisionShape(BlockView world, BlockPos pos) {
		VoxelShape baseShape = (!extending && source && pushedBlockState.getBlock() instanceof PistonBlock)
			? pushedBlockState.with(PistonBlock.EXTENDED, true).getCollisionShape(world, pos)
			: VoxelShapes.empty();

		Direction movementDir = ENTITY_MOVEMENT_DIRECTION.get();

		if (progress < 1.0 && movementDir == getMovementDirection()) {
			return baseShape;
		}

		BlockState movingState = isSource()
			? Blocks.PISTON_HEAD
				.getDefaultState()
				.with(PistonHeadBlock.FACING, facing)
				.with(PistonHeadBlock.SHORT, extending != 1.0F - progress < 0.25F)
			: pushedBlockState;

		float extended = getAmountExtended(progress);
		double offsetX = facing.getOffsetX() * extended;
		double offsetY = facing.getOffsetY() * extended;
		double offsetZ = facing.getOffsetZ() * extended;

		return VoxelShapes.union(baseShape, movingState.getCollisionShape(world, pos).offset(offsetX, offsetY, offsetZ));
	}

	public long getSavedWorldTime() {
		return savedWorldTime;
	}

	@Override
	public void setWorld(World world) {
		super.setWorld(world);

		if (world
			.createCommandRegistryWrapper(RegistryKeys.BLOCK)
			.getOptional(pushedBlockState.getBlock().getRegistryEntry().registryKey())
			.isEmpty()
		) {
			pushedBlockState = Blocks.AIR.getDefaultState();
		}
	}
}
