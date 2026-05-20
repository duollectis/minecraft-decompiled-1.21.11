package com.mojang.authlib.yggdrasil;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.mojang.authlib.Environment;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.request.TelemetryEventsRequest;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YggdrassilTelemetrySession implements TelemetrySession {
   private static final Logger LOGGER = LoggerFactory.getLogger(YggdrassilTelemetrySession.class);
   private static final String SOURCE = "minecraft.java";
   private final MinecraftClient minecraftClient;
   private final URL routeEvents;
   private final Executor ioExecutor;

   @VisibleForTesting
   YggdrassilTelemetrySession(MinecraftClient minecraftClient, Environment environment, Executor ioExecutor) {
      this.minecraftClient = minecraftClient;
      this.routeEvents = HttpAuthenticationService.constantURL(environment.servicesHost() + "/events");
      this.ioExecutor = ioExecutor;
   }

   @Override
   public boolean isEnabled() {
      return true;
   }

   @Override
   public TelemetryEvent createNewEvent(String type) {
      return new YggdrassilTelemetryEvent(this, type);
   }

   void sendEvent(String type, JsonObject data) {
      Instant sendTime = Instant.now();
      TelemetryEventsRequest.Event request = new TelemetryEventsRequest.Event("minecraft.java", type, sendTime, data);
      this.ioExecutor.execute(() -> {
         try {
            TelemetryEventsRequest envelope = new TelemetryEventsRequest(ImmutableList.of(request));
            this.minecraftClient.post(this.routeEvents, envelope, Void.class);
         } catch (MinecraftClientException var3x) {
            LOGGER.debug("Failed to send telemetry event {}", request.name(), var3x);
         }
      });
   }
}
