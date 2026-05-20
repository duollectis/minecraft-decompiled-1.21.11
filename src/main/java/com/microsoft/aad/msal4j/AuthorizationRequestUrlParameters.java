package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationRequestUrlParameters {
   private String redirectUri;
   private Set<String> scopes;
   private String codeChallenge;
   private String codeChallengeMethod;
   private String state;
   private String nonce;
   private ResponseMode responseMode;
   private String loginHint;
   private String domainHint;
   private Prompt prompt;
   private String correlationId;
   private boolean instanceAware;
   private static final String ADMIN_CONSENT_ENDPOINT = "https://login.microsoftonline.com/{tenant}/adminconsent";
   Map<String, String> extraQueryParameters;
   Map<String, String> requestParameters = new HashMap<>();
   Logger log = LoggerFactory.getLogger(AuthorizationRequestUrlParameters.class);

   public static AuthorizationRequestUrlParameters.Builder builder(String redirectUri, Set<String> scopes) {
      ParameterValidationUtils.validateNotBlank("redirect_uri", redirectUri);
      ParameterValidationUtils.validateNotNull("scopes", scopes);
      return builder().redirectUri(redirectUri).scopes(scopes);
   }

   private static AuthorizationRequestUrlParameters.Builder builder() {
      return new AuthorizationRequestUrlParameters.Builder();
   }

   private AuthorizationRequestUrlParameters(AuthorizationRequestUrlParameters.Builder builder) {
      this.redirectUri = builder.redirectUri;
      this.requestParameters.put("redirect_uri", this.redirectUri);
      this.scopes = builder.scopes;
      Set<String> scopesParam = new LinkedHashSet<>(AbstractMsalAuthorizationGrant.COMMON_SCOPES);
      scopesParam.addAll(builder.scopes);
      if (builder.extraScopesToConsent != null) {
         scopesParam.addAll(builder.extraScopesToConsent);
      }

      this.scopes = scopesParam;
      this.requestParameters.put("scope", String.join(" ", scopesParam));
      this.requestParameters.put("response_type", "code");
      if (builder.claims != null) {
         String claimsParam = String.join(" ", builder.claims);
         this.requestParameters.put("claims", claimsParam);
      }

      if (builder.claimsChallenge != null && builder.claimsChallenge.trim().length() > 0) {
         JsonHelper.validateJsonFormat(builder.claimsChallenge);
         this.requestParameters.put("claims", builder.claimsChallenge);
      }

      if (builder.claimsRequest != null) {
         String claimsRequest = builder.claimsRequest.formatAsJSONString();
         if (this.requestParameters.get("claims") != null) {
            claimsRequest = JsonHelper.mergeJSONString(claimsRequest, this.requestParameters.get("claims"));
         }

         this.requestParameters.put("claims", claimsRequest);
      }

      if (builder.codeChallenge != null) {
         this.codeChallenge = builder.codeChallenge;
         this.requestParameters.put("code_challenge", builder.codeChallenge);
      }

      if (builder.codeChallengeMethod != null) {
         this.codeChallengeMethod = builder.codeChallengeMethod;
         this.requestParameters.put("code_challenge_method", builder.codeChallengeMethod);
      }

      if (builder.state != null) {
         this.state = builder.state;
         this.requestParameters.put("state", builder.state);
      }

      if (builder.nonce != null) {
         this.nonce = builder.nonce;
         this.requestParameters.put("nonce", builder.nonce);
      }

      if (builder.responseMode != null) {
         this.responseMode = builder.responseMode;
         this.requestParameters.put("response_mode", builder.responseMode.toString());
      } else {
         this.responseMode = ResponseMode.FORM_POST;
         this.requestParameters.put("response_mode", ResponseMode.FORM_POST.toString());
      }

      if (builder.loginHint != null) {
         this.loginHint = this.loginHint();
         this.requestParameters.put("login_hint", builder.loginHint);
         this.requestParameters.put("X-AnchorMailbox", String.format("upn:%s", builder.loginHint));
      }

      if (builder.domainHint != null) {
         this.domainHint = this.domainHint();
         this.requestParameters.put("domain_hint", builder.domainHint);
      }

      if (builder.prompt != null) {
         this.prompt = builder.prompt;
         this.requestParameters.put("prompt", builder.prompt.toString());
      }

      if (builder.correlationId != null) {
         this.correlationId = builder.correlationId;
         this.requestParameters.put("correlation_id", builder.correlationId);
      }

      if (builder.instanceAware) {
         this.instanceAware = builder.instanceAware;
         this.requestParameters.put("instance_aware", String.valueOf(this.instanceAware));
      }

      if (null != builder.extraQueryParameters && !builder.extraQueryParameters.isEmpty()) {
         this.extraQueryParameters = builder.extraQueryParameters;

         for (Entry<String, String> entry : this.extraQueryParameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (this.requestParameters.containsKey(key)) {
               this.log.warn("A query parameter {} has been provided with values multiple times.", key);
            }

            this.requestParameters.put(key, value);
         }
      }
   }

   URL createAuthorizationURL(Authority authority, Map<String, String> requestParameters) {
      try {
         String authorizationCodeEndpoint;
         if (this.prompt == Prompt.ADMIN_CONSENT) {
            authorizationCodeEndpoint = "https://login.microsoftonline.com/{tenant}/adminconsent".replace("{tenant}", authority.tenant);
         } else {
            authorizationCodeEndpoint = authority.authorizationEndpoint();
         }

         String uriString = authorizationCodeEndpoint + "?" + StringHelper.serializeQueryParameters(requestParameters);
         return new URL(uriString);
      } catch (MalformedURLException var6) {
         throw new MsalClientException(var6);
      }
   }

   public String redirectUri() {
      return this.redirectUri;
   }

   public Set<String> scopes() {
      return this.scopes;
   }

   public String codeChallenge() {
      return this.codeChallenge;
   }

   public String codeChallengeMethod() {
      return this.codeChallengeMethod;
   }

   public String state() {
      return this.state;
   }

   public String nonce() {
      return this.nonce;
   }

   public ResponseMode responseMode() {
      return this.responseMode;
   }

   public String loginHint() {
      return this.loginHint;
   }

   public String domainHint() {
      return this.domainHint;
   }

   public Prompt prompt() {
      return this.prompt;
   }

   public String correlationId() {
      return this.correlationId;
   }

   public boolean instanceAware() {
      return this.instanceAware;
   }

   public Map<String, String> extraQueryParameters() {
      return this.extraQueryParameters;
   }

   public Map<String, List<String>> requestParameters() {
      return StringHelper.convertToMultiValueMap(this.requestParameters);
   }

   public Logger log() {
      return this.log;
   }

   public static class Builder {
      private String redirectUri;
      private Set<String> scopes;
      private Set<String> extraScopesToConsent;
      private Set<String> claims;
      private String claimsChallenge;
      private ClaimsRequest claimsRequest;
      private String codeChallenge;
      private String codeChallengeMethod;
      private String state;
      private String nonce;
      private ResponseMode responseMode;
      private String loginHint;
      private String domainHint;
      private Prompt prompt;
      private String correlationId;
      private boolean instanceAware;
      private Map<String, String> extraQueryParameters;

      public AuthorizationRequestUrlParameters build() {
         return new AuthorizationRequestUrlParameters(this);
      }

      private AuthorizationRequestUrlParameters.Builder self() {
         return this;
      }

      public AuthorizationRequestUrlParameters.Builder redirectUri(String val) {
         ParameterValidationUtils.validateNotNull("redirectUri", val);
         this.redirectUri = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder scopes(Set<String> val) {
         ParameterValidationUtils.validateNotNull("scopes", val);
         this.scopes = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder extraScopesToConsent(Set<String> val) {
         this.extraScopesToConsent = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder claimsChallenge(String val) {
         this.claimsChallenge = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder claims(ClaimsRequest val) {
         this.claimsRequest = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder codeChallenge(String val) {
         this.codeChallenge = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder codeChallengeMethod(String val) {
         this.codeChallengeMethod = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder state(String val) {
         this.state = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder nonce(String val) {
         this.nonce = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder responseMode(ResponseMode val) {
         this.responseMode = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder loginHint(String val) {
         this.loginHint = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder domainHint(String val) {
         this.domainHint = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder prompt(Prompt val) {
         this.prompt = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder correlationId(String val) {
         this.correlationId = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder instanceAware(boolean val) {
         this.instanceAware = val;
         return this.self();
      }

      public AuthorizationRequestUrlParameters.Builder extraQueryParameters(Map<String, String> val) {
         this.extraQueryParameters = val;
         return this.self();
      }
   }
}
