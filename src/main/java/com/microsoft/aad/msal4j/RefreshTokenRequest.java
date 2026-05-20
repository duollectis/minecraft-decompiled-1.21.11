package com.microsoft.aad.msal4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class RefreshTokenRequest extends MsalRequest {
   private SilentRequest parentSilentRequest;
   private RefreshTokenParameters parameters;

   RefreshTokenRequest(RefreshTokenParameters parameters, AbstractApplicationBase application, RequestContext requestContext) {
      super(application, createAuthenticationGrant(parameters), requestContext);
      this.parameters = parameters;
   }

   RefreshTokenRequest(RefreshTokenParameters parameters, AbstractApplicationBase application, RequestContext requestContext, SilentRequest silentRequest) {
      this(parameters, application, requestContext);
      this.parentSilentRequest = silentRequest;
   }

   private static AbstractMsalAuthorizationGrant createAuthenticationGrant(RefreshTokenParameters parameters) {
      Map<String, String> params = new LinkedHashMap<>();
      params.put("grant_type", "refresh_token");
      params.put("refresh_token", parameters.refreshToken());
      return new OAuthAuthorizationGrant(params, parameters.scopes(), parameters.claims());
   }

   String getFullThumbprint() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.application().clientId() + ".");
      String authority = this.parentSilentRequest != null && this.parentSilentRequest.requestAuthority() != null
         ? this.parentSilentRequest.requestAuthority().authority()
         : this.application().authority();
      sb.append(authority + ".");
      if (this.parentSilentRequest != null && this.parentSilentRequest.parameters().account() != null) {
         sb.append(this.parentSilentRequest.parameters().account().homeAccountId() + ".");
      }

      sb.append(this.parameters.refreshToken() + ".");
      Set<String> sortedScopes = new TreeSet<>(this.parameters.scopes());
      sb.append(String.join(" ", sortedScopes) + ".");
      return StringHelper.createSha256Hash(sb.toString());
   }
}
