package net.minecraft.entity.vehicle;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Обычная вагонетка для перевозки пассажиров.
 * При включённых улучшениях вагонеток плавно поворачивает игрока вместе с вагонеткой.
 */
public class MinecartEntity extends AbstractMinecartEntity {

	private static final double LERP_FACTOR = 0.5;
	private static final double MIN_MOVE_DISTANCE = 0.01;
	private static final float WOBBLE_STRENGTH_ON_ACTIVATOR = 50.0F;

	private float currentYawAngle;
	private float prevYawAngle;

	public MinecartEntity(EntityType<?> entityType, World world) {
		super(entityType, world);
	}

	/**
	 * Обрабатывает посадку игрока в вагонетку.
	 * На сервере возвращает {@code CONSUME} при успешной посадке, {@code PASS} — при неудаче.
	 */
	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (player.shouldCancelInteraction() || hasPassengers()) {
			return ActionResult.PASS;
		}

		if (!getEntityWorld().isClient() && !player.startRiding(this)) {
			return ActionResult.PASS;
		}

		prevYawAngle = currentYawAngle;

		if (!getEntityWorld().isClient()) {
			return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
		}

		return ActionResult.SUCCESS;
	}

	@Override
	protected Item asItem() {
		return Items.MINECART;
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.MINECART);
	}

	@Override
	public void onActivatorRail(ServerWorld serverWorld, int x, int y, int z, boolean powered) {
		if (!powered) {
			return;
		}

		if (hasPassengers()) {
			removeAllPassengers();
		}

		if (getDamageWobbleTicks() == 0) {
			setDamageWobbleSide(-getDamageWobbleSide());
			setDamageWobbleTicks(10);
			setDamageWobbleStrength(WOBBLE_STRENGTH_ON_ACTIVATOR);
			scheduleVelocityUpdate();
		}
	}

	@Override
	public boolean isRideable() {
		return true;
	}

	/**
	 * Отслеживает изменение угла поворота за тик для плавного вращения пассажира-игрока.
	 */
	@Override
	public void tick() {
		double yawBefore = getYaw();
		Vec3d posBefore = getEntityPos();
		super.tick();
		double yawDelta = (getYaw() - yawBefore) % 360.0;

		if (getEntityWorld().isClient() && posBefore.distanceTo(getEntityPos()) > MIN_MOVE_DISTANCE) {
			currentYawAngle += (float) yawDelta;
			currentYawAngle %= 360.0F;
		}
	}

	/**
	 * При включённых улучшениях вагонеток плавно интерполирует поворот игрока-пассажира
	 * вместе с вагонеткой для устранения резких рывков.
	 */
	@Override
	protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
		super.updatePassengerPosition(passenger, positionUpdater);

		if (!getEntityWorld().isClient()) {
			return;
		}

		if (!(passenger instanceof PlayerEntity playerEntity)) {
			return;
		}

		if (!playerEntity.shouldRotateWithMinecart()) {
			return;
		}

		if (!areMinecartImprovementsEnabled(getEntityWorld())) {
			return;
		}

		float interpolatedYaw = (float) MathHelper.lerpAngleDegrees(LERP_FACTOR, prevYawAngle, currentYawAngle);
		playerEntity.setYaw(playerEntity.getYaw() - (interpolatedYaw - prevYawAngle));
		prevYawAngle = interpolatedYaw;
	}
}
