package com.microsoft.aad.msal4j;

import java.io.Serializable;
import java.util.Map;

public interface ITenantProfile extends Serializable {
   Map<String, ?> getClaims();

   String environment();
}
