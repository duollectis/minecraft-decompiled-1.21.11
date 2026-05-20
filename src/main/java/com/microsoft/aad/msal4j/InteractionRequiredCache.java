package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class InteractionRequiredCache {
   static int DEFAULT_CACHING_TIME_SEC = 120;
   static final int CACHE_SIZE_LIMIT_TO_TRIGGER_EXPIRED_ENTITIES_REMOVAL = 10;
   static Map<String, InteractionRequiredCache.CachedEntity> requestsToCache = new ConcurrentHashMap<>();

   static void set(String requestHash, MsalInteractionRequiredException ex) {
      removeInvalidCacheEntities();
      long currentTimestamp = System.currentTimeMillis();
      requestsToCache.put(requestHash, new InteractionRequiredCache.CachedEntity(ex, currentTimestamp + DEFAULT_CACHING_TIME_SEC * 1000));
   }

   static MsalInteractionRequiredException getCachedInteractionRequiredException(String requestHash) {
      removeInvalidCacheEntities();
      if (requestsToCache.containsKey(requestHash)) {
         InteractionRequiredCache.CachedEntity cachedEntity = requestsToCache.get(requestHash);
         if (isCacheEntityValid(cachedEntity)) {
            return cachedEntity.exception;
         }

         requestsToCache.remove(requestHash);
      }

      return null;
   }

   private static boolean isCacheEntityValid(InteractionRequiredCache.CachedEntity cachedEntity) {
      long expirationTimestamp = cachedEntity.expirationTimestamp;
      long currentTimestamp = System.currentTimeMillis();
      return currentTimestamp < expirationTimestamp && currentTimestamp >= expirationTimestamp - DEFAULT_CACHING_TIME_SEC * 1000;
   }

   private static void removeInvalidCacheEntities() {
      if (requestsToCache.size() > 10) {
         requestsToCache.values().removeIf(value -> !isCacheEntityValid(value));
      }
   }

   static void clear() {
      requestsToCache.clear();
   }

   private static class CachedEntity {
      MsalInteractionRequiredException exception;
      long expirationTimestamp;

      public CachedEntity(MsalInteractionRequiredException exception, long expirationTimestamp) {
         this.exception = exception;
         this.expirationTimestamp = expirationTimestamp;
      }
   }
}
