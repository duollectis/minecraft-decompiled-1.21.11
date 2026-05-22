package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.passive.TameableEntity;

import java.util.EnumSet;

/**
 * Цель прирученного существа, атакующего ту же цель, что и его владелец.
 * Синхронизируется с {@code lastAttackTime} владельца, чтобы не реагировать на старые атаки.
 */
public class AttackWithOwnerGoal extends TrackTargetGoal {

	private final TameableEntity tameable;
	private LivingEntity attacking;
	private int lastAttackTime;

	public AttackWithOwnerGoal(TameableEntity tameable) {
		super(tameable, false);
		this.tameable = tameable;
		setControls(EnumSet.of(Goal.Control.TARGET));
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

		attacking = owner.getAttacking();
		int attackTime = owner.getLastAttackTime();
		return attackTime != lastAttackTime
				&& canTrack(attacking, TargetPredicate.DEFAULT)
				&& tameable.canAttackWithOwner(attacking, owner);
	}

	@Override
	public void start() {
		mob.setTarget(attacking);
		LivingEntity owner = tameable.getOwner();
		if (owner != null) {
			lastAttackTime = owner.getLastAttackTime();
		}

		super.start();
	}
}
