package com.microsoft.aad.msal4j;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SilentParameters implements IAcquireTokenParameters {
   private Set<String> scopes;
   private IAccount account;
   private ClaimsRequest claims;
   private String authorityUrl;
   private boolean forceRefresh;
   private Map<String, String> extraHttpHeaders;
   private Map<String, String> extraQueryParameters;
   private String tenant;
   private PopParameters proofOfPossession;

   private SilentParameters(
      Set<String> scopes,
      IAccount account,
      ClaimsRequest claims,
      String authorityUrl,
      boolean forceRefresh,
      Map<String, String> extraHttpHeaders,
      Map<String, String> extraQueryParameters,
      String tenant,
      PopParameters proofOfPossession
   ) {
      this.scopes = scopes;
      this.account = account;
      this.claims = claims;
      this.authorityUrl = authorityUrl;
      this.forceRefresh = forceRefresh;
      this.extraHttpHeaders = extraHttpHeaders;
      this.extraQueryParameters = extraQueryParameters;
      this.tenant = tenant;
      this.proofOfPossession = proofOfPossession;
   }

   private static SilentParameters.SilentParametersBuilder builder() {
      return new SilentParameters.SilentParametersBuilder();
   }

   public static SilentParameters.SilentParametersBuilder builder(Set<String> scopes, IAccount account) {
      ParameterValidationUtils.validateNotNull("account", account);
      ParameterValidationUtils.validateNotNull("scopes", scopes);
      return builder().scopes(removeEmptyScope(scopes)).account(account);
   }

   public static SilentParameters.SilentParametersBuilder builder(Set<String> scopes) {
      ParameterValidationUtils.validateNotNull("scopes", scopes);
      return builder().scopes(removeEmptyScope(scopes));
   }

   private static Set<String> removeEmptyScope(Set<String> scopes) {
      Set<String> updatedScopes = new HashSet<>();

      for (String scope : scopes) {
         if (!scope.equalsIgnoreCase(StringHelper.EMPTY_STRING)) {
            updatedScopes.add(scope.trim());
         }
      }

      return updatedScopes;
   }

   @Override
   public Set<String> scopes() {
      return this.scopes;
   }

   public IAccount account() {
      return this.account;
   }

   @Override
   public ClaimsRequest claims() {
      return this.claims;
   }

   public String authorityUrl() {
      return this.authorityUrl;
   }

   public boolean forceRefresh() {
      return this.forceRefresh;
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

   public PopParameters proofOfPossession() {
      return this.proofOfPossession;
   }

   public static class SilentParametersBuilder {
      private Set<String> scopes;
      private IAccount account;
      private ClaimsRequest claims;
      private String authorityUrl;
      private boolean forceRefresh;
      private Map<String, String> extraHttpHeaders;
      private Map<String, String> extraQueryParameters;
      private String tenant;
      private PopParameters proofOfPossession;

      SilentParametersBuilder() {
      }

      public SilentParameters.SilentParametersBuilder proofOfPossession(HttpMethod httpMethod, URI uri, String nonce) {
         this.proofOfPossession = new PopParameters(httpMethod, uri, nonce);
         return this;
      }

      public SilentParameters.SilentParametersBuilder scopes(Set<String> scopes) {
         ParameterValidationUtils.validateNotNull("scopes", scopes);
         this.scopes = scopes;
         return this;
      }

      public SilentParameters.SilentParametersBuilder account(IAccount account) {
         this.account = account;
         return this;
      }

      public SilentParameters.SilentParametersBuilder claims(ClaimsRequest claims) {
         this.claims = claims;
         return this;
      }

      public SilentParameters.SilentParametersBuilder authorityUrl(String authorityUrl) {
         this.authorityUrl = authorityUrl;
         return this;
      }

      public SilentParameters.SilentParametersBuilder forceRefresh(boolean forceRefresh) {
         this.forceRefresh = forceRefresh;
         return this;
      }

      public SilentParameters.SilentParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
         this.extraHttpHeaders = extraHttpHeaders;
         return this;
      }

      public SilentParameters.SilentParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
         this.extraQueryParameters = extraQueryParameters;
         return this;
      }

      public SilentParameters.SilentParametersBuilder tenant(String tenant) {
         this.tenant = tenant;
         return this;
      }

      public SilentParameters build() {
         return new SilentParameters(
            this.scopes,
            this.account,
            this.claims,
            this.authorityUrl,
            this.forceRefresh,
            this.extraHttpHeaders,
            this.extraQueryParameters,
            this.tenant,
            this.proofOfPossession
         );
      }

      @Override
      public String toString() {
         return "SilentParameters.SilentParametersBuilder(scopes="
            + this.scopes
            + ", account="
            + this.account
            + ", claims="
            + this.claims
            + ", authorityUrl="
            + this.authorityUrl
            + ", forceRefresh="
            + this.forceRefresh
            + ", extraHttpHeaders="
            + this.extraHttpHeaders
            + ", extraQueryParameters="
            + this.extraQueryParameters
            + ", tenant="
            + this.tenant
            + ", proofOfPossession="
            + this.proofOfPossession
            + ")";
      }
   }
}
