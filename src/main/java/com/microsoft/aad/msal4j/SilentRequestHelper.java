package com.microsoft.aad.msal4j;

import java.util.Date;
import org.slf4j.Logger;

class SilentRequestHelper {
   private static final int ACCESS_TOKEN_EXPIRE_BUFFER_IN_SEC = 300;

   private SilentRequestHelper() {
   }

   static CacheRefreshReason getCacheRefreshReasonIfApplicable(SilentParameters parameters, AuthenticationResult cachedResult, Logger log) {
      if (parameters.claims() != null) {
         log.debug(String.format("Refreshing access token. Cache refresh reason: %s", CacheRefreshReason.CLAIMS));
         return CacheRefreshReason.CLAIMS;
      } else {
         long currTimeStampSec = new Date().getTime() / 1000L;
         if (!StringHelper.isBlank(cachedResult.accessToken()) && cachedResult.expiresOn() < currTimeStampSec + 300L) {
            log.debug(String.format("Refreshing access token. Cache refresh reason: %s", CacheRefreshReason.EXPIRED));
            return CacheRefreshReason.EXPIRED;
         } else if (!StringHelper.isBlank(cachedResult.accessToken())
            && cachedResult.refreshOn() != null
            && cachedResult.refreshOn() > 0L
            && cachedResult.refreshOn() < currTimeStampSec
            && cachedResult.expiresOn() >= currTimeStampSec + 300L) {
            log.debug(String.format("Refreshing access token. Cache refresh reason: %s", CacheRefreshReason.PROACTIVE_REFRESH));
            return CacheRefreshReason.PROACTIVE_REFRESH;
         } else if (StringHelper.isBlank(cachedResult.accessToken()) && !StringHelper.isBlank(cachedResult.refreshToken())) {
            log.debug(String.format("Refreshing access token. Cache refresh reason: %s", CacheRefreshReason.NO_CACHED_ACCESS_TOKEN));
            return CacheRefreshReason.NO_CACHED_ACCESS_TOKEN;
         } else {
            return CacheRefreshReason.NOT_APPLICABLE;
         }
      }
   }
}
