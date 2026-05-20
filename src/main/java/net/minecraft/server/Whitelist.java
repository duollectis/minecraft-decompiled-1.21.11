package net.minecraft.server;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.dedicated.management.listener.ManagementListener;

public class Whitelist extends ServerConfigList<PlayerConfigEntry, WhitelistEntry> {
   public Whitelist(File file, ManagementListener managementListener) {
      super(file, managementListener);
   }

   @Override
   protected ServerConfigEntry<PlayerConfigEntry> fromJson(JsonObject json) {
      return new WhitelistEntry(json);
   }

   public boolean isAllowed(PlayerConfigEntry playerConfigEntry) {
      return this.contains(playerConfigEntry);
   }

   public boolean add(WhitelistEntry whitelistEntry) {
      if (super.add(whitelistEntry)) {
         if (whitelistEntry.getKey() != null) {
            this.field_62420.onAllowlistAdded(whitelistEntry.getKey());
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean remove(PlayerConfigEntry playerConfigEntry) {
      if (super.remove(playerConfigEntry)) {
         this.field_62420.onAllowlistRemoved(playerConfigEntry);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void clear() {
      for (WhitelistEntry whitelistEntry : this.values()) {
         if (whitelistEntry.getKey() != null) {
            this.field_62420.onAllowlistRemoved(whitelistEntry.getKey());
         }
      }

      super.clear();
   }

   @Override
   public String[] getNames() {
      return this.values().stream().map(ServerConfigEntry::getKey).filter(Objects::nonNull).map(PlayerConfigEntry::name).toArray(String[]::new);
   }

   protected String toString(PlayerConfigEntry playerConfigEntry) {
      return playerConfigEntry.id().toString();
   }
}
