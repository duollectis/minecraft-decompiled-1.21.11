package com.microsoft.aad.msal4j;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class UserNamePasswordParameters implements IAcquireTokenParameters {
   private Set<String> scopes;
   private String username;
   private char[] password;
   private ClaimsRequest claims;
   private Map<String, String> extraHttpHeaders;
   private Map<String, String> extraQueryParameters;
   private String tenant;
   private PopParameters proofOfPossession;

   private UserNamePasswordParameters(
      Set<String> scopes,
      String username,
      char[] password,
      ClaimsRequest claims,
      Map<String, String> extraHttpHeaders,
      Map<String, String> extraQueryParameters,
      String tenant,
      PopParameters proofOfPossession
   ) {
      this.scopes = scopes;
      this.username = username;
      this.password = password;
      this.claims = claims;
      this.extraHttpHeaders = extraHttpHeaders;
      this.extraQueryParameters = extraQueryParameters;
      this.tenant = tenant;
      this.proofOfPossession = proofOfPossession;
   }

   public char[] password() {
      return (char[])this.password.clone();
   }

   private static UserNamePasswordParameters.UserNamePasswordParametersBuilder builder() {
      return new UserNamePasswordParameters.UserNamePasswordParametersBuilder();
   }

   public static UserNamePasswordParameters.UserNamePasswordParametersBuilder builder(Set<String> scopes, String username, char[] password) {
      ParameterValidationUtils.validateNotNull("scopes", scopes);
      ParameterValidationUtils.validateNotBlank("username", username);
      ParameterValidationUtils.validateNotEmpty("password", password);
      return builder().scopes(scopes).username(username).password(password);
   }

   @Override
   public Set<String> scopes() {
      return this.scopes;
   }

   public String username() {
      return this.username;
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

   public PopParameters proofOfPossession() {
      return this.proofOfPossession;
   }

   public static class UserNamePasswordParametersBuilder {
      private Set<String> scopes;
      private String username;
      private char[] password;
      private ClaimsRequest claims;
      private Map<String, String> extraHttpHeaders;
      private Map<String, String> extraQueryParameters;
      private String tenant;
      private PopParameters proofOfPossession;

      UserNamePasswordParametersBuilder() {
      }

      public UserNamePasswordParameters.UserNamePasswordParametersBuilder password(char[] password) {
         ParameterValidationUtils.validateNotNull("password", password);
         this.password = (char[])password.clone();
         return this;
      }

      public UserNamePasswordParameters.UserNamePasswordParametersBuilder proofOfPossession(HttpMethod httpMethod, URI uri, String nonce) {
         this.proofOfPossession = new PopParameters(httpMethod, uri, nonce);
         return this;
      }

      public UserNamePasswordParameters.UserNamePasswordParametersBuilder scopes(Set<String> scopes) {
         ParameterValidationUtils.validateNotNull("scopes", scopes);
         this.scopes = scopes;
         return this;
      }

      public UserNamePasswordParameters.UserNamePasswordParametersBuilder username(String username) {
         ParameterValidationUtils.validateNotNull("username", username);
         this.username = username;
         return this;
      }

      public UserNamePasswordParameters.UserNamePasswordParametersBuilder claims(ClaimsRequest claims) {
         this.claims = claims;
         return this;
      }

      public UserNamePasswordParameters.UserNamePasswordParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
         this.extraHttpHeaders = extraHttpHeaders;
         return this;
      }

      public UserNamePasswordParameters.UserNamePasswordParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
         this.extraQueryParameters = extraQueryParameters;
         return this;
      }

      public UserNamePasswordParameters.UserNamePasswordParametersBuilder tenant(String tenant) {
         this.tenant = tenant;
         return this;
      }

      public UserNamePasswordParameters build() {
         return new UserNamePasswordParameters(
            this.scopes, this.username, this.password, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.proofOfPossession
         );
      }

      @Override
      public String toString() {
         return "UserNamePasswordParameters.UserNamePasswordParametersBuilder(scopes="
            + this.scopes
            + ", username="
            + this.username
            + ", password="
            + Arrays.toString(this.password)
            + ", claims="
            + this.claims
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
