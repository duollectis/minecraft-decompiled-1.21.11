package net.minecraft.entity.projectile.thrown;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Базовый класс для всех бросаемых снарядов (яйца, зелья, жемчуг и т.д.).
 * <p>
 * Реализует общую физику: гравитацию, замедление (drag) с пузырьками в воде,
 * обработку столкновений и начальное взаимодействие с пузырьковыми столбами.
 */
public abstract class ThrownEntity extends ProjectileEntity {

	private static final float MIN_RENDER_DISTANCE_SQUARED = 12.25F;

	/** Коэффициент замедления в воде. */
	private static final float DRAG_IN_WATER = 0.8F;

	/** Коэффициент замедления в воздухе. */
	private static final float DRAG_IN_AIR = 0.99F;

	/** Смещение частицы пузырька относительно текущей позиции (доля от скорости). */
	private static final float BUBBLE_PARTICLE_OFFSET = 0.25F;

	/** Количество пузырьков при движении в воде. */
	private static final int BUBBLE_PARTICLE_COUNT = 4;

	/** Базовый размер для расчёта дистанции рендера. */
	private static final double RENDER_DISTANCE_BASE = 4.0;

	/** Множитель дистанции рендера. */
	private static final double RENDER_DISTANCE_MULTIPLIER = 64.0;

	protected ThrownEntity(EntityType<? extends ThrownEntity> entityType, World world) {
		super(entityType, world);
	}

	protected ThrownEntity(EntityType<? extends ThrownEntity> type, double x, double y, double z, World world) {
		this(type, world);
		setPosition(x, y, z);
	}

	@Override
	public boolean shouldRender(double distance) {
		if (age < 2 && distance < MIN_RENDER_DISTANCE_SQUARED) {
			return false;
		}

		double sideLength = getBoundingBox().getAverageSideLength() * RENDER_DISTANCE_BASE;
		if (Double.isNaN(sideLength)) {
			sideLength = RENDER_DISTANCE_BASE;
		}

		sideLength *= RENDER_DISTANCE_MULTIPLIER;
		return distance < sideLength * sideLength;
	}

	@Override
	public boolean canUsePortals(boolean allowVehicles) {
		return true;
	}

	@Override
	public void tick() {
		tickInitialBubbleColumnCollision();
		applyGravity();
		applyDrag();

		HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
		Vec3d nextPos = hitResult.getType() != HitResult.Type.MISS
				? hitResult.getPos()
				: getEntityPos().add(getVelocity());

		setPosition(nextPos);
		updateRotation();
		tickBlockCollision();
		super.tick();

		if (hitResult.getType() != HitResult.Type.MISS && isAlive()) {
			hitOrDeflect(hitResult);
		}
	}

	private void applyDrag() {
		Vec3d velocity = getVelocity();
		Vec3d pos = getEntityPos();
		float drag;

		if (isTouchingWater()) {
			for (int index = 0; index < BUBBLE_PARTICLE_COUNT; index++) {
				getEntityWorld().addParticleClient(
						ParticleTypes.BUBBLE,
						pos.x - velocity.x * BUBBLE_PARTICLE_OFFSET,
						pos.y - velocity.y * BUBBLE_PARTICLE_OFFSET,
						pos.z - velocity.z * BUBBLE_PARTICLE_OFFSET,
						velocity.x,
						velocity.y,
						velocity.z
				);
			}

			drag = DRAG_IN_WATER;
		} else {
			drag = DRAG_IN_AIR;
		}

		setVelocity(velocity.multiply(drag));
	}

	private void tickInitialBubbleColumnCollision() {
		if (!firstUpdate) {
			return;
		}

		for (BlockPos blockPos : BlockPos.iterate(getBoundingBox())) {
			BlockState blockState = getEntityWorld().getBlockState(blockPos);
			if (blockState.isOf(Blocks.BUBBLE_COLUMN)) {
				blockState.onEntityCollision(
						getEntityWorld(),
						blockPos,
						this,
						EntityCollisionHandler.DUMMY,
						true
				);
			}
		}
	}

	@Override
	protected double getGravity() {
		return 0.03;
	}
}
