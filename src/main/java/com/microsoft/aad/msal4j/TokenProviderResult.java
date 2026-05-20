package com.microsoft.aad.msal4j;

public class TokenProviderResult {
   private String accessToken;
   private String tenantId;
   private long expiresInSeconds;
   private long refreshInSeconds;

   public String getAccessToken() {
      return this.accessToken;
   }

   public String getTenantId() {
      return this.tenantId;
   }

   public long getExpiresInSeconds() {
      return this.expiresInSeconds;
   }

   public long getRefreshInSeconds() {
      return this.refreshInSeconds;
   }

   public void setAccessToken(String accessToken) {
      this.accessToken = accessToken;
   }

   public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
   }

   public void setExpiresInSeconds(long expiresInSeconds) {
      this.expiresInSeconds = expiresInSeconds;
   }

   public void setRefreshInSeconds(long refreshInSeconds) {
      this.refreshInSeconds = refreshInSeconds;
   }
}
