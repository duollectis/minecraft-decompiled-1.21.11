package net.minecraft.command.permission;

/**
 * Предикат, проверяющий наличие разрешения у источника команды.
 */
public interface PermissionPredicate {

	PermissionPredicate NONE = perm -> false;

	PermissionPredicate ALL = perm -> true;

	boolean hasPermission(Permission perm);

	default PermissionPredicate or(PermissionPredicate other) {
		return other instanceof OrPermissionPredicate
				? other.or(this)
				: new OrPermissionPredicate(this, other);
	}
}
