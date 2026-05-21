package net.minecraft.entity.ai.goal;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * {@code Goal}.
 */
public abstract class Goal {

	private final EnumSet<Goal.Control> controls = EnumSet.noneOf(Goal.Control.class);

	public abstract boolean canStart();

	public boolean shouldContinue() {
		return this.canStart();
	}

	public boolean canStop() {
		return true;
	}

	public void start() {
	}

	public void stop() {
	}

	public boolean shouldRunEveryTick() {
		return false;
	}

	public void tick() {
	}

	public void setControls(EnumSet<Goal.Control> controls) {
		this.controls.clear();
		this.controls.addAll(controls);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	public EnumSet<Goal.Control> getControls() {
		return this.controls;
	}

	protected int getTickCount(int ticks) {
		return this.shouldRunEveryTick() ? ticks : toGoalTicks(ticks);
	}

	protected static int toGoalTicks(int serverTicks) {
		return MathHelper.ceilDiv(serverTicks, 2);
	}

	protected static ServerWorld getServerWorld(Entity entity) {
		return (ServerWorld) entity.getEntityWorld();
	}

	protected static ServerWorld castToServerWorld(World world) {
		return (ServerWorld) world;
	}

	/**
	 * {@code Control}.
	 */
	public static enum Control {
		MOVE,
		LOOK,
		JUMP,
		TARGET;
	}
}
