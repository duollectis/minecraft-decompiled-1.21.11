package net.minecraft.command.permission;

import net.minecraft.command.DefaultPermissions;

/**
 * {@code LeveledPermissionPredicate}.
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
			return this.getLevel().isAtLeast(level.level());
		}
		else {
			return permission.equals(DefaultPermissions.ENTITY_SELECTORS) ? this
			                                                                .getLevel()
			                                                                .isAtLeast(PermissionLevel.GAMEMASTERS)
			                                                              : false;
		}
	}

	@Override
	default PermissionPredicate or(PermissionPredicate other) {
		if (other instanceof LeveledPermissionPredicate leveledPermissionPredicate) {
			return this.getLevel().isAtLeast(leveledPermissionPredicate.getLevel()) ? leveledPermissionPredicate : this;
		}
		else {
			return PermissionPredicate.super.or(other);
		}
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
