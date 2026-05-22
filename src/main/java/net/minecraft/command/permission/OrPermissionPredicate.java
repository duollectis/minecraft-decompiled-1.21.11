package net.minecraft.command.permission;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;

/**
 * Составной предикат разрешений: разрешение считается выданным,
 * если хотя бы один из вложенных предикатов его подтверждает.
 * Вложенные {@code OrPermissionPredicate} запрещены — используется плоская структура.
 */
public class OrPermissionPredicate implements PermissionPredicate {

	private final ReferenceSet<PermissionPredicate> predicates = new ReferenceArraySet<>();

	OrPermissionPredicate(PermissionPredicate first, PermissionPredicate second) {
		predicates.add(first);
		predicates.add(second);
		validate();
	}

	private OrPermissionPredicate(ReferenceSet<PermissionPredicate> existing, PermissionPredicate additional) {
		predicates.addAll(existing);
		predicates.add(additional);
		validate();
	}

	private OrPermissionPredicate(ReferenceSet<PermissionPredicate> first, ReferenceSet<PermissionPredicate> second) {
		predicates.addAll(first);
		predicates.addAll(second);
		validate();
	}

	@Override
	public boolean hasPermission(Permission permission) {
		for (PermissionPredicate predicate : predicates) {
			if (predicate.hasPermission(permission)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public PermissionPredicate or(PermissionPredicate other) {
		return other instanceof OrPermissionPredicate otherOr
				? new OrPermissionPredicate(predicates, otherOr.predicates)
				: new OrPermissionPredicate(predicates, other);
	}

	@VisibleForTesting
	public ReferenceSet<PermissionPredicate> getPredicates() {
		return new ReferenceArraySet<>(predicates);
	}

	private void validate() {
		for (PermissionPredicate predicate : predicates) {
			if (predicate instanceof OrPermissionPredicate) {
				throw new IllegalArgumentException("Cannot have PermissionSetUnion within another PermissionSetUnion");
			}
		}
	}
}
