package com.microsoft.aad.msal4j;

public enum CacheRefreshReason {
   NOT_APPLICABLE(0),
   FORCE_REFRESH(1),
   CLAIMS(1),
   NO_CACHED_ACCESS_TOKEN(2),
   EXPIRED(3),
   PROACTIVE_REFRESH(4);

   final int telemetryValue;

   private CacheRefreshReason(int telemetryValue) {
      this.telemetryValue = telemetryValue;
   }
}
