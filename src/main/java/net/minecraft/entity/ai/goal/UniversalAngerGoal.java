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
 * Цель универсального гнева: при включённом правиле {@code UNIVERSAL_ANGER} атакует
 * любого игрока, ударившего моба, и опционально оповещает соседних мобов того же типа.
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
		return getServerWorld(mob).getGameRules().getValue(GameRules.UNIVERSAL_ANGER)
				&& canStartUniversalAnger();
	}

	private boolean canStartUniversalAnger() {
		return mob.getAttacker() != null
				&& mob.getAttacker().getType() == EntityType.PLAYER
				&& mob.getLastAttackedTime() > lastAttackedTime;
	}

	@Override
	public void start() {
		lastAttackedTime = mob.getLastAttackedTime();
		mob.universallyAnger();

		if (triggerOthers) {
			getOthersInRange()
					.stream()
					.filter(entity -> entity != mob)
					.map(entity -> (Angerable) entity)
					.forEach(Angerable::universallyAnger);
		}

		super.start();
	}

	private List<? extends MobEntity> getOthersInRange() {
		double followRange = mob.getAttributeValue(EntityAttributes.FOLLOW_RANGE);
		Box box = Box.from(mob.getEntityPos()).expand(followRange, BOX_VERTICAL_EXPANSION, followRange);

		return mob
				.getEntityWorld()
				.getEntitiesByClass(
						(Class<? extends MobEntity>) mob.getClass(),
						box,
						EntityPredicates.EXCEPT_SPECTATOR
				);
	}
}
