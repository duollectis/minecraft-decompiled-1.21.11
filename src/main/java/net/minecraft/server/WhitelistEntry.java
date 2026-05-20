package net.minecraft.server;

import com.google.gson.JsonObject;

public class WhitelistEntry extends ServerConfigEntry<PlayerConfigEntry> {
   public WhitelistEntry(PlayerConfigEntry player) {
      super(player);
   }

   public WhitelistEntry(JsonObject json) {
      super(PlayerConfigEntry.read(json));
   }

   @Override
   protected void write(JsonObject json) {
      if (this.getKey() != null) {
         this.getKey().write(json);
      }
   }
}
