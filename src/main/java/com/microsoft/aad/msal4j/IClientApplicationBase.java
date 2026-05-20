package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

interface IClientApplicationBase extends IApplicationBase {
   String clientId();

   String authority();

   boolean validateAuthority();

   URL getAuthorizationRequestUrl(AuthorizationRequestUrlParameters var1);

   CompletableFuture<IAuthenticationResult> acquireToken(AuthorizationCodeParameters var1);

   CompletableFuture<IAuthenticationResult> acquireToken(RefreshTokenParameters var1);

   CompletableFuture<IAuthenticationResult> acquireTokenSilently(SilentParameters var1) throws MalformedURLException;

   CompletableFuture<Set<IAccount>> getAccounts();

   CompletableFuture removeAccount(IAccount var1);
}
