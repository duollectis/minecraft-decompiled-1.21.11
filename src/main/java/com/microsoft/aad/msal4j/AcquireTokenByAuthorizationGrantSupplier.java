package com.microsoft.aad.msal4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

class AcquireTokenByAuthorizationGrantSupplier extends AuthenticationResultSupplier {
   private Authority requestAuthority;
   private MsalRequest msalRequest;

   AcquireTokenByAuthorizationGrantSupplier(AbstractApplicationBase clientApplication, MsalRequest msalRequest, Authority authority) {
      super(clientApplication, msalRequest);
      this.msalRequest = msalRequest;
      this.requestAuthority = authority;
   }

   @Override
   AuthenticationResult execute() throws Exception {
      AbstractMsalAuthorizationGrant authGrant = this.msalRequest.msalAuthorizationGrant();
      if (this.IsUiRequiredCacheSupported()) {
         MsalInteractionRequiredException cachedEx = InteractionRequiredCache.getCachedInteractionRequiredException(
            ((RefreshTokenRequest)this.msalRequest).getFullThumbprint()
         );
         if (cachedEx != null) {
            throw cachedEx;
         }
      }

      if (authGrant instanceof OAuthAuthorizationGrant) {
         this.processPasswordGrant((OAuthAuthorizationGrant)authGrant);
      }

      if (authGrant instanceof IntegratedWindowsAuthorizationGrant) {
         IntegratedWindowsAuthorizationGrant integratedAuthGrant = (IntegratedWindowsAuthorizationGrant)authGrant;
         this.msalRequest.msalAuthorizationGrant = new OAuthAuthorizationGrant(
            this.getAuthorizationGrantIntegrated(integratedAuthGrant.getUserName()), integratedAuthGrant.getScopes(), integratedAuthGrant.getClaims()
         );
      }

      if (this.requestAuthority == null) {
         this.requestAuthority = this.clientApplication.authenticationAuthority;
      }

      this.requestAuthority = this.getAuthorityWithPrefNetworkHost(this.requestAuthority.authority());

      try {
         return this.clientApplication.acquireTokenCommon(this.msalRequest, this.requestAuthority);
      } catch (MsalInteractionRequiredException var3) {
         if (this.IsUiRequiredCacheSupported()) {
            InteractionRequiredCache.set(((RefreshTokenRequest)this.msalRequest).getFullThumbprint(), var3);
         }

         throw var3;
      }
   }

   private boolean IsUiRequiredCacheSupported() {
      return this.msalRequest instanceof RefreshTokenRequest && this.clientApplication instanceof PublicClientApplication;
   }

   private void processPasswordGrant(OAuthAuthorizationGrant authGrant) throws Exception {
      if (authGrant.getParamValue("grant_type").equals("password") && this.msalRequest.application().authenticationAuthority.authorityType == AuthorityType.AAD
         )
       {
         UserDiscoveryResponse userDiscoveryResponse = UserDiscoveryRequest.execute(
            this.clientApplication.authenticationAuthority.getUserRealmEndpoint(authGrant.getParamValue("username")),
            this.msalRequest.headers().getReadonlyHeaderMap(),
            this.msalRequest.requestContext(),
            this.clientApplication.serviceBundle()
         );
         if (userDiscoveryResponse.isAccountFederated()) {
            WSTrustResponse response = WSTrustRequest.execute(
               userDiscoveryResponse.federationMetadataUrl(),
               authGrant.getParamValue("username"),
               authGrant.getParamValue("password"),
               userDiscoveryResponse.cloudAudienceUrn(),
               this.msalRequest.requestContext(),
               this.clientApplication.serviceBundle(),
               this.clientApplication.logPii()
            );
            authGrant.addAndReplaceParams(this.getSAMLAuthGrantParameters(response));
         }
      }
   }

   private Map<String, String> getSAMLAuthGrantParameters(WSTrustResponse response) {
      Map<String, String> params = new LinkedHashMap<>();
      if (response.isTokenSaml2()) {
         params.put("grant_type", "urn:ietf:params:oauth:grant-type:saml2-bearer");
      } else {
         params.put("grant_type", "urn:ietf:params:oauth:grant-type:saml1_1-bearer");
      }

      params.put("assertion", Base64.getUrlEncoder().encodeToString(response.getToken().getBytes(StandardCharsets.UTF_8)));
      return params;
   }

   private Map<String, String> getAuthorizationGrantIntegrated(String userName) throws Exception {
      String userRealmEndpoint = this.clientApplication
         .authenticationAuthority
         .getUserRealmEndpoint(URLEncoder.encode(userName, StandardCharsets.UTF_8.name()));
      UserDiscoveryResponse userRealmResponse = UserDiscoveryRequest.execute(
         userRealmEndpoint, this.msalRequest.headers().getReadonlyHeaderMap(), this.msalRequest.requestContext(), this.clientApplication.serviceBundle()
      );
      if (userRealmResponse.isAccountFederated() && "WSTrust".equalsIgnoreCase(userRealmResponse.federationProtocol())) {
         String mexURL = userRealmResponse.federationMetadataUrl();
         String cloudAudienceUrn = userRealmResponse.cloudAudienceUrn();
         WSTrustResponse wsTrustResponse = WSTrustRequest.execute(
            mexURL, cloudAudienceUrn, this.msalRequest.requestContext(), this.clientApplication.serviceBundle(), this.clientApplication.logPii()
         );
         return this.getSAMLAuthGrantParameters(wsTrustResponse);
      } else if (userRealmResponse.isAccountManaged()) {
         throw new MsalClientException("Password is required for managed user", "password_required_for_managed_user");
      } else {
         throw new MsalClientException("User Realm request failed", "user_realm_discovery_failed");
      }
   }
}
