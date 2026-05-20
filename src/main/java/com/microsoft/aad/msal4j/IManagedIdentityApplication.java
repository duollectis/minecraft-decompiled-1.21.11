package com.microsoft.aad.msal4j;

import java.util.concurrent.CompletableFuture;

public interface IManagedIdentityApplication extends IApplicationBase {
   CompletableFuture<IAuthenticationResult> acquireTokenForManagedIdentity(ManagedIdentityParameters var1) throws Exception;
}
