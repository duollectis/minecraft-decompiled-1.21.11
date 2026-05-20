package net.minecraft.server;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.dedicated.management.listener.ManagementListener;

public class BannedPlayerList extends ServerConfigList<PlayerConfigEntry, BannedPlayerEntry> {
   public BannedPlayerList(File file, ManagementListener managementListener) {
      super(file, managementListener);
   }

   @Override
   protected ServerConfigEntry<PlayerConfigEntry> fromJson(JsonObject json) {
      return new BannedPlayerEntry(json);
   }

   public boolean contains(PlayerConfigEntry player) {
      return super.contains(player);
   }

   @Override
   public String[] getNames() {
      return this.values().stream().map(ServerConfigEntry::getKey).filter(Objects::nonNull).map(PlayerConfigEntry::name).toArray(String[]::new);
   }

   protected String toString(PlayerConfigEntry playerConfigEntry) {
      return playerConfigEntry.id().toString();
   }

   public boolean add(BannedPlayerEntry bannedPlayerEntry) {
      if (super.add(bannedPlayerEntry)) {
         if (bannedPlayerEntry.getKey() != null) {
            this.field_62420.onBanAdded(bannedPlayerEntry);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean remove(PlayerConfigEntry playerConfigEntry) {
      if (super.remove(playerConfigEntry)) {
         this.field_62420.onBanRemoved(playerConfigEntry);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void clear() {
      for (BannedPlayerEntry bannedPlayerEntry : this.values()) {
         if (bannedPlayerEntry.getKey() != null) {
            this.field_62420.onBanRemoved(bannedPlayerEntry.getKey());
         }
      }

      super.clear();
   }
}
