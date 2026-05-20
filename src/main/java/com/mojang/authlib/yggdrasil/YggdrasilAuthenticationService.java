package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Environment;
import com.mojang.authlib.EnvironmentParser;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import java.net.Proxy;
import java.net.URL;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YggdrasilAuthenticationService extends HttpAuthenticationService {
   private static final Logger LOGGER = LoggerFactory.getLogger(YggdrasilAuthenticationService.class);
   private final Environment environment;
   private final ServicesKeySet servicesKeySet;

   public YggdrasilAuthenticationService(Proxy proxy) {
      this(proxy, null, determineEnvironment());
   }

   public YggdrasilAuthenticationService(Proxy proxy, Environment environment) {
      this(proxy, null, environment);
   }

   private YggdrasilAuthenticationService(Proxy proxy, @Nullable ServicesKeySet servicesKeySet, Environment environment) {
      super(proxy);
      this.environment = environment;
      LOGGER.info("Environment: {}", environment);
      MinecraftClient client = MinecraftClient.unauthenticated(proxy);
      URL publicKeySetUrl = HttpAuthenticationService.constantURL(environment.servicesHost() + "/publickeys");
      this.servicesKeySet = servicesKeySet != null ? servicesKeySet : YggdrasilServicesKeyInfo.get(publicKeySetUrl, client);
   }

   public static YggdrasilAuthenticationService createOffline(Proxy proxy) {
      return new YggdrasilAuthenticationService(proxy, ServicesKeySet.EMPTY, determineEnvironment());
   }

   public static YggdrasilAuthenticationService createOffline(Proxy proxy, Environment environment) {
      return new YggdrasilAuthenticationService(proxy, ServicesKeySet.EMPTY, environment);
   }

   private static Environment determineEnvironment() {
      return EnvironmentParser.getEnvironmentFromProperties().orElse(YggdrasilEnvironment.PROD.getEnvironment());
   }

   @Override
   public MinecraftSessionService createMinecraftSessionService() {
      return new YggdrasilMinecraftSessionService(this.servicesKeySet, this.getProxy(), this.environment);
   }

   @Override
   public GameProfileRepository createProfileRepository() {
      return new YggdrasilGameProfileRepository(this.getProxy(), this.environment);
   }

   public UserApiService createUserApiService(String accessToken) {
      return new YggdrasilUserApiService(accessToken, this.getProxy(), this.environment);
   }

   public ServicesKeySet getServicesKeySet() {
      return this.servicesKeySet;
   }
}
