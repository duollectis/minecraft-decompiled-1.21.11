package com.microsoft.aad.msal4j;

import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;

public abstract class AbstractApplicationBase implements IApplicationBase {
   protected Logger log;
   protected Authority authenticationAuthority;
   private String correlationId;
   private boolean logPii;
   private Proxy proxy;
   private SSLSocketFactory sslSocketFactory;
   private IHttpClient httpClient;
   private Integer connectTimeoutForDefaultHttpClient;
   private Integer readTimeoutForDefaultHttpClient;
   private boolean retryDisabled;
   String tenant;
   private boolean validateAuthority;
   private String clientId;
   private String authority;
   ServiceBundle serviceBundle;
   Consumer<List<HashMap<String, String>>> telemetryConsumer;
   protected TokenCache tokenCache;

   CompletableFuture<IAuthenticationResult> executeRequest(MsalRequest msalRequest) {
      AuthenticationResultSupplier supplier = this.getAuthenticationResultSupplier(msalRequest);
      ExecutorService executorService = this.serviceBundle.getExecutorService();
      return executorService != null ? CompletableFuture.supplyAsync(supplier, executorService) : CompletableFuture.supplyAsync(supplier);
   }

   AuthenticationResult acquireTokenCommon(MsalRequest msalRequest, Authority requestAuthority) throws Exception {
      HttpHeaders headers = msalRequest.headers();
      if (this.logPii) {
         this.log.debug(LogHelper.createMessage(String.format("Using Client Http Headers: %s", headers), headers.getHeaderCorrelationIdValue()));
      }

      TokenRequestExecutor requestExecutor = new TokenRequestExecutor(requestAuthority, msalRequest, this.serviceBundle);
      AuthenticationResult result = requestExecutor.executeTokenRequest();
      if (this.authenticationAuthority.authorityType.equals(AuthorityType.AAD)) {
         InstanceDiscoveryMetadataEntry instanceDiscoveryMetadata = AadInstanceDiscoveryProvider.getMetadataEntry(
            requestAuthority.canonicalAuthorityUrl(), this.validateAuthority, msalRequest, this.serviceBundle
         );
         this.tokenCache.saveTokens(requestExecutor, result, instanceDiscoveryMetadata.preferredCache);
      } else {
         this.tokenCache.saveTokens(requestExecutor, result, this.authenticationAuthority.host);
      }

      return result;
   }

   private AuthenticationResultSupplier getAuthenticationResultSupplier(MsalRequest msalRequest) {
      AuthenticationResultSupplier supplier;
      if (msalRequest instanceof DeviceCodeFlowRequest) {
         supplier = new AcquireTokenByDeviceCodeFlowSupplier((PublicClientApplication)this, (DeviceCodeFlowRequest)msalRequest);
      } else if (msalRequest instanceof SilentRequest) {
         supplier = new AcquireTokenSilentSupplier(this, (SilentRequest)msalRequest);
      } else if (msalRequest instanceof InteractiveRequest) {
         supplier = new AcquireTokenByInteractiveFlowSupplier((PublicClientApplication)this, (InteractiveRequest)msalRequest);
      } else if (msalRequest instanceof ClientCredentialRequest) {
         supplier = new AcquireTokenByClientCredentialSupplier((ConfidentialClientApplication)this, (ClientCredentialRequest)msalRequest);
      } else if (msalRequest instanceof OnBehalfOfRequest) {
         supplier = new AcquireTokenByOnBehalfOfSupplier((ConfidentialClientApplication)this, (OnBehalfOfRequest)msalRequest);
      } else if (msalRequest instanceof ManagedIdentityRequest) {
         supplier = new AcquireTokenByManagedIdentitySupplier((ManagedIdentityApplication)this, (ManagedIdentityRequest)msalRequest);
      } else {
         supplier = new AcquireTokenByAuthorizationGrantSupplier(this, msalRequest, null);
      }

      return supplier;
   }

   @Override
   public String correlationId() {
      return this.correlationId;
   }

   @Override
   public boolean logPii() {
      return this.logPii;
   }

   @Override
   public Proxy proxy() {
      return this.proxy;
   }

   @Override
   public SSLSocketFactory sslSocketFactory() {
      return this.sslSocketFactory;
   }

   @Override
   public IHttpClient httpClient() {
      return this.httpClient;
   }

   public Integer connectTimeoutForDefaultHttpClient() {
      return this.connectTimeoutForDefaultHttpClient;
   }

   public Integer readTimeoutForDefaultHttpClient() {
      return this.readTimeoutForDefaultHttpClient;
   }

   boolean isRetryDisabled() {
      return this.retryDisabled;
   }

   String tenant() {
      return this.tenant;
   }

   boolean validateAuthority() {
      return this.validateAuthority;
   }

   String clientId() {
      return this.clientId;
   }

   String authority() {
      return this.authority;
   }

   ServiceBundle serviceBundle() {
      return this.serviceBundle;
   }

   Consumer<List<HashMap<String, String>>> telemetryConsumer() {
      return this.telemetryConsumer;
   }

   TokenCache tokenCache() {
      return this.tokenCache;
   }

   AbstractApplicationBase(AbstractApplicationBase.Builder<?> builder) {
      this.correlationId = builder.correlationId;
      this.logPii = builder.logPii;
      this.telemetryConsumer = builder.telemetryConsumer;
      this.proxy = builder.proxy;
      this.sslSocketFactory = builder.sslSocketFactory;
      this.connectTimeoutForDefaultHttpClient = builder.connectTimeoutForDefaultHttpClient;
      this.readTimeoutForDefaultHttpClient = builder.readTimeoutForDefaultHttpClient;
      this.authenticationAuthority = builder.authenticationAuthority;
      this.clientId = builder.clientId;
      this.retryDisabled = builder.disableInternalRetries;
      if (builder.httpClient == null) {
         this.httpClient = new DefaultHttpClient(
            builder.proxy, builder.sslSocketFactory, builder.connectTimeoutForDefaultHttpClient, builder.readTimeoutForDefaultHttpClient
         );
      } else {
         this.httpClient = builder.httpClient;
      }
   }

   public abstract static class Builder<T extends AbstractApplicationBase.Builder<T>> {
      private String correlationId;
      private boolean logPii = false;
      ExecutorService executorService;
      Proxy proxy;
      SSLSocketFactory sslSocketFactory;
      IHttpClient httpClient;
      private Consumer<List<HashMap<String, String>>> telemetryConsumer;
      Boolean onlySendFailureTelemetry = false;
      Integer connectTimeoutForDefaultHttpClient;
      Integer readTimeoutForDefaultHttpClient;
      boolean disableInternalRetries;
      private String clientId;
      private Authority authenticationAuthority = createDefaultAADAuthority();

      public Builder() {
      }

      public Builder(String clientId) {
         ParameterValidationUtils.validateNotBlank("clientId", clientId);
         this.clientId = clientId;
      }

      abstract T self();

      public T correlationId(String val) {
         ParameterValidationUtils.validateNotBlank("correlationId", val);
         this.correlationId = val;
         return this.self();
      }

      public T logPii(boolean val) {
         this.logPii = val;
         return this.self();
      }

      public T executorService(ExecutorService val) {
         ParameterValidationUtils.validateNotNull("executorService", val);
         this.executorService = val;
         return this.self();
      }

      public T proxy(Proxy val) {
         ParameterValidationUtils.validateNotNull("proxy", val);
         this.proxy = val;
         return this.self();
      }

      public T httpClient(IHttpClient val) {
         ParameterValidationUtils.validateNotNull("httpClient", val);
         this.httpClient = val;
         return this.self();
      }

      public T sslSocketFactory(SSLSocketFactory val) {
         ParameterValidationUtils.validateNotNull("sslSocketFactory", val);
         this.sslSocketFactory = val;
         return this.self();
      }

      public T connectTimeoutForDefaultHttpClient(Integer val) {
         ParameterValidationUtils.validateNotNull("connectTimeoutForDefaultHttpClient", val);
         this.connectTimeoutForDefaultHttpClient = val;
         return this.self();
      }

      public T readTimeoutForDefaultHttpClient(Integer val) {
         ParameterValidationUtils.validateNotNull("readTimeoutForDefaultHttpClient", val);
         this.readTimeoutForDefaultHttpClient = val;
         return this.self();
      }

      public T disableInternalRetries() {
         this.disableInternalRetries = true;
         return this.self();
      }

      T telemetryConsumer(Consumer<List<HashMap<String, String>>> val) {
         ParameterValidationUtils.validateNotNull("telemetryConsumer", val);
         this.telemetryConsumer = val;
         return this.self();
      }

      T onlySendFailureTelemetry(Boolean val) {
         this.onlySendFailureTelemetry = val;
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

      abstract AbstractApplicationBase build();
   }
}
