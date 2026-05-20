package com.microsoft.aad.msal4j;

import java.util.concurrent.CompletableFuture;

public interface IConfidentialClientApplication extends IClientApplicationBase {
   boolean sendX5c();

   CompletableFuture<IAuthenticationResult> acquireToken(ClientCredentialParameters var1);

   CompletableFuture<IAuthenticationResult> acquireToken(OnBehalfOfParameters var1);
}
