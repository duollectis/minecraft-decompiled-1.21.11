package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

public class ClientCredentialParameters implements IAcquireTokenParameters {
   private Set<String> scopes;
   private Boolean skipCache = false;
   private ClaimsRequest claims;
   private Map<String, String> extraHttpHeaders;
   private Map<String, String> extraQueryParameters;
   private String tenant;
   private IClientCredential clientCredential;

   private ClientCredentialParameters(
      Set<String> scopes,
      Boolean skipCache,
      ClaimsRequest claims,
      Map<String, String> extraHttpHeaders,
      Map<String, String> extraQueryParameters,
      String tenant,
      IClientCredential clientCredential
   ) {
      this.scopes = scopes;
      this.skipCache = skipCache;
      this.claims = claims;
      this.extraHttpHeaders = extraHttpHeaders;
      this.extraQueryParameters = extraQueryParameters;
      this.tenant = tenant;
      this.clientCredential = clientCredential;
   }

   private static ClientCredentialParameters.ClientCredentialParametersBuilder builder() {
      return new ClientCredentialParameters.ClientCredentialParametersBuilder();
   }

   public static ClientCredentialParameters.ClientCredentialParametersBuilder builder(Set<String> scopes) {
      ParameterValidationUtils.validateNotNull("scopes", scopes);
      return builder().scopes(scopes);
   }

   @Override
   public Set<String> scopes() {
      return this.scopes;
   }

   public Boolean skipCache() {
      return this.skipCache;
   }

   @Override
   public ClaimsRequest claims() {
      return this.claims;
   }

   @Override
   public Map<String, String> extraHttpHeaders() {
      return this.extraHttpHeaders;
   }

   @Override
   public Map<String, String> extraQueryParameters() {
      return this.extraQueryParameters;
   }

   @Override
   public String tenant() {
      return this.tenant;
   }

   public IClientCredential clientCredential() {
      return this.clientCredential;
   }

   public static class ClientCredentialParametersBuilder {
      private Set<String> scopes;
      private Boolean skipCache = false;
      private ClaimsRequest claims;
      private Map<String, String> extraHttpHeaders;
      private Map<String, String> extraQueryParameters;
      private String tenant;
      private IClientCredential clientCredential;

      ClientCredentialParametersBuilder() {
      }

      public ClientCredentialParameters.ClientCredentialParametersBuilder scopes(Set<String> scopes) {
         ParameterValidationUtils.validateNotNull("scopes", scopes);
         this.scopes = scopes;
         return this;
      }

      public ClientCredentialParameters.ClientCredentialParametersBuilder skipCache(Boolean skipCache) {
         this.skipCache = skipCache;
         return this;
      }

      public ClientCredentialParameters.ClientCredentialParametersBuilder claims(ClaimsRequest claims) {
         this.claims = claims;
         return this;
      }

      public ClientCredentialParameters.ClientCredentialParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
         this.extraHttpHeaders = extraHttpHeaders;
         return this;
      }

      public ClientCredentialParameters.ClientCredentialParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
         this.extraQueryParameters = extraQueryParameters;
         return this;
      }

      public ClientCredentialParameters.ClientCredentialParametersBuilder tenant(String tenant) {
         this.tenant = tenant;
         return this;
      }

      public ClientCredentialParameters.ClientCredentialParametersBuilder clientCredential(IClientCredential clientCredential) {
         this.clientCredential = clientCredential;
         return this;
      }

      public ClientCredentialParameters build() {
         return new ClientCredentialParameters(
            this.scopes, this.skipCache, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.clientCredential
         );
      }

      @Override
      public String toString() {
         return "ClientCredentialParameters.ClientCredentialParametersBuilder(scopes="
            + this.scopes
            + ", skipCache="
            + this.skipCache
            + ", claims="
            + this.claims
            + ", extraHttpHeaders="
            + this.extraHttpHeaders
            + ", extraQueryParameters="
            + this.extraQueryParameters
            + ", tenant="
            + this.tenant
            + ", clientCredential="
            + this.clientCredential
            + ")";
      }
   }
}
