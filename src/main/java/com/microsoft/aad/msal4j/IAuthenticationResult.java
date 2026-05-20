package com.microsoft.aad.msal4j;

import java.io.Serializable;
import java.util.Date;

public interface IAuthenticationResult extends Serializable {
   String accessToken();

   String idToken();

   IAccount account();

   ITenantProfile tenantProfile();

   String environment();

   String scopes();

   Date expiresOnDate();

   default AuthenticationResultMetadata metadata() {
      return AuthenticationResultMetadata.builder().build();
   }
}
