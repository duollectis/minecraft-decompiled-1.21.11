package net.minecraft.server;

import com.google.gson.JsonObject;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionLevel;

public class OperatorEntry extends ServerConfigEntry<PlayerConfigEntry> {
   private final LeveledPermissionPredicate level;
   private final boolean bypassPlayerLimit;

   public OperatorEntry(PlayerConfigEntry player, LeveledPermissionPredicate level, boolean bypassPlayerLimit) {
      super(player);
      this.level = level;
      this.bypassPlayerLimit = bypassPlayerLimit;
   }

   public OperatorEntry(JsonObject json) {
      super(PlayerConfigEntry.read(json));
      PermissionLevel permissionLevel = json.has("level") ? PermissionLevel.fromLevel(json.get("level").getAsInt()) : PermissionLevel.ALL;
      this.level = LeveledPermissionPredicate.fromLevel(permissionLevel);
      this.bypassPlayerLimit = json.has("bypassesPlayerLimit") && json.get("bypassesPlayerLimit").getAsBoolean();
   }

   public LeveledPermissionPredicate getLevel() {
      return this.level;
   }

   public boolean canBypassPlayerLimit() {
      return this.bypassPlayerLimit;
   }

   @Override
   protected void write(JsonObject json) {
      if (this.getKey() != null) {
         this.getKey().write(json);
         json.addProperty("level", this.level.getLevel().getLevel());
         json.addProperty("bypassesPlayerLimit", this.bypassPlayerLimit);
      }
   }
}
