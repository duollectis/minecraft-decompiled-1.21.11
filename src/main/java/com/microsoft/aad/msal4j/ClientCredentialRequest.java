package com.microsoft.aad.msal4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

class ClientCredentialRequest extends MsalRequest {
   ClientCredentialParameters parameters;
   Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider;

   ClientCredentialRequest(
      ClientCredentialParameters parameters,
      ConfidentialClientApplication application,
      RequestContext requestContext,
      Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider
   ) {
      super(application, createMsalGrant(parameters), requestContext);
      this.parameters = parameters;
      this.appTokenProvider = appTokenProvider;
   }

   private static OAuthAuthorizationGrant createMsalGrant(ClientCredentialParameters parameters) {
      Map<String, String> params = new LinkedHashMap<>();
      params.put("grant_type", "client_credentials");
      return new OAuthAuthorizationGrant(params, parameters.scopes(), parameters.claims());
   }
}
