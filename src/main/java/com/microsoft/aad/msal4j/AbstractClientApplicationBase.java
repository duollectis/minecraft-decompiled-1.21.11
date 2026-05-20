package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.SSLSocketFactory;

public abstract class AbstractClientApplicationBase extends AbstractApplicationBase implements IClientApplicationBase {
   private String clientId;
   private String authority;
   private boolean validateAuthority;
   private String applicationName;
   private String applicationVersion;
   private AadInstanceDiscoveryResponse aadAadInstanceDiscoveryResponse;
   private String clientCapabilities;
   private boolean autoDetectRegion;
   protected String azureRegion;
   private boolean instanceDiscovery;

   @Override
   public TokenCache tokenCache() {
      return super.tokenCache;
   }

   @Override
   public CompletableFuture<IAuthenticationResult> acquireToken(AuthorizationCodeParameters parameters) {
      ParameterValidationUtils.validateNotNull("parameters", parameters);
      RequestContext context = new RequestContext(this, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters);
      AuthorizationCodeRequest authorizationCodeRequest = new AuthorizationCodeRequest(parameters, this, context);
      return this.executeRequest(authorizationCodeRequest);
   }

   @Override
   public CompletableFuture<IAuthenticationResult> acquireToken(RefreshTokenParameters parameters) {
      ParameterValidationUtils.validateNotNull("parameters", parameters);
      RequestContext context = new RequestContext(this, PublicApi.ACQUIRE_TOKEN_BY_REFRESH_TOKEN, parameters);
      RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(parameters, this, context);
      return this.executeRequest(refreshTokenRequest);
   }

   @Override
   public CompletableFuture<IAuthenticationResult> acquireTokenSilently(SilentParameters parameters) throws MalformedURLException {
      ParameterValidationUtils.validateNotNull("parameters", parameters);
      RequestContext context;
      if (parameters.account() != null) {
         context = new RequestContext(
            this, PublicApi.ACQUIRE_TOKEN_SILENTLY, parameters, UserIdentifier.fromHomeAccountId(parameters.account().homeAccountId())
         );
      } else {
         context = new RequestContext(this, PublicApi.ACQUIRE_TOKEN_SILENTLY, parameters);
      }

      SilentRequest silentRequest = new SilentRequest(parameters, this, context, null);
      return this.executeRequest(silentRequest);
   }

   @Override
   public CompletableFuture<Set<IAccount>> getAccounts() {
      RequestContext context = new RequestContext(this, PublicApi.GET_ACCOUNTS, null);
      MsalRequest msalRequest = new MsalRequest(this, null, context) {};
      AccountsSupplier supplier = new AccountsSupplier(this, msalRequest);
      return super.serviceBundle().getExecutorService() != null
         ? CompletableFuture.supplyAsync(supplier, super.serviceBundle().getExecutorService())
         : CompletableFuture.supplyAsync(supplier);
   }

   @Override
   public CompletableFuture<Void> removeAccount(IAccount account) {
      RequestContext context = new RequestContext(this, PublicApi.REMOVE_ACCOUNTS, null);
      MsalRequest msalRequest = new MsalRequest(this, null, context) {};
      RemoveAccountRunnable runnable = new RemoveAccountRunnable(msalRequest, account);
      return super.serviceBundle().getExecutorService() != null
         ? CompletableFuture.runAsync(runnable, super.serviceBundle().getExecutorService())
         : CompletableFuture.runAsync(runnable);
   }

   @Override
   public URL getAuthorizationRequestUrl(AuthorizationRequestUrlParameters parameters) {
      ParameterValidationUtils.validateNotNull("parameters", parameters);
      parameters.requestParameters.put("client_id", this.clientId);
      if (this.clientCapabilities != null) {
         if (parameters.requestParameters.containsKey("claims")) {
            String claims = String.valueOf(parameters.requestParameters.get("claims"));
            String mergedClaimsCapabilities = JsonHelper.mergeJSONString(claims, this.clientCapabilities);
            parameters.requestParameters.put("claims", mergedClaimsCapabilities);
         } else {
            parameters.requestParameters.put("claims", this.clientCapabilities);
         }
      }

      return parameters.createAuthorizationURL(this.authenticationAuthority, parameters.requestParameters);
   }

   @Override
   public String clientId() {
      return this.clientId;
   }

   @Override
   public String authority() {
      return this.authority;
   }

   @Override
   public boolean validateAuthority() {
      return this.validateAuthority;
   }

   public String applicationName() {
      return this.applicationName;
   }

   public String applicationVersion() {
      return this.applicationVersion;
   }

   public AadInstanceDiscoveryResponse aadAadInstanceDiscoveryResponse() {
      return this.aadAadInstanceDiscoveryResponse;
   }

   public String clientCapabilities() {
      return this.clientCapabilities;
   }

   public boolean autoDetectRegion() {
      return this.autoDetectRegion;
   }

   public String azureRegion() {
      return this.azureRegion;
   }

   public boolean instanceDiscovery() {
      return this.instanceDiscovery;
   }

   AbstractClientApplicationBase(AbstractClientApplicationBase.Builder<?> builder) {
      super(builder);
      this.clientId = builder.clientId;
      this.authority = builder.authority;
      this.validateAuthority = builder.validateAuthority;
      this.applicationName = builder.applicationName;
      this.applicationVersion = builder.applicationVersion;
      this.authenticationAuthority = builder.authenticationAuthority;
      super.tokenCache = new TokenCache(builder.tokenCacheAccessAspect);
      this.aadAadInstanceDiscoveryResponse = builder.aadInstanceDiscoveryResponse;
      this.clientCapabilities = builder.clientCapabilities;
      this.autoDetectRegion = builder.autoDetectRegion;
      this.azureRegion = builder.azureRegion;
      this.instanceDiscovery = builder.isInstanceDiscoveryEnabled;
      super.serviceBundle = new ServiceBundle(
         builder.executorService,
         new TelemetryManager(this.telemetryConsumer, builder.onlySendFailureTelemetry),
         new HttpHelper(this, new DefaultRetryPolicy())
      );
      if (this.aadAadInstanceDiscoveryResponse != null) {
         AadInstanceDiscoveryProvider.cacheInstanceDiscoveryResponse(this.authenticationAuthority.host, this.aadAadInstanceDiscoveryResponse);
      }

      if (this.authenticationAuthority.authorityType == AuthorityType.OIDC) {
         ((OidcAuthority)this.authenticationAuthority)
            .setAuthorityProperties(OidcDiscoveryProvider.performOidcDiscovery((OidcAuthority)this.authenticationAuthority, this));
      }
   }

   public abstract static class Builder<T extends AbstractClientApplicationBase.Builder<T>> extends AbstractApplicationBase.Builder<T> {
      private String clientId;
      private String authority = "https://login.microsoftonline.com/common/";
      private Authority authenticationAuthority = createDefaultAADAuthority();
      private boolean validateAuthority = true;
      private String applicationName;
      private String applicationVersion;
      private ITokenCacheAccessAspect tokenCacheAccessAspect;
      private AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse;
      private String clientCapabilities;
      private boolean autoDetectRegion;
      private String azureRegion;
      protected boolean isInstanceDiscoveryEnabled = true;

      public Builder(String clientId) {
         ParameterValidationUtils.validateNotBlank("clientId", clientId);
         this.clientId = clientId;
      }

      abstract T self();

      public T authority(String val) throws MalformedURLException {
         this.authority = Authority.enforceTrailingSlash(val);
         URL authorityURL = new URL(this.authority);
         switch (Authority.detectAuthorityType(authorityURL)) {
            case AAD:
               this.authenticationAuthority = new AADAuthority(authorityURL);
               break;
            case ADFS:
               this.authenticationAuthority = new ADFSAuthority(authorityURL);
               break;
            case CIAM:
               this.authenticationAuthority = new CIAMAuthority(authorityURL);
               break;
            default:
               throw new IllegalArgumentException("Unsupported authority type.");
         }

         Authority.validateAuthority(this.authenticationAuthority.canonicalAuthorityUrl());
         return this.self();
      }

      public T b2cAuthority(String val) throws MalformedURLException {
         this.authority = Authority.enforceTrailingSlash(val);
         URL authorityURL = new URL(this.authority);
         Authority.validateAuthority(authorityURL);
         if (Authority.detectAuthorityType(authorityURL) != AuthorityType.B2C) {
            throw new IllegalArgumentException("Unsupported authority type. Please use B2C authority");
         } else {
            this.authenticationAuthority = new B2CAuthority(authorityURL);
            this.validateAuthority = false;
            return this.self();
         }
      }

      public T oidcAuthority(String val) throws MalformedURLException {
         this.authority = Authority.enforceTrailingSlash(val);
         URL authorityURL = new URL(this.authority);
         this.authenticationAuthority = new OidcAuthority(authorityURL);
         Authority.validateAuthority(this.authenticationAuthority.canonicalAuthorityUrl());
         return this.self();
      }

      public T validateAuthority(boolean val) {
         this.validateAuthority = val;
         return this.self();
      }

      public T applicationName(String val) {
         ParameterValidationUtils.validateNotNull("applicationName", val);
         this.applicationName = val;
         return this.self();
      }

      public T applicationVersion(String val) {
         ParameterValidationUtils.validateNotNull("applicationVersion", val);
         this.applicationVersion = val;
         return this.self();
      }

      public T setTokenCacheAccessAspect(ITokenCacheAccessAspect val) {
         ParameterValidationUtils.validateNotNull("tokenCacheAccessAspect", val);
         this.tokenCacheAccessAspect = val;
         return this.self();
      }

      public T aadInstanceDiscoveryResponse(String val) {
         ParameterValidationUtils.validateNotNull("aadInstanceDiscoveryResponse", val);
         this.aadInstanceDiscoveryResponse = AadInstanceDiscoveryProvider.parseInstanceDiscoveryMetadata(val);
         return this.self();
      }

      private static Authority createDefaultAADAuthority() {
         try {
            Authority authority = new AADAuthority(new URL("https://login.microsoftonline.com/common/"));
            return authority;
         } catch (Exception var2) {
            throw new MsalClientException(var2);
         }
      }

      public T clientCapabilities(Set<String> capabilities) {
         this.clientCapabilities = JsonHelper.formCapabilitiesJson(capabilities);
         return this.self();
      }

      public T autoDetectRegion(boolean val) {
         this.autoDetectRegion = val;
         return this.self();
      }

      public T azureRegion(String val) {
         this.azureRegion = val;
         return this.self();
      }

      public T instanceDiscovery(boolean val) {
         this.isInstanceDiscoveryEnabled = val;
         return this.self();
      }

      public T logPii(boolean val) {
         return super.logPii(val);
      }

      public T connectTimeoutForDefaultHttpClient(Integer val) {
         return super.connectTimeoutForDefaultHttpClient(val);
      }

      public T readTimeoutForDefaultHttpClient(Integer val) {
         return super.readTimeoutForDefaultHttpClient(val);
      }

      public T httpClient(IHttpClient val) {
         return super.httpClient(val);
      }

      public T sslSocketFactory(SSLSocketFactory val) {
         return super.sslSocketFactory(val);
      }

      public T executorService(ExecutorService val) {
         return super.executorService(val);
      }

      public T proxy(Proxy val) {
         return super.proxy(val);
      }

      public T correlationId(String val) {
         return super.correlationId(val);
      }

      abstract AbstractClientApplicationBase build();
   }
}
