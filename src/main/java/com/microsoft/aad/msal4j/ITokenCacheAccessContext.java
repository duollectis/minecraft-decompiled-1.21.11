package com.microsoft.aad.msal4j;

public interface ITokenCacheAccessContext {
   ITokenCache tokenCache();

   String clientId();

   IAccount account();

   boolean hasCacheChanged();
}
