package com.mojang.authlib;

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentParser {
   @Nullable
   private static String environmentOverride;
   private static final String PROP_PREFIX = "minecraft.api.";
   private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentParser.class);
   public static final String PROP_ENV = "minecraft.api.env";
   public static final String PROP_SESSION_HOST = "minecraft.api.session.host";
   public static final String PROP_SERVICES_HOST = "minecraft.api.services.host";
   public static final String PROP_PROFILES_HOST = "minecraft.api.profiles.host";

   public static void setEnvironmentOverride(@Nullable String override) {
      environmentOverride = override;
   }

   public static Optional<Environment> getEnvironmentFromProperties() {
      String envName = environmentOverride != null ? environmentOverride : System.getProperty("minecraft.api.env");
      Optional<Environment> env = YggdrasilEnvironment.fromString(envName);
      return env.isPresent() ? env : fromHostNames();
   }

   private static Optional<Environment> fromHostNames() {
      String session = System.getProperty("minecraft.api.session.host");
      String services = System.getProperty("minecraft.api.services.host");
      String profiles = System.getProperty("minecraft.api.profiles.host");
      if (services != null && session != null && profiles != null) {
         return Optional.of(new Environment(session, services, profiles, "properties"));
      } else {
         if (services != null || session != null || profiles != null) {
            LOGGER.info(
               "Ignoring hosts properties. All need to be set: {}",
               List.of("minecraft.api.services.host", "minecraft.api.session.host", "minecraft.api.profiles.host")
            );
         }

         return Optional.empty();
      }
   }
}
