package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

interface IAcquireTokenParameters {
   Set<String> scopes();

   ClaimsRequest claims();

   Map<String, String> extraHttpHeaders();

   String tenant();

   Map<String, String> extraQueryParameters();
}
