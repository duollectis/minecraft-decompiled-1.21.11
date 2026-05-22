package net.minecraft.entity.ai.goal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

/**
 * Цель формирования каравана лам: ищет ближайшую ламу-лидера (привязанную или
 * уже следующую за кем-то) и пристраивается в хвост колонны длиной до
 * {@link #MAX_CARAVAN_LENGTH}. При слишком большом отставании временно ускоряется.
 */
public class FormCaravanGoal extends Goal {

	private static final double SEARCH_RADIUS_XZ = 9.0;
	private static final double SEARCH_RADIUS_Y = 4.0;
	private static final double MIN_JOIN_DISTANCE_SQ = 4.0;
	private static final double MAX_DISTANCE_SQ_BEFORE_SPEED_UP = 676.0;
	private static final double MAX_SPEED = 3.0;
	private static final double SPEED_MULTIPLIER = 1.2;
	private static final double DEFAULT_SPEED = 2.1;
	private static final int SPEED_UP_DURATION_TICKS = 40;
	private static final int MAX_CARAVAN_LENGTH = 8;
	private static final float FOLLOW_GAP = 2.0F;

	public final LlamaEntity llama;
	private double speed;
	private int counter;

	public FormCaravanGoal(LlamaEntity llama, double speed) {
		this.llama = llama;
		this.speed = speed;
		this.setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (llama.isLeashed() || llama.isFollowing()) {
			return false;
		}

		List<Entity> nearby = llama.getEntityWorld().getOtherEntities(
			llama,
			llama.getBoundingBox().expand(SEARCH_RADIUS_XZ, SEARCH_RADIUS_Y, SEARCH_RADIUS_XZ),
			entity -> {
				EntityType<?> type = entity.getType();
				return type == EntityType.LLAMA || type == EntityType.TRADER_LLAMA;
			}
		);

		LlamaEntity leader = findLeader(nearby, true);

		if (leader == null) {
			leader = findLeader(nearby, false);
		}

		if (leader == null) {
			return false;
		}

		if (llama.squaredDistanceTo(leader) < MIN_JOIN_DISTANCE_SQ) {
			return false;
		}

		if (!leader.isLeashed() && !canFollow(leader, 1)) {
			return false;
		}

		llama.follow(leader);
		return true;
	}

	/** Ищет ближайшую ламу без последователя: если {@code preferFollowing} — среди следующих, иначе среди привязанных. */
	private LlamaEntity findLeader(List<Entity> candidates, boolean preferFollowing) {
		LlamaEntity closest = null;
		double closestDistSq = Double.MAX_VALUE;

		for (Entity entity : candidates) {
			LlamaEntity candidate = (LlamaEntity) entity;
			boolean qualifies = preferFollowing
				? candidate.isFollowing() && !candidate.hasFollower()
				: candidate.isLeashed() && !candidate.hasFollower();

			if (!qualifies) {
				continue;
			}

			double distSq = llama.squaredDistanceTo(candidate);

			if (distSq <= closestDistSq) {
				closestDistSq = distSq;
				closest = candidate;
			}
		}

		return closest;
	}

	@Override
	public boolean shouldContinue() {
		if (!llama.isFollowing() || !llama.getFollowing().isAlive() || !canFollow(llama, 0)) {
			return false;
		}

		double distSq = llama.squaredDistanceTo(llama.getFollowing());

		if (distSq > MAX_DISTANCE_SQ_BEFORE_SPEED_UP) {
			if (speed <= MAX_SPEED) {
				speed *= SPEED_MULTIPLIER;
				counter = toGoalTicks(SPEED_UP_DURATION_TICKS);
				return true;
			}

			if (counter == 0) {
				return false;
			}
		}

		if (counter > 0) {
			counter--;
		}

		return true;
	}

	@Override
	public void stop() {
		llama.stopFollowing();
		speed = DEFAULT_SPEED;
	}

	@Override
	public void tick() {
		if (!llama.isFollowing()) {
			return;
		}

		if (llama.getLeashHolder() instanceof LeashKnotEntity) {
			return;
		}

		LlamaEntity leader = llama.getFollowing();
		double dist = llama.distanceTo(leader);
		Vec3d direction = new Vec3d(
			leader.getX() - llama.getX(),
			leader.getY() - llama.getY(),
			leader.getZ() - llama.getZ()
		)
			.normalize()
			.multiply(Math.max(dist - FOLLOW_GAP, 0.0));

		llama.getNavigation().startMovingTo(
			llama.getX() + direction.x,
			llama.getY() + direction.y,
			llama.getZ() + direction.z,
			speed
		);
	}

	private boolean canFollow(LlamaEntity target, int length) {
		if (length > MAX_CARAVAN_LENGTH) {
			return false;
		}

		if (!target.isFollowing()) {
			return false;
		}

		return target.getFollowing().isLeashed() || canFollow(target.getFollowing(), length + 1);
	}
}
