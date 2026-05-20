package net.minecraft.server.dedicated.management.handler;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.OperatorEntry;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;

public interface OperatorManagementHandler {
   Collection<OperatorEntry> getOperators();

   void addToOperators(
      PlayerConfigEntry player, Optional<PermissionLevel> permissionLevel, Optional<Boolean> canBypassPlayerLimit, ManagementConnectionId remote
   );

   void addToOperators(PlayerConfigEntry player, ManagementConnectionId remote);

   void removeFromOperators(PlayerConfigEntry player, ManagementConnectionId remote);

   void clearOperators(ManagementConnectionId remote);
}
