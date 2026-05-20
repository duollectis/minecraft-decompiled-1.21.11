package com.microsoft.aad.msal4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.slf4j.LoggerFactory;

public class ConfidentialClientApplication extends AbstractClientApplicationBase implements IConfidentialClientApplication {
   IClientCredential clientCredential;
   private boolean sendX5c;
   public Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider;

   @Override
   public CompletableFuture<IAuthenticationResult> acquireToken(ClientCredentialParameters parameters) {
      ParameterValidationUtils.validateNotNull("parameters", parameters);
      RequestContext context = new RequestContext(this, PublicApi.ACQUIRE_TOKEN_FOR_CLIENT, parameters);
      ClientCredentialRequest clientCredentialRequest = new ClientCredentialRequest(parameters, this, context, this.appTokenProvider);
      return this.executeRequest(clientCredentialRequest);
   }

   @Override
   public CompletableFuture<IAuthenticationResult> acquireToken(OnBehalfOfParameters parameters) {
      ParameterValidationUtils.validateNotNull("parameters", parameters);
      RequestContext context = new RequestContext(this, PublicApi.ACQUIRE_TOKEN_ON_BEHALF_OF, parameters);
      OnBehalfOfRequest oboRequest = new OnBehalfOfRequest(parameters, this, context);
      return this.executeRequest(oboRequest);
   }

   private ConfidentialClientApplication(ConfidentialClientApplication.Builder builder) {
      super(builder);
      this.sendX5c = builder.sendX5c;
      this.appTokenProvider = builder.appTokenProvider;
      this.log = LoggerFactory.getLogger(ConfidentialClientApplication.class);
      this.clientCredential = builder.clientCredential;
      this.tenant = this.authenticationAuthority.tenant;
   }

   public static ConfidentialClientApplication.Builder builder(String clientId, IClientCredential clientCredential) {
      return new ConfidentialClientApplication.Builder(clientId, clientCredential);
   }

   @Override
   public boolean sendX5c() {
      return this.sendX5c;
   }

   public static class Builder extends AbstractClientApplicationBase.Builder<ConfidentialClientApplication.Builder> {
      private IClientCredential clientCredential;
      private boolean sendX5c = true;
      private Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider;

      private Builder(String clientId, IClientCredential clientCredential) {
         super(clientId);
         ParameterValidationUtils.validateNotNull("clientCredential", clientCredential);
         this.clientCredential = clientCredential;
      }

      public ConfidentialClientApplication.Builder sendX5c(boolean val) {
         this.sendX5c = val;
         return this.self();
      }

      public ConfidentialClientApplication.Builder appTokenProvider(
         Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider
      ) {
         if (appTokenProvider != null) {
            this.appTokenProvider = appTokenProvider;
            return this.self();
         } else {
            throw new NullPointerException("appTokenProvider is null");
         }
      }

      public ConfidentialClientApplication build() {
         return new ConfidentialClientApplication(this);
      }

      protected ConfidentialClientApplication.Builder self() {
         return this;
      }
   }
}
