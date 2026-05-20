package com.microsoft.aad.msal4j;

public enum ManagedIdentitySourceType {
   NONE,
   IMDS,
   APP_SERVICE,
   AZURE_ARC,
   CLOUD_SHELL,
   SERVICE_FABRIC,
   DEFAULT_TO_IMDS;
}
