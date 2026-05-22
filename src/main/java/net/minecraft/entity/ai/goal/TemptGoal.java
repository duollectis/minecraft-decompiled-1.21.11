package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Цель, заставляющая существо следовать за игроком, держащим приманку.
 * Прерывается, если игрок двигается или поворачивает голову (режим пугливости).
 */
public class TemptGoal extends Goal {

	private static final TargetPredicate
			TEMPTING_ENTITY_PREDICATE =
			TargetPredicate.createNonAttackable().ignoreVisibility();
	private static final double DEFAULT_RANGE = 2.5;
	private final TargetPredicate predicate;
	protected final MobEntity mob;
	protected final double speed;
	private double lastPlayerX;
	private double lastPlayerY;
	private double lastPlayerZ;
	private double lastPlayerPitch;
	private double lastPlayerYaw;
	protected @Nullable PlayerEntity closestPlayer;
	private int cooldown;
	private boolean active;
	private final Predicate<ItemStack> temptItemPredicate;
	private final boolean canBeScared;
	private final double range;

	public TemptGoal(
			PathAwareEntity entity,
			double speed,
			Predicate<ItemStack> temptItemPredicate,
			boolean canBeScared
	) {
		this((MobEntity) entity, speed, temptItemPredicate, canBeScared, DEFAULT_RANGE);
	}

	public TemptGoal(
			PathAwareEntity entity,
			double speed,
			Predicate<ItemStack> temptItemPredicate,
			boolean canBeScared,
			double range
	) {
		this((MobEntity) entity, speed, temptItemPredicate, canBeScared, range);
	}

	TemptGoal(MobEntity mob, double speed, Predicate<ItemStack> temptItemPredicate, boolean canBeScared, double range) {
		this.mob = mob;
		this.speed = speed;
		this.temptItemPredicate = temptItemPredicate;
		this.canBeScared = canBeScared;
		this.range = range;
		setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		predicate = TEMPTING_ENTITY_PREDICATE.copy().setPredicate((target, world) -> isTemptedBy(target));
	}

	@Override
	public boolean canStart() {
		if (cooldown > 0) {
			cooldown--;
			return false;
		}

		closestPlayer = getServerWorld(mob)
				.getClosestPlayer(
						predicate.setBaseMaxDistance(mob.getAttributeValue(EntityAttributes.TEMPT_RANGE)),
						mob
				);
		return closestPlayer != null;
	}

	private boolean isTemptedBy(LivingEntity entity) {
		return temptItemPredicate.test(entity.getMainHandStack())
				|| temptItemPredicate.test(entity.getOffHandStack());
	}

	@Override
	public boolean shouldContinue() {
		if (canBeScared()) {
			if (mob.squaredDistanceTo(closestPlayer) < 36.0) {
				if (closestPlayer.squaredDistanceTo(lastPlayerX, lastPlayerY, lastPlayerZ) > 0.010000000000000002) {
					return false;
				}

				if (Math.abs(closestPlayer.getPitch() - lastPlayerPitch) > 5.0
						|| Math.abs(closestPlayer.getYaw() - lastPlayerYaw) > 5.0
				) {
					return false;
				}
			}
			else {
				lastPlayerX = closestPlayer.getX();
				lastPlayerY = closestPlayer.getY();
				lastPlayerZ = closestPlayer.getZ();
			}

			lastPlayerPitch = closestPlayer.getPitch();
			lastPlayerYaw = closestPlayer.getYaw();
		}

		return canStart();
	}

	protected boolean canBeScared() {
		return canBeScared;
	}

	@Override
	public void start() {
		lastPlayerX = closestPlayer.getX();
		lastPlayerY = closestPlayer.getY();
		lastPlayerZ = closestPlayer.getZ();
		active = true;
	}

	@Override
	public void stop() {
		closestPlayer = null;
		stopMoving();
		cooldown = toGoalTicks(100);
		active = false;
	}

	@Override
	public void tick() {
		mob.getLookControl().lookAt(closestPlayer, mob.getMaxHeadRotation() + 20, mob.getMaxLookPitchChange());
		if (mob.squaredDistanceTo(closestPlayer) < range * range) {
			stopMoving();
		}
		else {
			startMovingTo(closestPlayer);
		}
	}

	protected void stopMoving() {
		mob.getNavigation().stop();
	}

	protected void startMovingTo(PlayerEntity player) {
		mob.getNavigation().startMovingTo(player, speed);
	}

	public boolean isActive() {
		return active;
	}

	/**
	 * Специализированная версия для Happy Ghast: использует {@code MoveControl}
	 * вместо навигации, чтобы двигаться в 3D-пространстве к позиции глаз игрока.
	 */
	public static class HappyGhastTemptGoal extends TemptGoal {

		public HappyGhastTemptGoal(
				MobEntity mobEntity,
				double speed,
				Predicate<ItemStack> temptItemPredicate,
				boolean canBeScared,
				double range
		) {
			super(mobEntity, speed, temptItemPredicate, canBeScared, range);
		}

		@Override
		protected void stopMoving() {
			mob.getMoveControl().setWaiting();
		}

		@Override
		protected void startMovingTo(PlayerEntity player) {
			Vec3d targetPos = player
					.getEyePos()
					.subtract(mob.getEntityPos())
					.multiply(mob.getRandom().nextDouble())
					.add(mob.getEntityPos());
			mob.getMoveControl().moveTo(targetPos.x, targetPos.y, targetPos.z, speed);
		}
	}
}
