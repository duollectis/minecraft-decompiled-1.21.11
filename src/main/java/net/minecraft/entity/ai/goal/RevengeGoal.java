package net.minecraft.entity.ai.goal;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Цель мести: атакует существо, которое последним ударило моба.
 * Поддерживает групповую месть — оповещает соседних мобов того же типа.
 */
public class RevengeGoal extends TrackTargetGoal {

	private static final TargetPredicate VALID_AVOIDABLES_PREDICATE =
			TargetPredicate.createAttackable().ignoreVisibility().ignoreDistanceScalingFactor();
	private static final int BOX_VERTICAL_EXPANSION = 10;
	private static final int MAX_VISIBILITY_WITHOUT_SIGHT = 300;

	private boolean groupRevenge;
	private int lastAttackedTime;
	private final Class<?>[] noRevengeTypes;
	private Class<?> @Nullable [] noHelpTypes;

	public RevengeGoal(PathAwareEntity mob, Class<?>... noRevengeTypes) {
		super(mob, true);
		this.noRevengeTypes = noRevengeTypes;
		setControls(EnumSet.of(Goal.Control.TARGET));
	}

	@Override
	public boolean canStart() {
		int attackedTime = mob.getLastAttackedTime();
		LivingEntity attacker = mob.getAttacker();

		if (attackedTime == lastAttackedTime || attacker == null) {
			return false;
		}

		if (attacker.getType() == EntityType.PLAYER
				&& getServerWorld(mob).getGameRules().getValue(GameRules.UNIVERSAL_ANGER)) {
			return false;
		}

		for (Class<?> excluded : noRevengeTypes) {
			if (excluded.isAssignableFrom(attacker.getClass())) {
				return false;
			}
		}

		return canTrack(attacker, VALID_AVOIDABLES_PREDICATE);
	}

	public RevengeGoal setGroupRevenge(Class<?>... noHelpTypes) {
		groupRevenge = true;
		this.noHelpTypes = noHelpTypes;
		return this;
	}

	@Override
	public void start() {
		mob.setTarget(mob.getAttacker());
		target = mob.getTarget();
		lastAttackedTime = mob.getLastAttackedTime();
		maxTimeWithoutVisibility = MAX_VISIBILITY_WITHOUT_SIGHT;

		if (groupRevenge) {
			callSameTypeForRevenge();
		}

		super.start();
	}

	protected void callSameTypeForRevenge() {
		double followRange = getFollowRange();
		Box searchBox = Box.from(mob.getEntityPos()).expand(followRange, BOX_VERTICAL_EXPANSION, followRange);

		@SuppressWarnings("unchecked")
		List<? extends MobEntity> nearby = mob.getEntityWorld().getEntitiesByClass(
				(Class<? extends MobEntity>) mob.getClass(),
				searchBox,
				EntityPredicates.EXCEPT_SPECTATOR
		);

		LivingEntity attacker = mob.getAttacker();

		for (MobEntity ally : nearby) {
			if (ally == mob || ally.getTarget() != null || ally.isTeammate(attacker)) {
				continue;
			}

			if (mob instanceof TameableEntity thisTameable
					&& ally instanceof TameableEntity allyTameable
					&& thisTameable.getOwner() != allyTameable.getOwner()) {
				continue;
			}

			if (noHelpTypes != null && isExcludedFromHelp(ally)) {
				continue;
			}

			setMobEntityTarget(ally, attacker);
		}
	}

	private boolean isExcludedFromHelp(MobEntity ally) {
		for (Class<?> excluded : noHelpTypes) {
			if (ally.getClass() == excluded) {
				return true;
			}
		}

		return false;
	}

	protected void setMobEntityTarget(MobEntity ally, LivingEntity target) {
		ally.setTarget(target);
	}
}
