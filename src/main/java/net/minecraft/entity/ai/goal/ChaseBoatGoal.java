package net.minecraft.entity.ai.goal;

import net.minecraft.entity.MovementType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Цель, заставляющая моба преследовать лодку с игроком: сначала подходит к лодке,
 * затем движется в направлении её движения, имитируя сопровождение.
 */
public class ChaseBoatGoal extends Goal {

	private static final float APPROACH_SPEED = 0.015F;
	private static final float FOLLOW_SPEED = 0.01F;
	private static final float NEAR_BOAT_DISTANCE = 4.0F;
	private static final float FAR_BOAT_DISTANCE = 12.0F;
	private static final double BOAT_SEARCH_RADIUS = 5.0;
	private static final int UPDATE_INTERVAL_TICKS = 10;
	private static final int AHEAD_OFFSET = 10;

	private int updateCountdownTicks;
	private final PathAwareEntity mob;
	private @Nullable PlayerEntity passenger;
	private ChaseBoatState state;

	public ChaseBoatGoal(PathAwareEntity mob) {
		this.mob = mob;
	}

	@Override
	public boolean canStart() {
		if (passenger != null && passenger.getVehicle() instanceof AbstractBoatEntity) {
			return true;
		}

		for (AbstractBoatEntity boat : mob.getEntityWorld()
			.getNonSpectatingEntities(AbstractBoatEntity.class, mob.getBoundingBox().expand(BOAT_SEARCH_RADIUS))
		) {
			if (boat.getControllingPassenger() instanceof PlayerEntity player
				&& player.getVehicle() instanceof AbstractBoatEntity
			) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean canStop() {
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return passenger != null
			&& passenger.hasVehicle()
			&& passenger.getVehicle() instanceof AbstractBoatEntity;
	}

	@Override
	public void start() {
		for (AbstractBoatEntity boat : mob.getEntityWorld()
			.getNonSpectatingEntities(AbstractBoatEntity.class, mob.getBoundingBox().expand(BOAT_SEARCH_RADIUS))
		) {
			if (boat.getControllingPassenger() instanceof PlayerEntity player) {
				passenger = player;
				break;
			}
		}

		updateCountdownTicks = 0;
		state = ChaseBoatState.GO_TO_BOAT;
	}

	@Override
	public void stop() {
		passenger = null;
	}

	@Override
	public void tick() {
		float speed = state == ChaseBoatState.GO_IN_BOAT_DIRECTION ? FOLLOW_SPEED : APPROACH_SPEED;
		mob.updateVelocity(speed, new Vec3d(mob.sidewaysSpeed, mob.upwardSpeed, mob.forwardSpeed));
		mob.move(MovementType.SELF, mob.getVelocity());

		if (--updateCountdownTicks > 0) {
			return;
		}

		updateCountdownTicks = getTickCount(UPDATE_INTERVAL_TICKS);

		if (state == ChaseBoatState.GO_TO_BOAT) {
			BlockPos target = passenger.getBlockPos()
				.offset(passenger.getHorizontalFacing().getOpposite())
				.add(0, -1, 0);
			mob.getNavigation().startMovingTo(target.getX(), target.getY(), target.getZ(), 1.0);

			if (mob.distanceTo(passenger) < NEAR_BOAT_DISTANCE) {
				updateCountdownTicks = 0;
				state = ChaseBoatState.GO_IN_BOAT_DIRECTION;
			}
		} else if (state == ChaseBoatState.GO_IN_BOAT_DIRECTION) {
			Direction direction = passenger.getMovementDirection();
			BlockPos ahead = passenger.getBlockPos().offset(direction, AHEAD_OFFSET);
			mob.getNavigation().startMovingTo(ahead.getX(), ahead.getY() - 1, ahead.getZ(), 1.0);

			if (mob.distanceTo(passenger) > FAR_BOAT_DISTANCE) {
				updateCountdownTicks = 0;
				state = ChaseBoatState.GO_TO_BOAT;
			}
		}
	}
}
