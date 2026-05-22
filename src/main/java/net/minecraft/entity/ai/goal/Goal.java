package net.minecraft.entity.ai.goal;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Базовый класс для всех целей (Goal) ИИ мобов.
 * Цели выполняются через {@link GoalSelector}, который управляет приоритетами
 * и контролями ({@link Control}). Каждая цель декларирует, какими контролями
 * она владеет, чтобы предотвратить конфликты между несовместимыми целями.
 */
public abstract class Goal {

	private final EnumSet<Goal.Control> controls = EnumSet.noneOf(Goal.Control.class);

	public abstract boolean canStart();

	public boolean shouldContinue() {
		return canStart();
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
		return getClass().getSimpleName();
	}

	public EnumSet<Goal.Control> getControls() {
		return controls;
	}

	protected int getTickCount(int ticks) {
		return shouldRunEveryTick() ? ticks : toGoalTicks(ticks);
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

	public enum Control {
		MOVE,
		LOOK,
		JUMP,
		TARGET;
	}
}
