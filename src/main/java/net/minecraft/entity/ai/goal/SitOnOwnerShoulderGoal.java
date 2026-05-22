package net.minecraft.entity.ai.goal;

import net.minecraft.entity.passive.TameableShoulderEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Цель посадки прирученного существа на плечо владельца: проверяет,
 * что игрок не летит, не в воде и не в порошковом снегу, затем
 * ждёт пересечения bounding box для вызова {@link TameableShoulderEntity#mountOnto}.
 */
public class SitOnOwnerShoulderGoal extends Goal {

	private final TameableShoulderEntity tameable;
	private boolean mounted;

	public SitOnOwnerShoulderGoal(TameableShoulderEntity tameable) {
		this.tameable = tameable;
	}

	@Override
	public boolean canStart() {
		if (!(tameable.getOwner() instanceof ServerPlayerEntity owner)) {
			return false;
		}

		boolean ownerAccessible = !owner.isSpectator()
			&& !owner.getAbilities().flying
			&& !owner.isTouchingWater()
			&& !owner.inPowderSnow;

		return !tameable.isSitting() && ownerAccessible && tameable.isReadyToSitOnPlayer();
	}

	@Override
	public boolean canStop() {
		return !mounted;
	}

	@Override
	public void start() {
		mounted = false;
	}

	@Override
	public void tick() {
		if (mounted || tameable.isInSittingPose() || tameable.isLeashed()) {
			return;
		}

		if (tameable.getOwner() instanceof ServerPlayerEntity owner
			&& tameable.getBoundingBox().intersects(owner.getBoundingBox())
		) {
			mounted = tameable.mountOnto(owner);
		}
	}
}
