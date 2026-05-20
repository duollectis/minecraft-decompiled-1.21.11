package com.microsoft.aad.msal4j;

import java.util.concurrent.TimeUnit;

class AcquireTokenByDeviceCodeFlowSupplier extends AuthenticationResultSupplier {
   private DeviceCodeFlowRequest deviceCodeFlowRequest;

   AcquireTokenByDeviceCodeFlowSupplier(PublicClientApplication clientApplication, DeviceCodeFlowRequest deviceCodeFlowRequest) {
      super(clientApplication, deviceCodeFlowRequest);
      this.deviceCodeFlowRequest = deviceCodeFlowRequest;
   }

   @Override
   AuthenticationResult execute() throws Exception {
      Authority requestAuthority = this.clientApplication.authenticationAuthority;
      requestAuthority = this.getAuthorityWithPrefNetworkHost(requestAuthority.authority());
      DeviceCode deviceCode = this.getDeviceCode(requestAuthority);
      return this.acquireTokenWithDeviceCode(deviceCode, requestAuthority);
   }

   private DeviceCode getDeviceCode(Authority requestAuthority) {
      DeviceCode deviceCode = this.deviceCodeFlowRequest
         .acquireDeviceCode(
            requestAuthority.deviceCodeEndpoint(),
            this.clientApplication.clientId(),
            this.deviceCodeFlowRequest.headers().getReadonlyHeaderMap(),
            this.clientApplication.serviceBundle()
         );
      this.deviceCodeFlowRequest.parameters().deviceCodeConsumer().accept(deviceCode);
      return deviceCode;
   }

   private AuthenticationResult acquireTokenWithDeviceCode(DeviceCode deviceCode, Authority requestAuthority) throws Exception {
      this.deviceCodeFlowRequest.createAuthenticationGrant(deviceCode);
      long expirationTimeInSeconds = this.getCurrentSystemTimeInSeconds() + deviceCode.expiresIn();
      AcquireTokenByAuthorizationGrantSupplier acquireTokenByAuthorisationGrantSupplier = new AcquireTokenByAuthorizationGrantSupplier(
         this.clientApplication, this.deviceCodeFlowRequest, requestAuthority
      );

      while (this.getCurrentSystemTimeInSeconds() < expirationTimeInSeconds) {
         if (this.deviceCodeFlowRequest.futureReference().get().isCancelled()) {
            throw new InterruptedException("Device code flow was cancelled before acquiring a token");
         }

         if (this.deviceCodeFlowRequest.futureReference().get().isCompletedExceptionally()) {
            throw new InterruptedException("Device code flow had an exception before acquiring a token");
         }

         try {
            return acquireTokenByAuthorisationGrantSupplier.execute();
         } catch (MsalServiceException var7) {
            if (var7.errorCode() == null || !var7.errorCode().equals("authorization_pending")) {
               throw var7;
            }

            TimeUnit.SECONDS.sleep(deviceCode.interval());
         }
      }

      throw new MsalClientException("Expired Device code", "code_expired");
   }

   private Long getCurrentSystemTimeInSeconds() {
      return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
   }
}
