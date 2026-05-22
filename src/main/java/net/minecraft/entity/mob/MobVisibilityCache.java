package net.minecraft.entity.mob;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.entity.Entity;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

/**
 * Кэш видимости мобов для оптимизации проверок линии видимости.
 */
public class MobVisibilityCache {

	private final MobEntity owner;
	private final IntSet visibleEntities = new IntOpenHashSet();
	private final IntSet invisibleEntities = new IntOpenHashSet();

	public MobVisibilityCache(MobEntity owner) {
		this.owner = owner;
	}

	public void clear() {
		visibleEntities.clear();
		invisibleEntities.clear();
	}

	public boolean canSee(Entity entity) {
		int entityId = entity.getId();

		if (visibleEntities.contains(entityId)) {
			return true;
		}

		if (invisibleEntities.contains(entityId)) {
			return false;
		}

		Profiler profiler = Profilers.get();
		profiler.push("hasLineOfSight");
		boolean visible = owner.canSee(entity);
		profiler.pop();

		if (visible) {
			visibleEntities.add(entityId);
		} else {
			invisibleEntities.add(entityId);
		}

		return visible;
	}
}
