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
 * {@code ChaseBoatGoal}.
 */
public class ChaseBoatGoal extends Goal {

	private int updateCountdownTicks;
	private final PathAwareEntity mob;
	private @Nullable PlayerEntity passenger;
	private ChaseBoatState state;

	public ChaseBoatGoal(PathAwareEntity mob) {
		this.mob = mob;
	}

	@Override
	public boolean canStart() {
		if (this.passenger != null && this.passenger.getVehicle() instanceof AbstractBoatEntity) {
			return true;
		}
		else {
			for (AbstractBoatEntity abstractBoatEntity : this.mob
					.getEntityWorld()
					.getNonSpectatingEntities(AbstractBoatEntity.class, this.mob.getBoundingBox().expand(5.0))) {
				if (abstractBoatEntity.getControllingPassenger() instanceof PlayerEntity playerEntity
						&& playerEntity.getVehicle() instanceof AbstractBoatEntity) {
					return true;
				}
			}

			return false;
		}
	}

	@Override
	public boolean canStop() {
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return this.passenger != null && this.passenger.hasVehicle() && this.passenger.getVehicle() instanceof AbstractBoatEntity;
	}

	@Override
	public void start() {
		for (AbstractBoatEntity abstractBoatEntity : this.mob
				.getEntityWorld()
				.getNonSpectatingEntities(AbstractBoatEntity.class, this.mob.getBoundingBox().expand(5.0))) {
			if (abstractBoatEntity.getControllingPassenger() instanceof PlayerEntity playerEntity) {
				this.passenger = playerEntity;
				break;
			}
		}

		this.updateCountdownTicks = 0;
		this.state = ChaseBoatState.GO_TO_BOAT;
	}

	@Override
	public void stop() {
		this.passenger = null;
	}

	@Override
	public void tick() {
		float f = this.state == ChaseBoatState.GO_IN_BOAT_DIRECTION ? 0.01F : 0.015F;
		this.mob.updateVelocity(f, new Vec3d(this.mob.sidewaysSpeed, this.mob.upwardSpeed, this.mob.forwardSpeed));
		this.mob.move(MovementType.SELF, this.mob.getVelocity());
		if (--this.updateCountdownTicks <= 0) {
			this.updateCountdownTicks = this.getTickCount(10);
			if (this.state == ChaseBoatState.GO_TO_BOAT) {
				BlockPos
						blockPos =
						this.passenger.getBlockPos().offset(this.passenger.getHorizontalFacing().getOpposite());
				blockPos = blockPos.add(0, -1, 0);
				this.mob.getNavigation().startMovingTo(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1.0);
				if (this.mob.distanceTo(this.passenger) < 4.0F) {
					this.updateCountdownTicks = 0;
					this.state = ChaseBoatState.GO_IN_BOAT_DIRECTION;
				}
			}
			else if (this.state == ChaseBoatState.GO_IN_BOAT_DIRECTION) {
				Direction direction = this.passenger.getMovementDirection();
				BlockPos blockPos2 = this.passenger.getBlockPos().offset(direction, 10);
				this.mob.getNavigation().startMovingTo(blockPos2.getX(), blockPos2.getY() - 1, blockPos2.getZ(), 1.0);
				if (this.mob.distanceTo(this.passenger) > 12.0F) {
					this.updateCountdownTicks = 0;
					this.state = ChaseBoatState.GO_TO_BOAT;
				}
			}
		}
	}
}
