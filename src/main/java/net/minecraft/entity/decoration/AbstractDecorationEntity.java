package net.minecraft.entity.decoration;

import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Базовый класс для декоративных сущностей, прикреплённых к блоку и ориентированных по горизонтали
 * (рамки, картины). Управляет отслеживаемым направлением и проверкой пространства для размещения.
 */
public abstract class AbstractDecorationEntity extends BlockAttachedEntity {

	private static final TrackedData<Direction> FACING =
			DataTracker.registerData(AbstractDecorationEntity.class, TrackedDataHandlerRegistry.FACING);
	private static final Direction DEFAULT_FACING = Direction.SOUTH;

	protected AbstractDecorationEntity(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
		super(entityType, world);
	}

	protected AbstractDecorationEntity(
			EntityType<? extends AbstractDecorationEntity> type,
			World world,
			BlockPos pos
	) {
		this(type, world);
		attachedBlockPos = pos;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(FACING, DEFAULT_FACING);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (data.equals(FACING)) {
			setFacing(getHorizontalFacing());
		}
	}

	@Override
	public Direction getHorizontalFacing() {
		return dataTracker.get(FACING);
	}

	protected void setFacingInternal(Direction facing) {
		dataTracker.set(FACING, facing);
	}

	/**
	 * Устанавливает горизонтальное направление сущности, обновляя yaw и позицию прикрепления.
	 * Требует строго горизонтального направления (не вверх/вниз).
	 */
	protected void setFacing(Direction facing) {
		Objects.requireNonNull(facing);
		Validate.isTrue(facing.getAxis().isHorizontal());
		setFacingInternal(facing);
		setYaw(facing.getHorizontalQuarterTurns() * 90);
		lastYaw = getYaw();
		updateAttachmentPosition();
	}

	@Override
	protected void updateAttachmentPosition() {
		if (getHorizontalFacing() == null) {
			return;
		}

		Box box = calculateBoundingBox(attachedBlockPos, getHorizontalFacing());
		Vec3d center = box.getCenter();
		setPos(center.x, center.y, center.z);
		setBoundingBox(box);
	}

	/**
	 * Вычисляет хитбокс сущности на основе позиции блока-опоры и стороны прикрепления.
	 */
	protected abstract Box calculateBoundingBox(BlockPos pos, Direction side);

	@Override
	public boolean canStayAttached() {
		if (isSpaceBlocked(getCheckBoundingBox())) {
			return false;
		}

		boolean allSolid = BlockPos.stream(getAttachmentBox()).allMatch(pos -> {
			BlockState blockState = getEntityWorld().getBlockState(pos);
			return blockState.isSolid() || AbstractRedstoneGateBlock.isRedstoneGate(blockState);
		});

		return allSolid && hasNoIntersectingDecoration(false);
	}

	/**
	 * Возвращает AABB блока-опоры, смещённый в сторону прикрепления на половину блока.
	 * Используется для проверки твёрдости поверхности.
	 */
	protected Box getAttachmentBox() {
		return getBoundingBox().offset(getHorizontalFacing().getUnitVector().mul(-0.5F)).contract(1.0E-7);
	}

	/**
	 * Проверяет, нет ли пересекающихся декоративных сущностей того же типа или направления.
	 *
	 * @param skipTypeCheck если {@code true} — проверяет только совпадение направления, игнорируя тип
	 */
	protected boolean hasNoIntersectingDecoration(boolean skipTypeCheck) {
		Predicate<AbstractDecorationEntity> predicate = entity -> {
			boolean sameType = !skipTypeCheck && entity.getType() == getType();
			boolean sameFacing = entity.getHorizontalFacing() == getHorizontalFacing();
			return entity != this && (sameType || sameFacing);
		};

		return !getEntityWorld()
				.hasEntities(TypeFilter.instanceOf(AbstractDecorationEntity.class), getCheckBoundingBox(), predicate);
	}

	/**
	 * Проверяет, заблокировано ли пространство блоком или другой сущностью.
	 */
	protected boolean isSpaceBlocked(Box box) {
		World world = getEntityWorld();
		return !world.isBlockSpaceEmpty(this, box) || !world.isSpaceEmpty(this, box);
	}

	protected Box getCheckBoundingBox() {
		return getBoundingBox();
	}

	/** Вызывается при размещении сущности в мире. */
	public abstract void onPlace();

	@Override
	public ItemEntity dropStack(ServerWorld world, ItemStack stack, float yOffset) {
		ItemEntity itemEntity = new ItemEntity(
				getEntityWorld(),
				getX() + getHorizontalFacing().getOffsetX() * 0.15F,
				getY() + yOffset,
				getZ() + getHorizontalFacing().getOffsetZ() * 0.15F,
				stack
		);
		itemEntity.setToDefaultPickupDelay();
		getEntityWorld().spawnEntity(itemEntity);
		return itemEntity;
	}

	@Override
	public float applyRotation(BlockRotation rotation) {
		Direction direction = getHorizontalFacing();
		if (direction.getAxis() != Direction.Axis.Y) {
			switch (rotation) {
				case CLOCKWISE_180:
					direction = direction.getOpposite();
					break;
				case COUNTERCLOCKWISE_90:
					direction = direction.rotateYCounterclockwise();
					break;
				case CLOCKWISE_90:
					direction = direction.rotateYClockwise();
			}

			setFacing(direction);
		}

		float yaw = MathHelper.wrapDegrees(getYaw());

		return switch (rotation) {
			case CLOCKWISE_180 -> yaw + 180.0F;
			case COUNTERCLOCKWISE_90 -> yaw + 90.0F;
			case CLOCKWISE_90 -> yaw + 270.0F;
			default -> yaw;
		};
	}

	@Override
	public float applyMirror(BlockMirror mirror) {
		return applyRotation(mirror.getRotation(getHorizontalFacing()));
	}
}
