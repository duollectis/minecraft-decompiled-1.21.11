package com.microsoft.aad.msal4j;

import java.io.Serializable;

public class AuthenticationResultMetadata implements Serializable {
   private TokenSource tokenSource;
   private Long refreshOn;
   private CacheRefreshReason cacheRefreshReason = CacheRefreshReason.NOT_APPLICABLE;

   AuthenticationResultMetadata(TokenSource tokenSource, Long refreshOn, CacheRefreshReason cacheRefreshReason) {
      this.tokenSource = tokenSource;
      this.refreshOn = refreshOn;
      this.cacheRefreshReason = cacheRefreshReason == null ? CacheRefreshReason.NOT_APPLICABLE : cacheRefreshReason;
   }

   public static AuthenticationResultMetadata.AuthenticationResultMetadataBuilder builder() {
      return new AuthenticationResultMetadata.AuthenticationResultMetadataBuilder();
   }

   public TokenSource tokenSource() {
      return this.tokenSource;
   }

   public Long refreshOn() {
      return this.refreshOn;
   }

   public CacheRefreshReason cacheRefreshReason() {
      return this.cacheRefreshReason;
   }

   void tokenSource(TokenSource tokenSource) {
      this.tokenSource = tokenSource;
   }

   void refreshOn(Long refreshOn) {
      this.refreshOn = refreshOn;
   }

   void cacheRefreshReason(CacheRefreshReason cacheRefreshReason) {
      this.cacheRefreshReason = cacheRefreshReason;
   }

   public static class AuthenticationResultMetadataBuilder {
      private TokenSource tokenSource;
      private Long refreshOn;
      private CacheRefreshReason cacheRefreshReason;

      AuthenticationResultMetadataBuilder() {
      }

      public AuthenticationResultMetadata.AuthenticationResultMetadataBuilder tokenSource(TokenSource tokenSource) {
         this.tokenSource = tokenSource;
         return this;
      }

      public AuthenticationResultMetadata.AuthenticationResultMetadataBuilder refreshOn(Long refreshOn) {
         this.refreshOn = refreshOn;
         return this;
      }

      public AuthenticationResultMetadata.AuthenticationResultMetadataBuilder cacheRefreshReason(CacheRefreshReason cacheRefreshReason) {
         this.cacheRefreshReason = cacheRefreshReason;
         return this;
      }

      public AuthenticationResultMetadata build() {
         return new AuthenticationResultMetadata(this.tokenSource, this.refreshOn, this.cacheRefreshReason);
      }

      @Override
      public String toString() {
         return "AuthenticationResultMetadata.AuthenticationResultMetadataBuilder(tokenSource="
            + this.tokenSource
            + ", refreshOn="
            + this.refreshOn
            + ", cacheRefreshReason$value="
            + this.cacheRefreshReason
            + ")";
      }
   }
}
