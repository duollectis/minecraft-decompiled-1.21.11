package com.microsoft.aad.msal4j;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

class InteractiveRequest extends MsalRequest {
   private AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference;
   private InteractiveRequestParameters interactiveRequestParameters;
   private String verifier;
   private String state;
   private PublicClientApplication publicClientApplication;
   private URL authorizationUrl;

   InteractiveRequest(
      InteractiveRequestParameters parameters,
      AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference,
      PublicClientApplication publicClientApplication,
      RequestContext requestContext
   ) {
      super(publicClientApplication, null, requestContext);
      this.interactiveRequestParameters = parameters;
      this.futureReference = futureReference;
      this.publicClientApplication = publicClientApplication;
      this.validateRedirectUrl(parameters.redirectUri());
   }

   URL authorizationUrl() {
      if (this.authorizationUrl == null) {
         this.authorizationUrl = this.createAuthorizationUrl();
      }

      return this.authorizationUrl;
   }

   private void validateRedirectUrl(URI redirectUri) {
      String host = redirectUri.getHost();
      String scheme = redirectUri.getScheme();
      if (scheme != null && scheme.equals("http")) {
         InetAddress address;
         try {
            address = InetAddress.getByName(host);
         } catch (UnknownHostException var6) {
            throw new MsalClientException(
               String.format(
                  "Unknown host exception for host \"%s\". For more information about redirect URI formats, see https://aka.ms/msal4j-interactive-request",
                  host
               ),
               "loopback_redirect_uri"
            );
         }

         if (address == null || !address.isLoopbackAddress()) {
            throw new MsalClientException(
               "Only loopback redirect URI is supported for interactive requests. For more information about redirect URI formats, see https://aka.ms/msal4j-interactive-request",
               "loopback_redirect_uri"
            );
         }
      } else {
         throw new MsalClientException(
            String.format(
               "Only http://localhost or http://localhost:port is supported for the redirect URI of an interactive request using a browser, but \"%s\" was found. For more information about redirect URI formats, see https://aka.ms/msal4j-interactive-request",
               scheme
            ),
            "loopback_redirect_uri"
         );
      }
   }

   private URL createAuthorizationUrl() {
      AuthorizationRequestUrlParameters.Builder authorizationRequestUrlBuilder = AuthorizationRequestUrlParameters.builder(
            this.interactiveRequestParameters.redirectUri().toString(), this.interactiveRequestParameters.scopes()
         )
         .prompt(this.interactiveRequestParameters.prompt())
         .claimsChallenge(this.interactiveRequestParameters.claimsChallenge())
         .loginHint(this.interactiveRequestParameters.loginHint())
         .domainHint(this.interactiveRequestParameters.domainHint())
         .correlationId(this.publicClientApplication.correlationId())
         .instanceAware(this.interactiveRequestParameters.instanceAware())
         .extraQueryParameters(this.interactiveRequestParameters.extraQueryParameters());
      this.addPkceAndState(authorizationRequestUrlBuilder);
      AuthorizationRequestUrlParameters authorizationRequestUrlParameters = authorizationRequestUrlBuilder.build();
      return this.publicClientApplication.getAuthorizationRequestUrl(authorizationRequestUrlParameters);
   }

   private void addPkceAndState(AuthorizationRequestUrlParameters.Builder builder) {
      SecureRandom secureRandom = new SecureRandom();
      byte[] randomBytes = new byte[32];
      secureRandom.nextBytes(randomBytes);
      this.verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
      this.state = UUID.randomUUID().toString() + UUID.randomUUID().toString();
      builder.codeChallenge(StringHelper.createBase64EncodedSha256Hash(this.verifier)).codeChallengeMethod("S256").state(this.state);
   }

   AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference() {
      return this.futureReference;
   }

   InteractiveRequestParameters interactiveRequestParameters() {
      return this.interactiveRequestParameters;
   }

   String verifier() {
      return this.verifier;
   }

   String state() {
      return this.state;
   }
}
