package net.minecraft.entity;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Сущность «Глаза Края», выпущенного игроком. Летит к ближайшей крепости,
 * ограничивая горизонтальное расстояние полёта до {@code MAX_HORIZONTAL_TRAVEL_DISTANCE} блоков.
 * После 80 тиков либо выпадает предметом (с вероятностью 4/5), либо разрушается с эффектом.
 */
public class EyeOfEnderEntity extends Entity implements FlyingItemEntity {

	private static final float MIN_RENDER_DISTANCE_SQUARED = 12.25F;
	private static final float MAX_HORIZONTAL_TRAVEL_DISTANCE = 12.0F;
	private static final int LIFESPAN_TICKS = 80;
	private static final int SHATTER_WORLD_EVENT = 2003;
	private static final int BUBBLE_PARTICLE_COUNT = 4;
	private static final TrackedData<ItemStack>
			ITEM =
			DataTracker.registerData(EyeOfEnderEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
	private @Nullable Vec3d targetPos;
	private int lifespan;
	private boolean dropsItem;

	public EyeOfEnderEntity(EntityType<? extends EyeOfEnderEntity> entityType, World world) {
		super(entityType, world);
	}

	public EyeOfEnderEntity(World world, double x, double y, double z) {
		this(EntityType.EYE_OF_ENDER, world);
		this.setPosition(x, y, z);
	}

	public void setItem(ItemStack stack) {
		getDataTracker().set(ITEM, stack.isEmpty() ? getItem() : stack.copyWithCount(1));
	}

	@Override
	public ItemStack getStack() {
		return getDataTracker().get(ITEM);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(ITEM, getItem());
	}

	@Override
	public boolean shouldRender(double distance) {
		if (age < 2 && distance < MIN_RENDER_DISTANCE_SQUARED) {
			return false;
		}

		double renderRange = getBoundingBox().getAverageSideLength() * 4.0;
		if (Double.isNaN(renderRange)) {
			renderRange = 4.0;
		}

		renderRange *= 64.0;
		return distance < renderRange * renderRange;
	}

	/**
	 * Задаёт целевую позицию полёта. Если цель дальше {@code MAX_HORIZONTAL_TRAVEL_DISTANCE},
	 * обрезает вектор до этого расстояния, добавляя 8 блоков по Y для дугообразной траектории.
	 *
	 * @param pos целевая позиция (обычно координаты крепости)
	 */
	public void initTargetPos(Vec3d pos) {
		Vec3d direction = pos.subtract(getEntityPos());
		double horizontalDist = direction.horizontalLength();
		targetPos = horizontalDist > MAX_HORIZONTAL_TRAVEL_DISTANCE
			? getEntityPos().add(direction.x * (MAX_HORIZONTAL_TRAVEL_DISTANCE / horizontalDist), 8.0, direction.z * (MAX_HORIZONTAL_TRAVEL_DISTANCE / horizontalDist))
			: pos;

		lifespan = 0;
		dropsItem = random.nextInt(5) > 0;
	}

	@Override
	public void tick() {
		super.tick();
		Vec3d nextPos = getEntityPos().add(getVelocity());

		if (getEntityWorld().isClient()) {
			Vec3d trailPos = nextPos.subtract(getVelocity().multiply(0.25));
			addParticles(trailPos, getVelocity());
		}
		else {
			if (targetPos != null) {
				setVelocity(updateVelocity(getVelocity(), nextPos, targetPos));
			}

			lifespan++;
			if (lifespan > LIFESPAN_TICKS) {
				playSound(SoundEvents.ENTITY_ENDER_EYE_DEATH, 1.0F, 1.0F);
				discard();
				if (dropsItem) {
					getEntityWorld().spawnEntity(
						new ItemEntity(getEntityWorld(), getX(), getY(), getZ(), getStack())
					);
				}
				else {
					getEntityWorld().syncWorldEvent(SHATTER_WORLD_EVENT, getBlockPos(), 0);
				}
			}
		}

		setPosition(nextPos);
	}

	private void addParticles(Vec3d pos, Vec3d velocity) {
		if (isTouchingWater()) {
			for (int bubble = 0; bubble < BUBBLE_PARTICLE_COUNT; bubble++) {
				getEntityWorld().addParticleClient(
					ParticleTypes.BUBBLE, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z
				);
			}
		}
		else {
			getEntityWorld().addParticleClient(
				ParticleTypes.PORTAL,
				pos.x + random.nextDouble() * 0.6 - 0.3,
				pos.y - 0.5,
				pos.z + random.nextDouble() * 0.6 - 0.3,
				velocity.x,
				velocity.y,
				velocity.z
			);
		}
	}

	private static Vec3d updateVelocity(Vec3d velocity, Vec3d currentPos, Vec3d targetPos) {
		Vec3d horizontalDelta = new Vec3d(targetPos.x - currentPos.x, 0.0, targetPos.z - currentPos.z);
		double horizontalDist = horizontalDelta.length();
		double horizontalSpeed = MathHelper.lerp(0.0025, velocity.horizontalLength(), horizontalDist);
		double verticalSpeed = velocity.y;
		if (horizontalDist < 1.0) {
			horizontalSpeed *= 0.8;
			verticalSpeed *= 0.8;
		}

		double verticalDirection = currentPos.y - velocity.y < targetPos.y ? 1.0 : -1.0;
		return horizontalDelta.multiply(horizontalSpeed / horizontalDist)
		                      .add(0.0, verticalSpeed + (verticalDirection - verticalSpeed) * 0.015, 0.0);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.put("Item", ItemStack.CODEC, getStack());
	}

	@Override
	protected void readCustomData(ReadView view) {
		setItem(view.<ItemStack>read("Item", ItemStack.CODEC).orElse(getItem()));
	}

	private ItemStack getItem() {
		return new ItemStack(Items.ENDER_EYE);
	}

	@Override
	public float getBrightnessAtEyes() {
		return 1.0F;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}
}
