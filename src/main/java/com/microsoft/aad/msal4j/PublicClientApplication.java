package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.LoggerFactory;

public class PublicClientApplication extends AbstractClientApplicationBase implements IPublicClientApplication {
   private IBroker broker;
   private boolean brokerEnabled;

   @Override
   public CompletableFuture<IAuthenticationResult> acquireToken(UserNamePasswordParameters parameters) {
      ParameterValidationUtils.validateNotNull("parameters", parameters);
      RequestContext context = new RequestContext(this, PublicApi.ACQUIRE_TOKEN_BY_USERNAME_PASSWORD, parameters, UserIdentifier.fromUpn(parameters.username()));
      CompletableFuture<IAuthenticationResult> future;
      if (this.validateBrokerUsage(parameters)) {
         future = this.broker.acquireToken(this, parameters);
      } else {
         UserNamePasswordRequest userNamePasswordRequest = new UserNamePasswordRequest(parameters, this, context);
         future = this.executeRequest(userNamePasswordRequest);
      }

      return future;
   }

   @Override
   public CompletableFuture<IAuthenticationResult> acquireToken(IntegratedWindowsAuthenticationParameters parameters) {
      ParameterValidationUtils.validateNotNull("parameters", parameters);
      RequestContext context = new RequestContext(
         this, PublicApi.ACQUIRE_TOKEN_BY_INTEGRATED_WINDOWS_AUTH, parameters, UserIdentifier.fromUpn(parameters.username())
      );
      IntegratedWindowsAuthenticationRequest integratedWindowsAuthenticationRequest = new IntegratedWindowsAuthenticationRequest(parameters, this, context);
      return this.executeRequest(integratedWindowsAuthenticationRequest);
   }

   @Override
   public CompletableFuture<IAuthenticationResult> acquireToken(DeviceCodeFlowParameters parameters) {
      if (AuthorityType.B2C.equals(this.authenticationAuthority.authorityType())) {
         throw new IllegalArgumentException("Invalid authority type. Device Flow is not supported by B2C authority.");
      } else {
         ParameterValidationUtils.validateNotNull("parameters", parameters);
         RequestContext context = new RequestContext(this, PublicApi.ACQUIRE_TOKEN_BY_DEVICE_CODE_FLOW, parameters);
         AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference = new AtomicReference<>();
         DeviceCodeFlowRequest deviceCodeRequest = new DeviceCodeFlowRequest(parameters, futureReference, this, context);
         CompletableFuture<IAuthenticationResult> future = this.executeRequest(deviceCodeRequest);
         futureReference.set(future);
         return future;
      }
   }

   @Override
   public CompletableFuture<IAuthenticationResult> acquireToken(InteractiveRequestParameters parameters) {
      ParameterValidationUtils.validateNotNull("parameters", parameters);
      AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference = new AtomicReference<>();
      RequestContext context = new RequestContext(this, PublicApi.ACQUIRE_TOKEN_INTERACTIVE, parameters, UserIdentifier.fromUpn(parameters.loginHint()));
      InteractiveRequest interactiveRequest = new InteractiveRequest(parameters, futureReference, this, context);
      CompletableFuture<IAuthenticationResult> future;
      if (this.validateBrokerUsage(parameters)) {
         future = this.broker.acquireToken(this, parameters);
      } else {
         future = this.executeRequest(interactiveRequest);
      }

      futureReference.set(future);
      return future;
   }

   @Override
   public CompletableFuture<IAuthenticationResult> acquireTokenSilently(SilentParameters parameters) throws MalformedURLException {
      CompletableFuture<IAuthenticationResult> future;
      if (this.validateBrokerUsage(parameters)) {
         future = this.broker.acquireToken(this, parameters);
      } else {
         future = super.acquireTokenSilently(parameters);
      }

      return future;
   }

   @Override
   public CompletableFuture<Void> removeAccount(IAccount account) {
      if (this.brokerEnabled) {
         this.broker.removeAccount(this, account);
      }

      return super.removeAccount(account);
   }

   private PublicClientApplication(PublicClientApplication.Builder builder) {
      super(builder);
      ParameterValidationUtils.validateNotBlank("clientId", this.clientId());
      this.log = LoggerFactory.getLogger(PublicClientApplication.class);
      this.broker = builder.broker;
      this.brokerEnabled = builder.brokerEnabled;
      this.tenant = this.authenticationAuthority.tenant;
   }

   public static PublicClientApplication.Builder builder(String clientId) {
      return new PublicClientApplication.Builder(clientId);
   }

   private boolean validateBrokerUsage(InteractiveRequestParameters parameters) {
      if (!this.brokerEnabled && parameters.proofOfPossession() != null) {
         throw new MsalClientException(
            "InteractiveRequestParameters.proofOfPossession should not be used when broker is not available, see https://aka.ms/msal4j-pop for more information",
            "brokers_package_error"
         );
      } else {
         return this.brokerEnabled;
      }
   }

   private boolean validateBrokerUsage(UserNamePasswordParameters parameters) {
      if (!this.brokerEnabled && parameters.proofOfPossession() != null) {
         throw new MsalClientException(
            "UserNamePasswordParameters.proofOfPossession should not be used when broker is not available, see https://aka.ms/msal4j-pop for more information",
            "brokers_package_error"
         );
      } else {
         return this.brokerEnabled;
      }
   }

   private boolean validateBrokerUsage(SilentParameters parameters) {
      if (!this.brokerEnabled && parameters.proofOfPossession() != null) {
         throw new MsalClientException(
            "UserNamePasswordParameters.proofOfPossession should not be used when broker is not available, see https://aka.ms/msal4j-pop for more information",
            "brokers_package_error"
         );
      } else {
         return this.brokerEnabled;
      }
   }

   public static class Builder extends AbstractClientApplicationBase.Builder<PublicClientApplication.Builder> {
      private IBroker broker = null;
      private boolean brokerEnabled = false;

      private Builder(String clientId) {
         super(clientId);
      }

      public PublicClientApplication.Builder broker(IBroker val) {
         this.broker = val;
         this.brokerEnabled = this.broker.isBrokerAvailable();
         return this.self();
      }

      public PublicClientApplication build() {
         return new PublicClientApplication(this);
      }

      protected PublicClientApplication.Builder self() {
         return this;
      }
   }
}
