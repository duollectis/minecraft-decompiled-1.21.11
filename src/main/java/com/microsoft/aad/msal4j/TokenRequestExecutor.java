package com.microsoft.aad.msal4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TokenRequestExecutor {
   Logger log = LoggerFactory.getLogger(TokenRequestExecutor.class);
   final Authority requestAuthority;
   final String tenant;
   private final MsalRequest msalRequest;
   private final ServiceBundle serviceBundle;

   TokenRequestExecutor(Authority requestAuthority, MsalRequest msalRequest, ServiceBundle serviceBundle) {
      this.requestAuthority = requestAuthority;
      this.serviceBundle = serviceBundle;
      this.msalRequest = msalRequest;
      this.tenant = msalRequest.requestContext().apiParameters().tenant() == null
         ? msalRequest.application().tenant()
         : msalRequest.requestContext().apiParameters().tenant();
   }

   AuthenticationResult executeTokenRequest() throws IOException {
      this.log.debug("Sending token request to: {}", this.requestAuthority.canonicalAuthorityUrl());
      OAuthHttpRequest oAuthHttpRequest = this.createOauthHttpRequest();
      HttpResponse oauthHttpResponse = oAuthHttpRequest.send();
      return this.createAuthenticationResultFromOauthHttpResponse(oauthHttpResponse);
   }

   OAuthHttpRequest createOauthHttpRequest() throws MalformedURLException {
      if (this.requestAuthority.tokenEndpointUrl() == null) {
         throw new MsalClientException("The endpoint URI is not specified", "invalid_endpoint_uri");
      } else {
         OAuthHttpRequest oauthHttpRequest = new OAuthHttpRequest(
            HttpMethod.POST,
            this.requestAuthority.tokenEndpointUrl(),
            this.msalRequest.headers().getReadonlyHeaderMap(),
            this.msalRequest.requestContext(),
            this.serviceBundle
         );
         Map<String, String> params = new HashMap<>(this.msalRequest.msalAuthorizationGrant().toParameters());
         if (this.msalRequest.application() instanceof AbstractClientApplicationBase
            && ((AbstractClientApplicationBase)this.msalRequest.application()).clientCapabilities() != null) {
            params.put("claims", ((AbstractClientApplicationBase)this.msalRequest.application()).clientCapabilities());
         }

         if (this.msalRequest.msalAuthorizationGrant.getClaims() != null) {
            String claimsRequest = this.msalRequest.msalAuthorizationGrant.getClaims().formatAsJSONString();
            if (params.get("claims") != null) {
               claimsRequest = JsonHelper.mergeJSONString(params.get("claims"), claimsRequest);
            }

            params.put("claims", claimsRequest);
         }

         if (this.msalRequest.requestContext().apiParameters().extraQueryParameters() != null) {
            for (String key : this.msalRequest.requestContext().apiParameters().extraQueryParameters().keySet()) {
               if (params.containsKey(key)) {
                  this.log.warn("A query parameter {} has been provided with values multiple times.", key);
               }

               params.put(key, this.msalRequest.requestContext().apiParameters().extraQueryParameters().get(key));
            }
         }

         oauthHttpRequest.setQuery(StringHelper.serializeQueryParameters(params));
         if (this.msalRequest.application() instanceof AbstractClientApplicationBase) {
            this.addQueryParameters(oauthHttpRequest);
         }

         return oauthHttpRequest;
      }
   }

   private void addQueryParameters(OAuthHttpRequest oauthHttpRequest) {
      Map<String, String> queryParameters = StringHelper.parseQueryParameters(oauthHttpRequest.query);
      String clientID = this.msalRequest.application().clientId();
      queryParameters.put("client_id", clientID);
      if (this.msalRequest.application() instanceof ConfidentialClientApplication) {
         ConfidentialClientApplication application = (ConfidentialClientApplication)this.msalRequest.application();
         this.addCredentialToRequest(queryParameters, application);
      }

      oauthHttpRequest.setQuery(StringHelper.serializeQueryParameters(queryParameters));
   }

   private void addCredentialToRequest(Map<String, String> queryParameters, ConfidentialClientApplication application) {
      IClientCredential credentialToUse = application.clientCredential;
      Authority authorityToUse = application.authenticationAuthority;
      if (this.msalRequest instanceof ClientCredentialRequest) {
         ClientCredentialParameters parameters = ((ClientCredentialRequest)this.msalRequest).parameters;
         if (parameters.clientCredential() != null) {
            credentialToUse = parameters.clientCredential();
         }

         if (parameters.tenant() != null) {
            try {
               authorityToUse = Authority.replaceTenant(authorityToUse, parameters.tenant());
            } catch (MalformedURLException var7) {
               this.log.warn("Could not create authority with tenant override: {}", var7.getMessage());
            }
         }
      }

      if (credentialToUse != null) {
         if (credentialToUse instanceof ClientSecret) {
            queryParameters.put("client_secret", ((ClientSecret)credentialToUse).clientSecret());
         } else if (credentialToUse instanceof ClientAssertion) {
            this.addJWTBearerAssertionParams(queryParameters, ((ClientAssertion)credentialToUse).assertion());
         } else if (credentialToUse instanceof ClientCertificate) {
            ClientCertificate certificate = (ClientCertificate)credentialToUse;
            String assertion = certificate.getAssertion(authorityToUse, application.clientId(), application.sendX5c());
            this.addJWTBearerAssertionParams(queryParameters, assertion);
         }
      }
   }

   private void addJWTBearerAssertionParams(Map<String, String> queryParameters, String assertion) {
      queryParameters.put("client_assertion", assertion);
      queryParameters.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
   }

   private AuthenticationResult createAuthenticationResultFromOauthHttpResponse(HttpResponse oauthHttpResponse) {
      if (oauthHttpResponse.statusCode() == 200) {
         TokenResponse response = TokenResponse.parseHttpResponse(oauthHttpResponse);
         AccountCacheEntity accountCacheEntity = null;
         if (!StringHelper.isNullOrBlank(response.idToken())) {
            IdToken idToken = JsonHelper.createIdTokenFromEncodedTokenString(response.idToken());
            AuthorityType type = this.msalRequest.application().authenticationAuthority.authorityType;
            if (!StringHelper.isBlank(response.getClientInfo())) {
               if (type == AuthorityType.B2C) {
                  B2CAuthority authority = (B2CAuthority)this.msalRequest.application().authenticationAuthority;
                  accountCacheEntity = AccountCacheEntity.create(response.getClientInfo(), this.requestAuthority, idToken, authority.policy());
               } else {
                  accountCacheEntity = AccountCacheEntity.create(response.getClientInfo(), this.requestAuthority, idToken);
               }
            } else if (type == AuthorityType.ADFS) {
               accountCacheEntity = AccountCacheEntity.createADFSAccount(this.requestAuthority, idToken);
            }
         }

         long currTimestampSec = new Date().getTime() / 1000L;
         return AuthenticationResult.builder()
            .accessToken(response.accessToken())
            .refreshToken(response.refreshToken())
            .familyId(response.getFoci())
            .idToken(response.idToken())
            .environment(this.requestAuthority.host())
            .expiresOn(currTimestampSec + response.getExpiresIn())
            .extExpiresOn(response.getExtExpiresIn() > 0L ? currTimestampSec + response.getExtExpiresIn() : 0L)
            .refreshOn(response.getRefreshIn() > 0L ? currTimestampSec + response.getRefreshIn() : 0L)
            .accountCacheEntity(accountCacheEntity)
            .scopes(response.getScope())
            .metadata(
               AuthenticationResultMetadata.builder()
                  .tokenSource(TokenSource.IDENTITY_PROVIDER)
                  .refreshOn(response.getRefreshIn() > 0L ? currTimestampSec + response.getRefreshIn() : 0L)
                  .build()
            )
            .build();
      } else {
         if (oauthHttpResponse.statusCode() == 429 || oauthHttpResponse.statusCode() >= 500) {
            this.serviceBundle.getServerSideTelemetry().previousRequests.putAll(this.serviceBundle.getServerSideTelemetry().previousRequestInProgress);
         }

         throw MsalServiceExceptionFactory.fromHttpResponse(oauthHttpResponse);
      }
   }

   Logger getLog() {
      return this.log;
   }

   Authority getRequestAuthority() {
      return this.requestAuthority;
   }

   String getTenant() {
      return this.tenant;
   }

   MsalRequest getMsalRequest() {
      return this.msalRequest;
   }

   ServiceBundle getServiceBundle() {
      return this.serviceBundle;
   }
}
