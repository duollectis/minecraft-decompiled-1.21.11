package net.minecraft.entity.ai.goal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * Цель укрощения лошади: лошадь мечется, пока игрок сидит верхом,
 * периодически проверяя темперамент и решая — принять или сбросить всадника.
 */
public class HorseBondWithPlayerGoal extends Goal {

	private static final int TAMING_CHECK_INTERVAL = 50;
	private static final int TEMPER_INCREASE = 5;
	private static final byte ANGRY_STATUS = 6;

	private final AbstractHorseEntity horse;
	private final double speed;
	private double targetX;
	private double targetY;
	private double targetZ;

	public HorseBondWithPlayerGoal(AbstractHorseEntity horse, double speed) {
		this.horse = horse;
		this.speed = speed;
		setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (horse.isControlledByMob() || horse.isTame() || !horse.hasPassengers()) {
			return false;
		}

		Vec3d wanderPos = NoPenaltyTargeting.find(horse, 5, 4);
		if (wanderPos == null) {
			return false;
		}

		targetX = wanderPos.x;
		targetY = wanderPos.y;
		targetZ = wanderPos.z;
		return true;
	}

	@Override
	public void start() {
		horse.getNavigation().startMovingTo(targetX, targetY, targetZ, speed);
	}

	@Override
	public boolean shouldContinue() {
		return !horse.isTame() && !horse.getNavigation().isIdle() && horse.hasPassengers();
	}

	@Override
	public void tick() {
		if (horse.isTame() || horse.getRandom().nextInt(getTickCount(TAMING_CHECK_INTERVAL)) != 0) {
			return;
		}

		Entity passenger = horse.getFirstPassenger();
		if (passenger == null) {
			return;
		}

		if (passenger instanceof PlayerEntity player) {
			int temper = horse.getTemper();
			int maxTemper = horse.getMaxTemper();
			if (maxTemper > 0 && horse.getRandom().nextInt(maxTemper) < temper) {
				horse.bondWithPlayer(player);
				return;
			}

			horse.addTemper(TEMPER_INCREASE);
		}

		horse.removeAllPassengers();
		horse.playAngrySound();
		horse.getEntityWorld().sendEntityStatus(horse, ANGRY_STATUS);
	}
}
