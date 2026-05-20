package net.minecraft.server.dedicated.management.handler;

import java.util.Collection;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;

public interface AllowlistManagementHandler {
   Collection<WhitelistEntry> getAllowlist();

   boolean add(WhitelistEntry player, ManagementConnectionId remote);

   void clear(ManagementConnectionId remote);

   void remove(PlayerConfigEntry player, ManagementConnectionId remote);

   void kickUnlisted(ManagementConnectionId remote);
}
