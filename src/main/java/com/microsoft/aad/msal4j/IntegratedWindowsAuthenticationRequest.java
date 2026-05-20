package com.microsoft.aad.msal4j;

class IntegratedWindowsAuthenticationRequest extends MsalRequest {
   IntegratedWindowsAuthenticationRequest(
      IntegratedWindowsAuthenticationParameters parameters, PublicClientApplication application, RequestContext requestContext
   ) {
      super(application, createAuthenticationGrant(parameters), requestContext);
   }

   private static AbstractMsalAuthorizationGrant createAuthenticationGrant(IntegratedWindowsAuthenticationParameters parameters) {
      return new IntegratedWindowsAuthorizationGrant(parameters.scopes(), parameters.username(), parameters.claims());
   }
}
