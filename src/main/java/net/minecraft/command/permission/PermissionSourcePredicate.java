package net.minecraft.command.permission;

import java.util.function.Predicate;

public record PermissionSourcePredicate<T extends PermissionSource>(PermissionCheck test) implements Predicate<T> {
   public boolean test(T permissionSource) {
      return this.test.allows(permissionSource.getPermissions());
   }
}
