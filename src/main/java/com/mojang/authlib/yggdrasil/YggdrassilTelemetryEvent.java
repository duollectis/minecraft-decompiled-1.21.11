package com.mojang.authlib.yggdrasil;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.TelemetryEvent;
import javax.annotation.Nullable;

public class YggdrassilTelemetryEvent implements TelemetryEvent {
   private final YggdrassilTelemetrySession service;
   private final String type;
   @Nullable
   private JsonObject data = new JsonObject();

   YggdrassilTelemetryEvent(YggdrassilTelemetrySession service, String type) {
      this.service = service;
      this.type = type;
   }

   private JsonObject data() {
      if (this.data == null) {
         throw new IllegalStateException("Event already sent");
      } else {
         return this.data;
      }
   }

   @Override
   public void addProperty(String id, String value) {
      this.data().addProperty(id, value);
   }

   @Override
   public void addProperty(String id, int value) {
      this.data().addProperty(id, value);
   }

   @Override
   public void addProperty(String id, long value) {
      this.data().addProperty(id, value);
   }

   @Override
   public void addProperty(String id, boolean value) {
      this.data().addProperty(id, value);
   }

   @Override
   public void addNullProperty(String id) {
      this.data().add(id, JsonNull.INSTANCE);
   }

   @Override
   public void send() {
      this.service.sendEvent(this.type, this.data);
      this.data = null;
   }
}
