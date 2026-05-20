package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

public class RefreshTokenParameters implements IAcquireTokenParameters {
   private Set<String> scopes;
   private String refreshToken;
   private ClaimsRequest claims;
   private Map<String, String> extraHttpHeaders;
   private Map<String, String> extraQueryParameters;
   private String tenant;

   private RefreshTokenParameters(
      Set<String> scopes,
      String refreshToken,
      ClaimsRequest claims,
      Map<String, String> extraHttpHeaders,
      Map<String, String> extraQueryParameters,
      String tenant
   ) {
      this.scopes = scopes;
      this.refreshToken = refreshToken;
      this.claims = claims;
      this.extraHttpHeaders = extraHttpHeaders;
      this.extraQueryParameters = extraQueryParameters;
      this.tenant = tenant;
   }

   private static RefreshTokenParameters.RefreshTokenParametersBuilder builder() {
      return new RefreshTokenParameters.RefreshTokenParametersBuilder();
   }

   public static RefreshTokenParameters.RefreshTokenParametersBuilder builder(Set<String> scopes, String refreshToken) {
      ParameterValidationUtils.validateNotBlank("refreshToken", refreshToken);
      return builder().scopes(scopes).refreshToken(refreshToken);
   }

   @Override
   public Set<String> scopes() {
      return this.scopes;
   }

   public String refreshToken() {
      return this.refreshToken;
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

   public static class RefreshTokenParametersBuilder {
      private Set<String> scopes;
      private String refreshToken;
      private ClaimsRequest claims;
      private Map<String, String> extraHttpHeaders;
      private Map<String, String> extraQueryParameters;
      private String tenant;

      RefreshTokenParametersBuilder() {
      }

      public RefreshTokenParameters.RefreshTokenParametersBuilder scopes(Set<String> scopes) {
         ParameterValidationUtils.validateNotNull("scopes", scopes);
         this.scopes = scopes;
         return this;
      }

      public RefreshTokenParameters.RefreshTokenParametersBuilder refreshToken(String refreshToken) {
         ParameterValidationUtils.validateNotNull("refreshToken", this.scopes);
         this.refreshToken = refreshToken;
         return this;
      }

      public RefreshTokenParameters.RefreshTokenParametersBuilder claims(ClaimsRequest claims) {
         this.claims = claims;
         return this;
      }

      public RefreshTokenParameters.RefreshTokenParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
         this.extraHttpHeaders = extraHttpHeaders;
         return this;
      }

      public RefreshTokenParameters.RefreshTokenParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
         this.extraQueryParameters = extraQueryParameters;
         return this;
      }

      public RefreshTokenParameters.RefreshTokenParametersBuilder tenant(String tenant) {
         this.tenant = tenant;
         return this;
      }

      public RefreshTokenParameters build() {
         return new RefreshTokenParameters(this.scopes, this.refreshToken, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant);
      }

      @Override
      public String toString() {
         return "RefreshTokenParameters.RefreshTokenParametersBuilder(scopes="
            + this.scopes
            + ", refreshToken="
            + this.refreshToken
            + ", claims="
            + this.claims
            + ", extraHttpHeaders="
            + this.extraHttpHeaders
            + ", extraQueryParameters="
            + this.extraQueryParameters
            + ", tenant="
            + this.tenant
            + ")";
      }
   }
}
