package net.minecraft.entity.ai.brain;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Представляет тип активности мозга сущности.
 * Активности определяют, какой набор задач ({@link Brain}) выполняется в данный момент.
 * Каждая сущность с мозгом может иметь несколько одновременно активных активностей
 * (например, CORE всегда активна), но только одна не-ядровая активность является текущей.
 */
public class Activity {

	public static final Activity CORE = register("core");
	public static final Activity IDLE = register("idle");
	public static final Activity WORK = register("work");
	public static final Activity PLAY = register("play");
	public static final Activity REST = register("rest");
	public static final Activity MEET = register("meet");
	public static final Activity PANIC = register("panic");
	public static final Activity RAID = register("raid");
	public static final Activity PRE_RAID = register("pre_raid");
	public static final Activity HIDE = register("hide");
	public static final Activity FIGHT = register("fight");
	public static final Activity CELEBRATE = register("celebrate");
	public static final Activity ADMIRE_ITEM = register("admire_item");
	public static final Activity AVOID = register("avoid");
	public static final Activity RIDE = register("ride");
	public static final Activity PLAY_DEAD = register("play_dead");
	public static final Activity LONG_JUMP = register("long_jump");
	public static final Activity RAM = register("ram");
	public static final Activity TONGUE = register("tongue");
	public static final Activity SWIM = register("swim");
	public static final Activity LAY_SPAWN = register("lay_spawn");
	public static final Activity SNIFF = register("sniff");
	public static final Activity INVESTIGATE = register("investigate");
	public static final Activity ROAR = register("roar");
	public static final Activity EMERGE = register("emerge");
	public static final Activity DIG = register("dig");

	private final String id;
	private final int hashCode;

	public Activity(String id) {
		this.id = id;
		this.hashCode = id.hashCode();
	}

	public String getId() {
		return id;
	}

	private static Activity register(String id) {
		return Registry.register(Registries.ACTIVITY, id, new Activity(id));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Activity activity = (Activity) o;
		return id.equals(activity.id);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return getId();
	}
}
