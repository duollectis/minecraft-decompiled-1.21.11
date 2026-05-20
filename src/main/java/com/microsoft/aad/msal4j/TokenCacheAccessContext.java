package com.microsoft.aad.msal4j;

public class TokenCacheAccessContext implements ITokenCacheAccessContext {
   private ITokenCache tokenCache;
   private String clientId;
   private IAccount account;
   private boolean hasCacheChanged;

   TokenCacheAccessContext(ITokenCache tokenCache, String clientId, IAccount account, boolean hasCacheChanged) {
      this.tokenCache = tokenCache;
      this.clientId = clientId;
      this.account = account;
      this.hasCacheChanged = hasCacheChanged;
   }

   public static TokenCacheAccessContext.TokenCacheAccessContextBuilder builder() {
      return new TokenCacheAccessContext.TokenCacheAccessContextBuilder();
   }

   @Override
   public ITokenCache tokenCache() {
      return this.tokenCache;
   }

   @Override
   public String clientId() {
      return this.clientId;
   }

   @Override
   public IAccount account() {
      return this.account;
   }

   @Override
   public boolean hasCacheChanged() {
      return this.hasCacheChanged;
   }

   public static class TokenCacheAccessContextBuilder {
      private ITokenCache tokenCache;
      private String clientId;
      private IAccount account;
      private boolean hasCacheChanged;

      TokenCacheAccessContextBuilder() {
      }

      public TokenCacheAccessContext.TokenCacheAccessContextBuilder tokenCache(ITokenCache tokenCache) {
         this.tokenCache = tokenCache;
         return this;
      }

      public TokenCacheAccessContext.TokenCacheAccessContextBuilder clientId(String clientId) {
         this.clientId = clientId;
         return this;
      }

      public TokenCacheAccessContext.TokenCacheAccessContextBuilder account(IAccount account) {
         this.account = account;
         return this;
      }

      public TokenCacheAccessContext.TokenCacheAccessContextBuilder hasCacheChanged(boolean hasCacheChanged) {
         this.hasCacheChanged = hasCacheChanged;
         return this;
      }

      public TokenCacheAccessContext build() {
         return new TokenCacheAccessContext(this.tokenCache, this.clientId, this.account, this.hasCacheChanged);
      }

      @Override
      public String toString() {
         return "TokenCacheAccessContext.TokenCacheAccessContextBuilder(tokenCache="
            + this.tokenCache
            + ", clientId="
            + this.clientId
            + ", account="
            + this.account
            + ", hasCacheChanged="
            + this.hasCacheChanged
            + ")";
      }
   }
}
