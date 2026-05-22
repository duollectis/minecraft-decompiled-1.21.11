package net.minecraft.entity.ai.goal;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.EnumSet;

/**
 * Цель атаки арбалетом: управляет циклом зарядки, ожидания и выстрела.
 * Моб сближается с целью пока не зарядит арбалет, затем стреляет при наличии видимости.
 */
public class CrossbowAttackGoal<T extends HostileEntity & RangedAttackMob & CrossbowUser> extends Goal {

	public static final UniformIntProvider COOLDOWN_RANGE = TimeHelper.betweenSeconds(1, 2);
	private static final int MIN_SEEING_TICKS = 5;
	private static final int CHARGED_WAIT_BASE = 20;
	private static final float LOOK_ANGLE = 30.0F;
	private static final double CHARGED_SPEED_FACTOR = 0.5;

	private final T actor;
	private Stage stage = Stage.UNCHARGED;
	private final double speed;
	private final float squaredRange;
	private int seeingTargetTicker;
	private int chargedTicksLeft;
	private int cooldown;

	public CrossbowAttackGoal(T actor, double speed, float range) {
		this.actor = actor;
		this.speed = speed;
		this.squaredRange = range * range;
		setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		return hasAliveTarget() && isEntityHoldingCrossbow();
	}

	private boolean isEntityHoldingCrossbow() {
		return actor.isHolding(Items.CROSSBOW);
	}

	@Override
	public boolean shouldContinue() {
		return hasAliveTarget() && (canStart() || !actor.getNavigation().isIdle()) && isEntityHoldingCrossbow();
	}

	private boolean hasAliveTarget() {
		return actor.getTarget() != null && actor.getTarget().isAlive();
	}

	@Override
	public void stop() {
		super.stop();
		actor.setAttacking(false);
		actor.setTarget(null);
		seeingTargetTicker = 0;
		if (actor.isUsingItem()) {
			actor.clearActiveItem();
			actor.setCharging(false);
			actor.getActiveItem().set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
		}
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = actor.getTarget();
		if (target == null) {
			return;
		}

		boolean canSee = actor.getVisibilityCache().canSee(target);
		boolean wasSeeingTarget = seeingTargetTicker > 0;
		if (canSee != wasSeeingTarget) {
			seeingTargetTicker = 0;
		}

		seeingTargetTicker = canSee ? seeingTargetTicker + 1 : seeingTargetTicker - 1;

		double distSq = actor.squaredDistanceTo(target);
		boolean shouldApproach = (distSq > squaredRange || seeingTargetTicker < MIN_SEEING_TICKS) && chargedTicksLeft == 0;
		if (shouldApproach) {
			cooldown--;
			if (cooldown <= 0) {
				double moveSpeed = isUncharged() ? speed : speed * CHARGED_SPEED_FACTOR;
				actor.getNavigation().startMovingTo(target, moveSpeed);
				cooldown = COOLDOWN_RANGE.get(actor.getRandom());
			}
		}
		else {
			cooldown = 0;
			actor.getNavigation().stop();
		}

		actor.getLookControl().lookAt(target, LOOK_ANGLE, LOOK_ANGLE);

		if (stage == Stage.UNCHARGED) {
			if (!shouldApproach) {
				actor.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(actor, Items.CROSSBOW));
				stage = Stage.CHARGING;
				actor.setCharging(true);
			}
		}
		else if (stage == Stage.CHARGING) {
			if (!actor.isUsingItem()) {
				stage = Stage.UNCHARGED;
			}

			int drawTicks = actor.getItemUseTime();
			ItemStack crossbow = actor.getActiveItem();
			if (drawTicks >= CrossbowItem.getPullTime(crossbow, actor)) {
				actor.stopUsingItem();
				stage = Stage.CHARGED;
				chargedTicksLeft = CHARGED_WAIT_BASE + actor.getRandom().nextInt(CHARGED_WAIT_BASE);
				actor.setCharging(false);
			}
		}
		else if (stage == Stage.CHARGED) {
			chargedTicksLeft--;
			if (chargedTicksLeft == 0) {
				stage = Stage.READY_TO_ATTACK;
			}
		}
		else if (stage == Stage.READY_TO_ATTACK && canSee) {
			actor.shootAt(target, 1.0F);
			stage = Stage.UNCHARGED;
		}
	}

	private boolean isUncharged() {
		return stage == Stage.UNCHARGED;
	}

	enum Stage {
		UNCHARGED,
		CHARGING,
		CHARGED,
		READY_TO_ATTACK
	}
}
