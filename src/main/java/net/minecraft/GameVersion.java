package net.minecraft;

import java.util.Date;
import net.minecraft.resource.PackVersion;
import net.minecraft.resource.ResourceType;

public interface GameVersion {
   SaveVersion dataVersion();

   String id();

   String name();

   int protocolVersion();

   PackVersion packVersion(ResourceType type);

   Date buildTime();

   boolean stable();

   public record Impl(
      String id,
      String name,
      SaveVersion dataVersion,
      int protocolVersion,
      PackVersion resourcePackVersion,
      PackVersion datapackVersion,
      Date buildTime,
      boolean stable
   ) implements GameVersion {
      @Override
      public PackVersion packVersion(ResourceType type) {
         return switch (type) {
            case CLIENT_RESOURCES -> this.resourcePackVersion;
            case SERVER_DATA -> this.datapackVersion;
         };
      }
   }
}
