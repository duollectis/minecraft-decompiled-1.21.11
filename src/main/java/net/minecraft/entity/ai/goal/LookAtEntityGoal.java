package net.minecraft.entity.ai.goal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

/** Цель взгляда на ближайшую сущность заданного типа. */
public class LookAtEntityGoal extends Goal {

	public static final float DEFAULT_CHANCE = 0.02F;
	private static final int LOOK_TIME_BASE = 40;
	private static final int LOOK_TIME_JITTER = 40;
	private static final double LOOK_SEARCH_HEIGHT = 3.0;

	protected final MobEntity mob;
	protected @Nullable Entity target;
	protected final float range;
	private int lookTime;
	protected final float chance;
	private final boolean lookForward;
	protected final Class<? extends LivingEntity> targetType;
	protected final TargetPredicate targetPredicate;

	public LookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range) {
		this(mob, targetType, range, DEFAULT_CHANCE);
	}

	public LookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range, float chance) {
		this(mob, targetType, range, chance, false);
	}

	public LookAtEntityGoal(
			MobEntity mob,
			Class<? extends LivingEntity> targetType,
			float range,
			float chance,
			boolean lookForward
	) {
		this.mob = mob;
		this.targetType = targetType;
		this.range = range;
		this.chance = chance;
		this.lookForward = lookForward;
		setControls(EnumSet.of(Goal.Control.LOOK));

		if (targetType == PlayerEntity.class) {
			Predicate<Entity> ridePredicate = EntityPredicates.rides(mob);
			this.targetPredicate = TargetPredicate.createNonAttackable()
					.setBaseMaxDistance(range)
					.setPredicate((entity, world) -> ridePredicate.test(entity));
		}
		else {
			this.targetPredicate = TargetPredicate.createNonAttackable().setBaseMaxDistance(range);
		}
	}

	@Override
	public boolean canStart() {
		if (mob.getRandom().nextFloat() >= chance) {
			return false;
		}

		if (mob.getTarget() != null) {
			target = mob.getTarget();
		}

		ServerWorld serverWorld = getServerWorld(mob);

		if (targetType == PlayerEntity.class) {
			target = serverWorld.getClosestPlayer(targetPredicate, mob, mob.getX(), mob.getEyeY(), mob.getZ());
		}
		else {
			target = serverWorld.getClosestEntity(
					mob.getEntityWorld().getEntitiesByClass(
							targetType,
							mob.getBoundingBox().expand(range, LOOK_SEARCH_HEIGHT, range),
							livingEntity -> true
					),
					targetPredicate,
					mob,
					mob.getX(),
					mob.getEyeY(),
					mob.getZ()
			);
		}

		return target != null;
	}

	@Override
	public boolean shouldContinue() {
		if (!target.isAlive()) {
			return false;
		}

		if (mob.squaredDistanceTo(target) > range * range) {
			return false;
		}

		return lookTime > 0;
	}

	@Override
	public void start() {
		lookTime = getTickCount(LOOK_TIME_BASE + mob.getRandom().nextInt(LOOK_TIME_JITTER));
	}

	@Override
	public void stop() {
		target = null;
	}

	@Override
	public void tick() {
		if (!target.isAlive()) {
			return;
		}

		double lookY = lookForward ? mob.getEyeY() : target.getEyeY();
		mob.getLookControl().lookAt(target.getX(), lookY, target.getZ());
		lookTime--;
	}
}
