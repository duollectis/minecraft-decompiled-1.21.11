package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.CopperGolemEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.math.Box;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Цель железного голема: раз в некоторое время смотрит на ближайшего жителя
 * и при завершении дарит ему мак, если тот принимает подарки от голема.
 */
public class IronGolemLookGoal extends Goal {

	private static final TargetPredicate CLOSE_VILLAGER_PREDICATE =
		TargetPredicate.createNonAttackable().setBaseMaxDistance(6.0);
	private static final Item GIFT = Items.POPPY;
	private static final int LOOK_CHANCE = 8000;
	private static final float LOOK_ANGLE = 30.0F;
	private static final double GIFT_RANGE_XZ = 6.0;
	private static final double GIFT_RANGE_Y = 2.0;

	public static final int MAX_LOOK_COOLDOWN = 400;

	private final IronGolemEntity golem;
	private @Nullable LivingEntity recipient;
	private int lookCountdown;

	public IronGolemLookGoal(IronGolemEntity golem) {
		this.golem = golem;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (!golem.getEntityWorld().isDay()) {
			return false;
		}

		if (golem.getRandom().nextInt(LOOK_CHANCE) != 0) {
			return false;
		}

		recipient = getServerWorld(golem).getClosestEntity(
			EntityTypeTags.CANDIDATE_FOR_IRON_GOLEM_GIFT,
			CLOSE_VILLAGER_PREDICATE,
			golem,
			golem.getX(),
			golem.getY(),
			golem.getZ(),
			getRange()
		);

		return recipient != null;
	}

	@Override
	public boolean shouldContinue() {
		return lookCountdown > 0;
	}

	@Override
	public void start() {
		lookCountdown = getTickCount(MAX_LOOK_COOLDOWN);
		golem.setLookingAtVillager(true);
	}

	@Override
	public void stop() {
		golem.setLookingAtVillager(false);

		if (lookCountdown == 0
			&& recipient instanceof MobEntity mobEntity
			&& mobEntity.getType().isIn(EntityTypeTags.ACCEPTS_IRON_GOLEM_GIFT)
			&& mobEntity.getEquippedStack(CopperGolemEntity.POPPY_SLOT).isEmpty()
			&& getRange().intersects(mobEntity.getBoundingBox())
		) {
			mobEntity.equipStack(CopperGolemEntity.POPPY_SLOT, GIFT.getDefaultStack());
			mobEntity.setDropGuaranteed(CopperGolemEntity.POPPY_SLOT);
		}

		recipient = null;
	}

	@Override
	public void tick() {
		if (recipient != null) {
			golem.getLookControl().lookAt(recipient, LOOK_ANGLE, LOOK_ANGLE);
		}

		lookCountdown--;
	}

	private Box getRange() {
		return golem.getBoundingBox().expand(GIFT_RANGE_XZ, GIFT_RANGE_Y, GIFT_RANGE_XZ);
	}
}
