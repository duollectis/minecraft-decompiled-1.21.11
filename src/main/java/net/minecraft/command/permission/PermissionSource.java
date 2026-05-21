package net.minecraft.command.permission;

/**
 * {@code PermissionSource}.
 */
public interface PermissionSource {

	PermissionPredicate getPermissions();
}
