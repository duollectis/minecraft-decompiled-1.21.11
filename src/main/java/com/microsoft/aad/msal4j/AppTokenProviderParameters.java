package com.microsoft.aad.msal4j;

import java.util.Set;

public class AppTokenProviderParameters {
   public Set<String> scopes;
   public String correlationId;
   public String claims;
   public String tenantId;

   public AppTokenProviderParameters(Set<String> scopes, String correlationId, String claims, String tenantId) {
      this.scopes = scopes;
      this.correlationId = correlationId;
      this.claims = claims;
      this.tenantId = tenantId;
   }

   public Set<String> getScopes() {
      return this.scopes;
   }

   public String getCorrelationId() {
      return this.correlationId;
   }

   public String getClaims() {
      return this.claims;
   }

   public String getTenantId() {
      return this.tenantId;
   }

   public void setScopes(Set<String> scopes) {
      this.scopes = scopes;
   }

   public void setCorrelationId(String correlationId) {
      this.correlationId = correlationId;
   }

   public void setClaims(String claims) {
      this.claims = claims;
   }

   public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
   }
}
