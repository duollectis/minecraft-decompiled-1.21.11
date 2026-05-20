package com.microsoft.aad.msal4j;

import java.net.URI;
import java.util.Map;
import java.util.Set;

public class InteractiveRequestParameters implements IAcquireTokenParameters {
   private URI redirectUri;
   private ClaimsRequest claims;
   private Set<String> scopes;
   private Prompt prompt;
   private String loginHint;
   private String domainHint;
   private SystemBrowserOptions systemBrowserOptions;
   private String claimsChallenge;
   private Map<String, String> extraHttpHeaders;
   private Map<String, String> extraQueryParameters;
   private String tenant;
   private int httpPollingTimeoutInSeconds;
   private boolean instanceAware;
   private long windowHandle;
   private PopParameters proofOfPossession;

   private InteractiveRequestParameters(
      URI redirectUri,
      ClaimsRequest claims,
      Set<String> scopes,
      Prompt prompt,
      String loginHint,
      String domainHint,
      SystemBrowserOptions systemBrowserOptions,
      String claimsChallenge,
      Map<String, String> extraHttpHeaders,
      Map<String, String> extraQueryParameters,
      String tenant,
      int httpPollingTimeoutInSeconds,
      boolean instanceAware,
      long windowHandle,
      PopParameters proofOfPossession
   ) {
      this.redirectUri = redirectUri;
      this.claims = claims;
      this.scopes = scopes;
      this.prompt = prompt;
      this.loginHint = loginHint;
      this.domainHint = domainHint;
      this.systemBrowserOptions = systemBrowserOptions;
      this.claimsChallenge = claimsChallenge;
      this.extraHttpHeaders = extraHttpHeaders;
      this.extraQueryParameters = extraQueryParameters;
      this.tenant = tenant;
      this.httpPollingTimeoutInSeconds = httpPollingTimeoutInSeconds;
      this.instanceAware = instanceAware;
      this.windowHandle = windowHandle;
      this.proofOfPossession = proofOfPossession;
   }

   private static InteractiveRequestParameters.InteractiveRequestParametersBuilder builder() {
      return new InteractiveRequestParameters.InteractiveRequestParametersBuilder();
   }

   public static InteractiveRequestParameters.InteractiveRequestParametersBuilder builder(URI redirectUri) {
      ParameterValidationUtils.validateNotNull("redirect_uri", redirectUri);
      return builder().redirectUri(redirectUri);
   }

   public URI redirectUri() {
      return this.redirectUri;
   }

   @Override
   public ClaimsRequest claims() {
      return this.claims;
   }

   @Override
   public Set<String> scopes() {
      return this.scopes;
   }

   public Prompt prompt() {
      return this.prompt;
   }

   public String loginHint() {
      return this.loginHint;
   }

   public String domainHint() {
      return this.domainHint;
   }

   public SystemBrowserOptions systemBrowserOptions() {
      return this.systemBrowserOptions;
   }

   public String claimsChallenge() {
      return this.claimsChallenge;
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

   public int httpPollingTimeoutInSeconds() {
      return this.httpPollingTimeoutInSeconds;
   }

   public boolean instanceAware() {
      return this.instanceAware;
   }

   public long windowHandle() {
      return this.windowHandle;
   }

   public PopParameters proofOfPossession() {
      return this.proofOfPossession;
   }

   void redirectUri(URI redirectUri) {
      this.redirectUri = redirectUri;
   }

   public static class InteractiveRequestParametersBuilder {
      private URI redirectUri;
      private ClaimsRequest claims;
      private Set<String> scopes;
      private Prompt prompt;
      private String loginHint;
      private String domainHint;
      private SystemBrowserOptions systemBrowserOptions;
      private String claimsChallenge;
      private Map<String, String> extraHttpHeaders;
      private Map<String, String> extraQueryParameters;
      private String tenant;
      private int httpPollingTimeoutInSeconds = 120;
      private boolean instanceAware;
      private long windowHandle;
      private PopParameters proofOfPossession;

      InteractiveRequestParametersBuilder() {
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder proofOfPossession(HttpMethod httpMethod, URI uri, String nonce) {
         this.proofOfPossession = new PopParameters(httpMethod, uri, nonce);
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder redirectUri(URI redirectUri) {
         ParameterValidationUtils.validateNotNull("redirectUri", redirectUri);
         this.redirectUri = redirectUri;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder claims(ClaimsRequest claims) {
         this.claims = claims;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder scopes(Set<String> scopes) {
         this.scopes = scopes;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder prompt(Prompt prompt) {
         this.prompt = prompt;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder loginHint(String loginHint) {
         this.loginHint = loginHint;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder domainHint(String domainHint) {
         this.domainHint = domainHint;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder systemBrowserOptions(SystemBrowserOptions systemBrowserOptions) {
         this.systemBrowserOptions = systemBrowserOptions;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder claimsChallenge(String claimsChallenge) {
         this.claimsChallenge = claimsChallenge;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
         this.extraHttpHeaders = extraHttpHeaders;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
         this.extraQueryParameters = extraQueryParameters;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder tenant(String tenant) {
         this.tenant = tenant;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder httpPollingTimeoutInSeconds(int httpPollingTimeoutInSeconds) {
         this.httpPollingTimeoutInSeconds = httpPollingTimeoutInSeconds;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder instanceAware(boolean instanceAware) {
         this.instanceAware = instanceAware;
         return this;
      }

      public InteractiveRequestParameters.InteractiveRequestParametersBuilder windowHandle(long windowHandle) {
         this.windowHandle = windowHandle;
         return this;
      }

      public InteractiveRequestParameters build() {
         return new InteractiveRequestParameters(
            this.redirectUri,
            this.claims,
            this.scopes,
            this.prompt,
            this.loginHint,
            this.domainHint,
            this.systemBrowserOptions,
            this.claimsChallenge,
            this.extraHttpHeaders,
            this.extraQueryParameters,
            this.tenant,
            this.httpPollingTimeoutInSeconds,
            this.instanceAware,
            this.windowHandle,
            this.proofOfPossession
         );
      }

      @Override
      public String toString() {
         return "InteractiveRequestParameters.InteractiveRequestParametersBuilder(redirectUri="
            + this.redirectUri
            + ", claims="
            + this.claims
            + ", scopes="
            + this.scopes
            + ", prompt="
            + this.prompt
            + ", loginHint="
            + this.loginHint
            + ", domainHint="
            + this.domainHint
            + ", systemBrowserOptions="
            + this.systemBrowserOptions
            + ", claimsChallenge="
            + this.claimsChallenge
            + ", extraHttpHeaders="
            + this.extraHttpHeaders
            + ", extraQueryParameters="
            + this.extraQueryParameters
            + ", tenant="
            + this.tenant
            + ", httpPollingTimeoutInSeconds="
            + this.httpPollingTimeoutInSeconds
            + ", instanceAware="
            + this.instanceAware
            + ", windowHandle="
            + this.windowHandle
            + ", proofOfPossession="
            + this.proofOfPossession
            + ")";
      }
   }
}
