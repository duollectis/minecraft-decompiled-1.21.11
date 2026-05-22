package net.minecraft.command.permission;

import java.util.function.Predicate;

/**
 * Предикат, проверяющий наличие разрешения у источника команды через {@link PermissionCheck}.
 *
 * @param <T> тип источника, реализующего {@link PermissionSource}
 */
public record PermissionSourcePredicate<T extends PermissionSource>(PermissionCheck test) implements Predicate<T> {

	public boolean test(T permissionSource) {
		return test.allows(permissionSource.getPermissions());
	}
}
