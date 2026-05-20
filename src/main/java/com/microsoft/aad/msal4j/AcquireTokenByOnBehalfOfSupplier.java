package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AcquireTokenByOnBehalfOfSupplier extends AuthenticationResultSupplier {
   private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenByOnBehalfOfSupplier.class);
   private OnBehalfOfRequest onBehalfOfRequest;

   AcquireTokenByOnBehalfOfSupplier(ConfidentialClientApplication clientApplication, OnBehalfOfRequest onBehalfOfRequest) {
      super(clientApplication, onBehalfOfRequest);
      this.onBehalfOfRequest = onBehalfOfRequest;
   }

   @Override
   AuthenticationResult execute() throws Exception {
      if (this.onBehalfOfRequest.parameters.skipCache() != null && !this.onBehalfOfRequest.parameters.skipCache()) {
         LOG.debug("SkipCache set to false. Attempting cache lookup");

         try {
            SilentParameters parameters = SilentParameters.builder(this.onBehalfOfRequest.parameters.scopes())
               .claims(this.onBehalfOfRequest.parameters.claims())
               .tenant(this.onBehalfOfRequest.parameters.tenant())
               .build();
            RequestContext context = new RequestContext(this.clientApplication, PublicApi.ACQUIRE_TOKEN_SILENTLY, parameters);
            SilentRequest silentRequest = new SilentRequest(parameters, this.clientApplication, context, this.onBehalfOfRequest.parameters.userAssertion());
            AcquireTokenSilentSupplier supplier = new AcquireTokenSilentSupplier(this.clientApplication, silentRequest);
            return supplier.execute();
         } catch (MsalClientException var5) {
            LOG.debug(String.format("Cache lookup failed: %s", var5.getMessage()));
            return this.acquireTokenOnBehalfOf();
         }
      } else {
         LOG.debug("SkipCache set to true. Skipping cache lookup and attempting on-behalf-of request");
         return this.acquireTokenOnBehalfOf();
      }
   }

   private AuthenticationResult acquireTokenOnBehalfOf() throws Exception {
      AcquireTokenByAuthorizationGrantSupplier supplier = new AcquireTokenByAuthorizationGrantSupplier(this.clientApplication, this.onBehalfOfRequest, null);
      return supplier.execute();
   }
}
