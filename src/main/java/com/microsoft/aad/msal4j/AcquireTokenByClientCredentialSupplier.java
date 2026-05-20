package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AcquireTokenByClientCredentialSupplier extends AuthenticationResultSupplier {
   private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenByClientCredentialSupplier.class);
   private ClientCredentialRequest clientCredentialRequest;

   AcquireTokenByClientCredentialSupplier(ConfidentialClientApplication clientApplication, ClientCredentialRequest clientCredentialRequest) {
      super(clientApplication, clientCredentialRequest);
      this.clientCredentialRequest = clientCredentialRequest;
   }

   @Override
   AuthenticationResult execute() throws Exception {
      if (this.clientCredentialRequest.parameters.skipCache() != null && !this.clientCredentialRequest.parameters.skipCache()) {
         LOG.debug("SkipCache set to false. Attempting cache lookup");

         try {
            SilentParameters parameters = SilentParameters.builder(this.clientCredentialRequest.parameters.scopes())
               .claims(this.clientCredentialRequest.parameters.claims())
               .tenant(this.clientCredentialRequest.parameters.tenant())
               .build();
            RequestContext context = new RequestContext(this.clientApplication, PublicApi.ACQUIRE_TOKEN_SILENTLY, parameters);
            SilentRequest silentRequest = new SilentRequest(parameters, this.clientApplication, context, null);
            AcquireTokenSilentSupplier supplier = new AcquireTokenSilentSupplier(this.clientApplication, silentRequest);
            return supplier.execute();
         } catch (MsalClientException var5) {
            LOG.debug(String.format("Cache lookup failed: %s", var5.getMessage()));
            return this.acquireTokenByClientCredential();
         }
      } else {
         LOG.debug("SkipCache set to true. Skipping cache lookup and attempting client credentials request");
         return this.acquireTokenByClientCredential();
      }
   }

   private AuthenticationResult acquireTokenByClientCredential() throws Exception {
      if (this.clientCredentialRequest.appTokenProvider != null) {
         String claims = "";
         if (null != this.clientCredentialRequest.parameters.claims()) {
            claims = this.clientCredentialRequest.parameters.claims().toString();
         }

         AppTokenProviderParameters appTokenProviderParameters = new AppTokenProviderParameters(
            this.clientCredentialRequest.parameters.scopes(),
            this.clientCredentialRequest.requestContext().correlationId(),
            claims,
            this.clientCredentialRequest.parameters.tenant()
         );
         AcquireTokenByAppProviderSupplier supplier = new AcquireTokenByAppProviderSupplier(
            (AbstractClientApplicationBase)this.clientApplication, this.clientCredentialRequest, appTokenProviderParameters
         );
         return supplier.execute();
      } else {
         AcquireTokenByAuthorizationGrantSupplier supplier = new AcquireTokenByAuthorizationGrantSupplier(
            this.clientApplication, this.clientCredentialRequest, null
         );
         return supplier.execute();
      }
   }
}
