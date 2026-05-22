package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.passive.TameableEntity;

import java.util.EnumSet;

/**
 * Цель прирученного существа: атаковать того, кто ударил его владельца,
 * если существо не сидит и может атаковать нападавшего вместе с хозяином.
 */
public class TrackOwnerAttackerGoal extends TrackTargetGoal {

	private final TameableEntity tameable;
	private LivingEntity attacker;
	private int lastAttackedTime;

	public TrackOwnerAttackerGoal(TameableEntity tameable) {
		super(tameable, false);
		this.tameable = tameable;
		this.setControls(EnumSet.of(Goal.Control.TARGET));
	}

	@Override
	public boolean canStart() {
		if (!tameable.isTamed() || tameable.isSitting()) {
			return false;
		}

		LivingEntity owner = tameable.getOwner();

		if (owner == null) {
			return false;
		}

		attacker = owner.getAttacker();
		int attackTime = owner.getLastAttackedTime();
		return attackTime != lastAttackedTime
			&& canTrack(attacker, TargetPredicate.DEFAULT)
			&& tameable.canAttackWithOwner(attacker, owner);
	}

	@Override
	public void start() {
		mob.setTarget(attacker);
		LivingEntity owner = tameable.getOwner();

		if (owner != null) {
			lastAttackedTime = owner.getLastAttackedTime();
		}

		super.start();
	}
}
