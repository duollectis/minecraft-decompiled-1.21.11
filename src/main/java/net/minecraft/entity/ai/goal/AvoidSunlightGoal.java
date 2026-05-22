package net.minecraft.entity.ai.goal;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.mob.PathAwareEntity;

/**
 * Цель, заставляющая моба избегать прямого солнечного света, перестраивая маршрут
 * через {@link MobNavigation#setAvoidSunlight(boolean)}. Активируется только днём,
 * если голова моба не защищена шлемом.
 */
public class AvoidSunlightGoal extends Goal {

	private final PathAwareEntity mob;

	public AvoidSunlightGoal(PathAwareEntity mob) {
		this.mob = mob;
	}

	@Override
	public boolean canStart() {
		return mob.getEntityWorld().isDay()
			&& mob.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
			&& NavigationConditions.hasMobNavigation(mob);
	}

	@Override
	public void start() {
		if (mob.getNavigation() instanceof MobNavigation mobNavigation) {
			mobNavigation.setAvoidSunlight(true);
		}
	}

	@Override
	public void stop() {
		if (NavigationConditions.hasMobNavigation(mob)
			&& mob.getNavigation() instanceof MobNavigation mobNavigation
		) {
			mobNavigation.setAvoidSunlight(false);
		}
	}
}
