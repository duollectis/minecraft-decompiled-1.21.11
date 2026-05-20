package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Environment;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public enum YggdrasilEnvironment {
   PROD("https://sessionserver.mojang.com", "https://api.minecraftservices.com", "https://api.mojang.com"),
   STAGING("https://yggdrasil-auth-session-staging.mojang.zone", "https://api-staging.minecraftservices.com", "https://api-staging.mojang.com");

   private final Environment environment;

   private YggdrasilEnvironment(String sessionHost, String servicesHost, String profilesHost) {
      this.environment = new Environment(sessionHost, servicesHost, profilesHost, this.name());
   }

   public Environment getEnvironment() {
      return this.environment;
   }

   public static Optional<Environment> fromString(@Nullable String value) {
      return Stream.of(values()).filter(env -> value != null && value.equalsIgnoreCase(env.name())).findFirst().map(YggdrasilEnvironment::getEnvironment);
   }
}
