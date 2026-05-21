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

	/**
	 * Проверяет возможность start.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public abstract boolean canStart();

	/**
	 * Определяет, следует ли continue.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldContinue() {
		return this.canStart();
	}

	/**
	 * Проверяет возможность stop.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canStop() {
		return true;
	}

	/**
	 * Start.
	 */
	public void start() {
	}

	/**
	 * Stop.
	 */
	public void stop() {
	}

	/**
	 * Определяет, следует ли run every tick.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldRunEveryTick() {
		return false;
	}

	/**
	 * Tick.
	 */
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

	/**
	 * To goal ticks.
	 *
	 * @param serverTicks server ticks
	 *
	 * @return int — результат операции
	 */
	protected static int toGoalTicks(int serverTicks) {
		return MathHelper.ceilDiv(serverTicks, 2);
	}

	protected static ServerWorld getServerWorld(Entity entity) {
		return (ServerWorld) entity.getEntityWorld();
	}

	/**
	 * Cast to server world.
	 *
	 * @param world world
	 *
	 * @return ServerWorld — результат операции
	 */
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
