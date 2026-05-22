package net.minecraft.command.permission;

import net.minecraft.command.DefaultPermissions;

/**
 * Предикат разрешений на основе уровня доступа {@link PermissionLevel}.
 * Разрешение считается выданным, если уровень источника не ниже требуемого.
 */
public interface LeveledPermissionPredicate extends PermissionPredicate {

	@Deprecated
	LeveledPermissionPredicate ALL = create(PermissionLevel.ALL);

	LeveledPermissionPredicate MODERATORS = create(PermissionLevel.MODERATORS);

	LeveledPermissionPredicate GAMEMASTERS = create(PermissionLevel.GAMEMASTERS);

	LeveledPermissionPredicate ADMINS = create(PermissionLevel.ADMINS);

	LeveledPermissionPredicate OWNERS = create(PermissionLevel.OWNERS);

	PermissionLevel getLevel();

	@Override
	default boolean hasPermission(Permission permission) {
		if (permission instanceof Permission.Level level) {
			return getLevel().isAtLeast(level.level());
		}

		// Атомарное разрешение на использование @-селекторов требует уровня GAMEMASTERS
		return permission.equals(DefaultPermissions.ENTITY_SELECTORS)
				&& getLevel().isAtLeast(PermissionLevel.GAMEMASTERS);
	}

	@Override
	default PermissionPredicate or(PermissionPredicate other) {
		if (other instanceof LeveledPermissionPredicate otherLeveled) {
			// Возвращаем предикат с меньшим уровнем — он покрывает больше источников
			return getLevel().isAtLeast(otherLeveled.getLevel()) ? otherLeveled : this;
		}

		return PermissionPredicate.super.or(other);
	}

	static LeveledPermissionPredicate fromLevel(PermissionLevel level) {
		return switch (level) {
			case ALL -> ALL;
			case MODERATORS -> MODERATORS;
			case GAMEMASTERS -> GAMEMASTERS;
			case ADMINS -> ADMINS;
			case OWNERS -> OWNERS;
		};
	}

	private static LeveledPermissionPredicate create(PermissionLevel level) {
		return new LeveledPermissionPredicate() {
			@Override
			public PermissionLevel getLevel() {
				return level;
			}

			@Override
			public String toString() {
				return "permission level: " + level.name();
			}
		};
	}
}
