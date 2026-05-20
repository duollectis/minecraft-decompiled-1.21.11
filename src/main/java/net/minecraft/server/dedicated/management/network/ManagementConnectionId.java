package net.minecraft.server.dedicated.management.network;

public record ManagementConnectionId(Integer connectionId) {
   public static ManagementConnectionId of(Integer connectionId) {
      return new ManagementConnectionId(connectionId);
   }
}
