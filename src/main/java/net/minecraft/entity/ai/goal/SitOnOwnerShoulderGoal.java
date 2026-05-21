package net.minecraft.entity.ai.goal;

import net.minecraft.entity.passive.TameableShoulderEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * {@code SitOnOwnerShoulderGoal}.
 */
public class SitOnOwnerShoulderGoal extends Goal {

	private final TameableShoulderEntity tameable;
	private boolean mounted;

	public SitOnOwnerShoulderGoal(TameableShoulderEntity tameable) {
		this.tameable = tameable;
	}

	@Override
	public boolean canStart() {
		if (!(this.tameable.getOwner() instanceof ServerPlayerEntity serverPlayerEntity)) {
			return false;
		}
		else {
			boolean bl = !serverPlayerEntity.isSpectator()
					&& !serverPlayerEntity.getAbilities().flying
					&& !serverPlayerEntity.isTouchingWater()
					&& !serverPlayerEntity.inPowderSnow;
			return !this.tameable.isSitting() && bl && this.tameable.isReadyToSitOnPlayer();
		}
	}

	@Override
	public boolean canStop() {
		return !this.mounted;
	}

	@Override
	public void start() {
		this.mounted = false;
	}

	@Override
	public void tick() {
		if (!this.mounted && !this.tameable.isInSittingPose() && !this.tameable.isLeashed()) {
			if (this.tameable.getOwner() instanceof ServerPlayerEntity serverPlayerEntity
					&& this.tameable.getBoundingBox().intersects(serverPlayerEntity.getBoundingBox())) {
				this.mounted = this.tameable.mountOnto(serverPlayerEntity);
			}
		}
	}
}
