package net.minecraft.command.permission;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * {@code PermissionLevel}.
 */
public enum PermissionLevel implements StringIdentifiable {
	ALL("all", 0),
	MODERATORS("moderators", 1),
	GAMEMASTERS("gamemasters", 2),
	ADMINS("admins", 3),
	OWNERS("owners", 4);

	public static final Codec<PermissionLevel> CODEC = StringIdentifiable.createCodec(PermissionLevel::values);
	private static final IntFunction<PermissionLevel> BY_LEVEL = ValueLists.createIndexToValueFunction(
			(PermissionLevel level) -> level.level, values(), ValueLists.OutOfBoundsHandling.CLAMP
	);
	public static final Codec<PermissionLevel> NUMERIC_CODEC = Codec.INT.xmap(BY_LEVEL::apply, level -> level.level);
	private final String name;
	private final int level;

	private PermissionLevel(final String name, final int level) {
		this.name = name;
		this.level = level;
	}

	public boolean isAtLeast(PermissionLevel other) {
		return level >= other.level;
	}

	public static PermissionLevel fromLevel(int level) {
		return BY_LEVEL.apply(level);
	}

	public int getLevel() {
		return level;
	}

	@Override
	public String asString() {
		return name;
	}
}
