package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.raid.RaiderEntity;
import org.jspecify.annotations.Nullable;

/**
 * Цель атаки рейдера: с задержкой {@link #MAX_COOLDOWN} тиков ищет ближайшую
 * цель, если рейдер участвует в активном рейде.
 */
public class RaidGoal<T extends LivingEntity> extends ActiveTargetGoal<T> {

	private static final int MAX_COOLDOWN = 200;

	private int cooldown = 0;

	public RaidGoal(
		RaiderEntity raider,
		Class<T> targetEntityClass,
		boolean checkVisibility,
		TargetPredicate.@Nullable EntityPredicate targetPredicate
	) {
		super(raider, targetEntityClass, 500, checkVisibility, false, targetPredicate);
	}

	public int getCooldown() {
		return cooldown;
	}

	public void decreaseCooldown() {
		cooldown--;
	}

	@Override
	public boolean canStart() {
		if (cooldown > 0 || !mob.getRandom().nextBoolean()) {
			return false;
		}

		if (!((RaiderEntity) mob).hasActiveRaid()) {
			return false;
		}

		findClosestTarget();
		return targetEntity != null;
	}

	@Override
	public void start() {
		cooldown = toGoalTicks(MAX_COOLDOWN);
		super.start();
	}
}
