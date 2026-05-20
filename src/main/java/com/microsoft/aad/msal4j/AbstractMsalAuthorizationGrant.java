package com.microsoft.aad.msal4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class AbstractMsalAuthorizationGrant {
   static final String SCOPE_PARAM_NAME = "scope";
   static final String SCOPES_DELIMITER = " ";
   static final String SCOPE_OPEN_ID = "openid";
   static final String SCOPE_PROFILE = "profile";
   static final String SCOPE_OFFLINE_ACCESS = "offline_access";
   static final Set<String> COMMON_SCOPES = Stream.of("openid", "profile", "offline_access").collect(Collectors.toCollection(HashSet::new));
   Set<String> scopes;
   ClaimsRequest claims;

   abstract Map<String, String> toParameters();

   Set<String> getScopes() {
      return this.scopes;
   }

   ClaimsRequest getClaims() {
      return this.claims;
   }
}
