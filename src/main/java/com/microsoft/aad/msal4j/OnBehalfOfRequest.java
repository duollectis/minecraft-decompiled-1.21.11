package com.microsoft.aad.msal4j;

import java.util.LinkedHashMap;
import java.util.Map;

class OnBehalfOfRequest extends MsalRequest {
   OnBehalfOfParameters parameters;

   OnBehalfOfRequest(OnBehalfOfParameters parameters, ConfidentialClientApplication application, RequestContext requestContext) {
      super(application, createAuthenticationGrant(parameters), requestContext);
      this.parameters = parameters;
   }

   private static OAuthAuthorizationGrant createAuthenticationGrant(OnBehalfOfParameters parameters) {
      Map<String, String> params = new LinkedHashMap<>();
      params.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
      params.put("assertion", parameters.userAssertion().getAssertion());
      params.put("requested_token_use", "on_behalf_of");
      if (parameters.claims() != null) {
         params.put("claims", parameters.claims().formatAsJSONString());
      }

      return new OAuthAuthorizationGrant(params, parameters.scopes());
   }

   OnBehalfOfParameters parameters() {
      return this.parameters;
   }
}
