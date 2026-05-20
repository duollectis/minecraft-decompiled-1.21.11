package com.microsoft.aad.msal4j;

public interface ITokenCacheAccessAspect {
   void beforeCacheAccess(ITokenCacheAccessContext var1);

   void afterCacheAccess(ITokenCacheAccessContext var1);
}
