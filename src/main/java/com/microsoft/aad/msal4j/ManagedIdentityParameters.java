package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

public class ManagedIdentityParameters implements IAcquireTokenParameters {
   String resource;
   boolean forceRefresh;
   String claims;
   String revokedTokenHash;

   private ManagedIdentityParameters(String resource, boolean forceRefresh, String claims) {
      this.resource = resource;
      this.forceRefresh = forceRefresh;
      this.claims = claims;
   }

   @Override
   public Set<String> scopes() {
      return null;
   }

   @Override
   public ClaimsRequest claims() {
      if (this.claims != null && !this.claims.isEmpty()) {
         try {
            return ClaimsRequest.formatAsClaimsRequest(this.claims);
         } catch (Exception var2) {
            throw new MsalClientException("Failed to parse claims JSON: " + var2.getMessage(), "invalid_json");
         }
      } else {
         return null;
      }
   }

   @Override
   public Map<String, String> extraHttpHeaders() {
      return null;
   }

   @Override
   public String tenant() {
      return "managed_identity";
   }

   @Override
   public Map<String, String> extraQueryParameters() {
      return null;
   }

   private static ManagedIdentityParameters.ManagedIdentityParametersBuilder builder() {
      return new ManagedIdentityParameters.ManagedIdentityParametersBuilder();
   }

   public static ManagedIdentityParameters.ManagedIdentityParametersBuilder builder(String resource) {
      return builder().resource(resource);
   }

   public boolean forceRefresh() {
      return this.forceRefresh;
   }

   public String resource() {
      return this.resource;
   }

   public String revokedTokenHash() {
      return this.revokedTokenHash;
   }

   public static class ManagedIdentityParametersBuilder {
      private String resource;
      private boolean forceRefresh;
      private String claims;

      ManagedIdentityParametersBuilder() {
      }

      public ManagedIdentityParameters.ManagedIdentityParametersBuilder resource(String resource) {
         this.resource = resource;
         return this;
      }

      public ManagedIdentityParameters.ManagedIdentityParametersBuilder forceRefresh(boolean forceRefresh) {
         this.forceRefresh = forceRefresh;
         return this;
      }

      public ManagedIdentityParameters.ManagedIdentityParametersBuilder claims(String claims) {
         ParameterValidationUtils.validateNotBlank("claims", claims);
         this.claims = claims;
         return this;
      }

      public ManagedIdentityParameters build() {
         return new ManagedIdentityParameters(this.resource, this.forceRefresh, this.claims);
      }

      @Override
      public String toString() {
         return "ManagedIdentityParameters.ManagedIdentityParametersBuilder(resource=" + this.resource + ", forceRefresh=" + this.forceRefresh + ")";
      }
   }
}
