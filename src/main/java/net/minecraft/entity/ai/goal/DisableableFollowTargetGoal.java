package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.raid.RaiderEntity;
import org.jspecify.annotations.Nullable;

/**
 * Расширение {@link ActiveTargetGoal} с возможностью программного отключения
 * через {@link #setEnabled(boolean)}. Используется в рейдах для временной
 * приостановки преследования цели.
 */
public class DisableableFollowTargetGoal<T extends LivingEntity> extends ActiveTargetGoal<T> {

	private boolean enabled = true;

	public DisableableFollowTargetGoal(
		RaiderEntity actor,
		Class<T> targetEntityClass,
		int reciprocalChance,
		boolean checkVisibility,
		boolean checkCanNavigate,
		TargetPredicate.@Nullable EntityPredicate targetPredicate
	) {
		super(actor, targetEntityClass, reciprocalChance, checkVisibility, checkCanNavigate, targetPredicate);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean canStart() {
		return enabled && super.canStart();
	}
}
