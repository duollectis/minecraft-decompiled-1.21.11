package net.minecraft.entity.ai.goal;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Планировщик целей ИИ моба. Управляет набором {@link PrioritizedGoal},
 * выбирая активные цели по приоритету и совместимости контролей.
 * На каждый тик: сначала останавливает цели, потерявшие право на выполнение,
 * затем запускает новые подходящие цели, наконец тикает все активные.
 */
public class GoalSelector {

	private static final PrioritizedGoal REPLACEABLE_GOAL = new PrioritizedGoal(
			Integer.MAX_VALUE, new Goal() {
		@Override
		public boolean canStart() {
			return false;
		}
	}
	) {
		@Override
		public boolean isRunning() {
			return false;
		}
	};

	private final Map<Goal.Control, PrioritizedGoal> goalsByControl = new EnumMap<>(Goal.Control.class);
	private final Set<PrioritizedGoal> goals = new ObjectLinkedOpenHashSet();
	private final EnumSet<Goal.Control> disabledControls = EnumSet.noneOf(Goal.Control.class);

	public void add(int priority, Goal goal) {
		goals.add(new PrioritizedGoal(priority, goal));
	}

	public void clear(Predicate<Goal> predicate) {
		goals.removeIf(goal -> predicate.test(goal.getGoal()));
	}

	public void remove(Goal goal) {
		for (PrioritizedGoal prioritizedGoal : goals) {
			if (prioritizedGoal.getGoal() == goal && prioritizedGoal.isRunning()) {
				prioritizedGoal.stop();
			}
		}

		goals.removeIf(pg -> pg.getGoal() == goal);
	}

	private static boolean usesAny(PrioritizedGoal goal, EnumSet<Goal.Control> controls) {
		for (Goal.Control control : goal.getControls()) {
			if (controls.contains(control)) {
				return true;
			}
		}

		return false;
	}

	private static boolean canReplaceAll(PrioritizedGoal goal, Map<Goal.Control, PrioritizedGoal> goalsByControl) {
		for (Goal.Control control : goal.getControls()) {
			if (!goalsByControl.getOrDefault(control, REPLACEABLE_GOAL).canBeReplacedBy(goal)) {
				return false;
			}
		}

		return true;
	}

	public void tick() {
		Profiler profiler = Profilers.get();
		profiler.push("goalCleanup");

		for (PrioritizedGoal goal : goals) {
			if (goal.isRunning() && (usesAny(goal, disabledControls) || !goal.shouldContinue())) {
				goal.stop();
			}
		}

		goalsByControl.entrySet().removeIf(entry -> !entry.getValue().isRunning());
		profiler.pop();
		profiler.push("goalUpdate");

		for (PrioritizedGoal goal : goals) {
			if (!goal.isRunning()
					&& !usesAny(goal, disabledControls)
					&& canReplaceAll(goal, goalsByControl)
					&& goal.canStart()) {
				for (Goal.Control control : goal.getControls()) {
					goalsByControl.getOrDefault(control, REPLACEABLE_GOAL).stop();
					goalsByControl.put(control, goal);
				}

				goal.start();
			}
		}

		profiler.pop();
		tickGoals(true);
	}

	/**
	 * Тикает все активные цели. Если {@code tickAll = false} — только те,
	 * у которых {@link Goal#shouldRunEveryTick()} возвращает {@code true}.
	 */
	public void tickGoals(boolean tickAll) {
		Profiler profiler = Profilers.get();
		profiler.push("goalTick");

		for (PrioritizedGoal goal : goals) {
			if (goal.isRunning() && (tickAll || goal.shouldRunEveryTick())) {
				goal.tick();
			}
		}

		profiler.pop();
	}

	public Set<PrioritizedGoal> getGoals() {
		return goals;
	}

	public void disableControl(Goal.Control control) {
		disabledControls.add(control);
	}

	public void enableControl(Goal.Control control) {
		disabledControls.remove(control);
	}

	public void setControlEnabled(Goal.Control control, boolean enabled) {
		if (enabled) {
			enableControl(control);
		}
		else {
			disableControl(control);
		}
	}
}
