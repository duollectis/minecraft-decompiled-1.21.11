package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

class IntegratedWindowsAuthorizationGrant extends AbstractMsalAuthorizationGrant {
   private final String userName;

   IntegratedWindowsAuthorizationGrant(Set<String> scopes, String userName, ClaimsRequest claims) {
      this.userName = userName;
      this.scopes = scopes;
      this.claims = claims;
   }

   @Override
   Map<String, String> toParameters() {
      return null;
   }

   String getUserName() {
      return this.userName;
   }
}
