package net.minecraft.entity.ai.goal;

import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Цель спаривания животных: ищет ближайшего партнёра в радиусе 8 блоков
 * и инициирует размножение при достаточном сближении.
 */
public class AnimalMateGoal extends Goal {

	private static final TargetPredicate VALID_MATE_PREDICATE =
			TargetPredicate.createNonAttackable().setBaseMaxDistance(8.0).ignoreVisibility();
	private static final double MATE_SEARCH_RADIUS = 8.0;
	private static final double BREED_DISTANCE_SQ = 9.0;
	private static final int BREED_TIMER_TICKS = 60;

	protected final AnimalEntity animal;
	private final Class<? extends AnimalEntity> entityClass;
	protected final ServerWorld world;
	protected @Nullable AnimalEntity mate;
	private int timer;
	private final double speed;

	public AnimalMateGoal(AnimalEntity animal, double speed) {
		this(animal, speed, (Class<? extends AnimalEntity>) animal.getClass());
	}

	public AnimalMateGoal(AnimalEntity animal, double speed, Class<? extends AnimalEntity> entityClass) {
		this.animal = animal;
		this.world = getServerWorld(animal);
		this.entityClass = entityClass;
		this.speed = speed;
		setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (!animal.isInLove()) {
			return false;
		}

		mate = findMate();
		return mate != null;
	}

	@Override
	public boolean shouldContinue() {
		return mate.isAlive() && mate.isInLove() && timer < BREED_TIMER_TICKS && !mate.isPanicking();
	}

	@Override
	public void stop() {
		mate = null;
		timer = 0;
	}

	@Override
	public void tick() {
		animal.getLookControl().lookAt(mate, 10.0F, animal.getMaxLookPitchChange());
		animal.getNavigation().startMovingTo(mate, speed);
		timer++;
		if (timer >= getTickCount(BREED_TIMER_TICKS) && animal.squaredDistanceTo(mate) < BREED_DISTANCE_SQ) {
			breed();
		}
	}

	private @Nullable AnimalEntity findMate() {
		List<? extends AnimalEntity> candidates = world.getTargets(
				entityClass,
				VALID_MATE_PREDICATE,
				animal,
				animal.getBoundingBox().expand(MATE_SEARCH_RADIUS)
		);
		double closestDistSq = Double.MAX_VALUE;
		AnimalEntity closest = null;

		for (AnimalEntity candidate : candidates) {
			double distSq = animal.squaredDistanceTo(candidate);
			if (animal.canBreedWith(candidate) && !candidate.isPanicking() && distSq < closestDistSq) {
				closest = candidate;
				closestDistSq = distSq;
			}
		}

		return closest;
	}

	protected void breed() {
		animal.breed(world, mate);
	}
}
