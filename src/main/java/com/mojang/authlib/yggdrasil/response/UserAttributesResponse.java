package com.mojang.authlib.yggdrasil.response;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

public record UserAttributesResponse(
   @Nullable @SerializedName("privileges") UserAttributesResponse.Privileges privileges,
   @Nullable @SerializedName("profanityFilterPreferences") UserAttributesResponse.ProfanityFilterPreferences profanityFilterPreferences,
   @Nullable @SerializedName("banStatus") UserAttributesResponse.BanStatus banStatus
) {
   public record BanStatus(@SerializedName("bannedScopes") Map<String, UserAttributesResponse.BanStatus.BannedScope> bannedScopes) {
      public record BannedScope(
         @SerializedName("banId") UUID banId,
         @Nullable @SerializedName("expires") Instant expires,
         @SerializedName("reason") String reason,
         @Nullable @SerializedName("reasonMessage") String reasonMessage
      ) {
      }
   }

   public record Privileges(
      @Nullable @SerializedName("onlineChat") UserAttributesResponse.Privileges.Privilege onlineChat,
      @Nullable @SerializedName("multiplayerServer") UserAttributesResponse.Privileges.Privilege multiplayerServer,
      @Nullable @SerializedName("multiplayerRealms") UserAttributesResponse.Privileges.Privilege multiplayerRealms,
      @Nullable @SerializedName("telemetry") UserAttributesResponse.Privileges.Privilege telemetry,
      @Nullable @SerializedName("optionalTelemetry") UserAttributesResponse.Privileges.Privilege optionalTelemetry
   ) {
      public boolean getOnlineChat() {
         return this.onlineChat != null && this.onlineChat.enabled;
      }

      public boolean getMultiplayerServer() {
         return this.multiplayerServer != null && this.multiplayerServer.enabled;
      }

      public boolean getMultiplayerRealms() {
         return this.multiplayerRealms != null && this.multiplayerRealms.enabled;
      }

      public boolean getTelemetry() {
         return this.telemetry != null && this.telemetry.enabled;
      }

      public boolean getOptionalTelemetry() {
         return this.optionalTelemetry != null && this.optionalTelemetry.enabled;
      }

      public record Privilege(@SerializedName("enabled") boolean enabled) {
      }
   }

   public record ProfanityFilterPreferences(@SerializedName("profanityFilterOn") boolean enabled) {
   }
}
