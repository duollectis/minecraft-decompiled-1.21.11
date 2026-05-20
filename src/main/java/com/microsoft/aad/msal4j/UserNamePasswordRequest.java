package com.microsoft.aad.msal4j;

import java.util.LinkedHashMap;
import java.util.Map;

class UserNamePasswordRequest extends MsalRequest {
   UserNamePasswordRequest(UserNamePasswordParameters parameters, PublicClientApplication application, RequestContext requestContext) {
      super(application, createAuthenticationGrant(parameters), requestContext);
   }

   private static OAuthAuthorizationGrant createAuthenticationGrant(UserNamePasswordParameters parameters) {
      Map<String, String> params = new LinkedHashMap<>();
      params.put("grant_type", "password");
      params.put("username", parameters.username());
      params.put("password", new String(parameters.password()));
      return new OAuthAuthorizationGrant(params, parameters.scopes(), parameters.claims());
   }
}
