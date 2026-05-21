package net.minecraft.entity.ai.goal;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.minecraft.world.rule.GameRules;

import java.util.List;

/**
 * {@code UniversalAngerGoal}.
 */
public class UniversalAngerGoal<T extends MobEntity & Angerable> extends Goal {

	private static final int BOX_VERTICAL_EXPANSION = 10;
	private final T mob;
	private final boolean triggerOthers;
	private int lastAttackedTime;

	public UniversalAngerGoal(T mob, boolean triggerOthers) {
		this.mob = mob;
		this.triggerOthers = triggerOthers;
	}

	@Override
	public boolean canStart() {
		return getServerWorld(this.mob).getGameRules().getValue(GameRules.UNIVERSAL_ANGER)
				&& this.canStartUniversalAnger();
	}

	private boolean canStartUniversalAnger() {
		return this.mob.getAttacker() != null && this.mob.getAttacker().getType() == EntityType.PLAYER
				&& this.mob.getLastAttackedTime() > this.lastAttackedTime;
	}

	@Override
	public void start() {
		this.lastAttackedTime = this.mob.getLastAttackedTime();
		this.mob.universallyAnger();
		if (this.triggerOthers) {
			this
					.getOthersInRange()
					.stream()
					.filter(entity -> entity != this.mob)
					.map(entity -> (Angerable) entity)
					.forEach(Angerable::universallyAnger);
		}

		super.start();
	}

	private List<? extends MobEntity> getOthersInRange() {
		double d = this.mob.getAttributeValue(EntityAttributes.FOLLOW_RANGE);
		Box box = Box.from(this.mob.getEntityPos()).expand(d, 10.0, d);
		return this.mob
				.getEntityWorld()
				.getEntitiesByClass(
						(Class<? extends MobEntity>) this.mob.getClass(),
						box,
						EntityPredicates.EXCEPT_SPECTATOR
				);
	}
}
