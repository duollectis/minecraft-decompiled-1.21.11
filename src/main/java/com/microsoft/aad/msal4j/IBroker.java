package com.microsoft.aad.msal4j;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

public interface IBroker {
   default CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, SilentParameters requestParameters) {
      throw new MsalClientException("Broker implementation missing", "missing_broker");
   }

   default CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, InteractiveRequestParameters parameters) {
      throw new MsalClientException("Broker implementation missing", "missing_broker");
   }

   default CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, UserNamePasswordParameters parameters) {
      throw new MsalClientException("Broker implementation missing", "missing_broker");
   }

   default void removeAccount(PublicClientApplication application, IAccount account) throws MsalClientException {
      throw new MsalClientException("Broker implementation missing", "missing_broker");
   }

   default boolean isBrokerAvailable() {
      throw new MsalClientException("Broker implementation missing", "missing_broker");
   }

   default IAuthenticationResult parseBrokerAuthResult(
      String authority, String idToken, String accessToken, String accountId, String clientInfo, long accessTokenExpirationTime, boolean isPopAuthorization
   ) {
      AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();

      try {
         if (idToken != null) {
            builder.idToken(idToken);
            if (accountId != null) {
               IdToken idTokenObj = JsonHelper.createIdTokenFromEncodedTokenString(idToken);
               builder.accountCacheEntity(AccountCacheEntity.create(clientInfo, Authority.createAuthority(new URL(authority)), idTokenObj, null));
            }
         }

         if (accessToken != null) {
            builder.accessToken(accessToken);
            builder.expiresOn(accessTokenExpirationTime);
         }

         builder.isPopAuthorization(isPopAuthorization);
      } catch (Exception var11) {
         throw new MsalClientException(
            String.format("Exception when converting broker result to MSAL Java AuthenticationResult: %s", var11.getMessage()), "brokers_package_error"
         );
      }

      return builder.build();
   }
}
