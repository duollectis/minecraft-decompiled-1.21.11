package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ManagedIdentityRequest extends MsalRequest {
   private static final Logger LOG = LoggerFactory.getLogger(ManagedIdentityRequest.class);
   URI baseEndpoint;
   HttpMethod method;
   Map<String, String> headers;
   Map<String, String> bodyParameters;
   Map<String, String> queryParameters;

   public ManagedIdentityRequest(ManagedIdentityApplication managedIdentityApplication, RequestContext requestContext) {
      super(managedIdentityApplication, requestContext);
   }

   public String getBodyAsString() {
      return this.bodyParameters != null && !this.bodyParameters.isEmpty() ? StringHelper.serializeQueryParameters(this.bodyParameters) : "";
   }

   public URL computeURI() throws URISyntaxException {
      String endpoint = this.appendQueryParametersToBaseEndpoint();

      try {
         return new URL(endpoint);
      } catch (MalformedURLException var3) {
         throw new RuntimeException(var3);
      }
   }

   private String appendQueryParametersToBaseEndpoint() {
      if (this.queryParameters != null && !this.queryParameters.isEmpty()) {
         String queryString = StringHelper.serializeQueryParameters(this.queryParameters);
         return this.baseEndpoint.toString() + "?" + queryString;
      } else {
         return this.baseEndpoint.toString();
      }
   }

   void addUserAssignedIdToQuery(ManagedIdentityIdType idType, String userAssignedId) {
      switch (idType) {
         case CLIENT_ID:
            LOG.info("[Managed Identity] Adding user assigned client id to the request.");
            this.queryParameters.put("client_id", userAssignedId);
            break;
         case RESOURCE_ID:
            LOG.info("[Managed Identity] Adding user assigned resource id to the request.");
            if (ManagedIdentityClient.getManagedIdentitySource() == ManagedIdentitySourceType.IMDS) {
               this.queryParameters.put("msi_res_id", userAssignedId);
            } else {
               this.queryParameters.put("mi_res_id", userAssignedId);
            }
            break;
         case OBJECT_ID:
            LOG.info("[Managed Identity] Adding user assigned object id to the request.");
            this.queryParameters.put("object_id", userAssignedId);
      }
   }

   void addTokenRevocationParametersToQuery(ManagedIdentityParameters parameters) {
      ManagedIdentitySourceType sourceType = ManagedIdentityClient.getManagedIdentitySource();
      boolean supportsTokenRevocation = Constants.TOKEN_REVOCATION_SUPPORTED_ENVIRONMENTS.contains(sourceType);
      if (supportsTokenRevocation) {
         ManagedIdentityApplication managedIdentityApplication = (ManagedIdentityApplication)this.application();
         if (managedIdentityApplication.getClientCapabilities() != null && !managedIdentityApplication.getClientCapabilities().isEmpty()) {
            String clientCapabilities = String.join(",", managedIdentityApplication.getClientCapabilities());
            this.queryParameters.put("xms_cc", clientCapabilities.toString());
         }

         if (!StringHelper.isNullOrBlank(parameters.claims) && !StringHelper.isNullOrBlank(parameters.revokedTokenHash())) {
            LOG.info("[Managed Identity] Adding token revocation parameter to request");
            if (this.queryParameters == null) {
               this.queryParameters = new HashMap<>();
            }

            this.queryParameters.put("token_sha256_to_refresh", parameters.revokedTokenHash());
         }
      }
   }
}
