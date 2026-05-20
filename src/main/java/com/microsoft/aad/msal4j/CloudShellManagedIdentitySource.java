package com.microsoft.aad.msal4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CloudShellManagedIdentitySource extends AbstractManagedIdentitySource {
   private static final Logger LOG = LoggerFactory.getLogger(CloudShellManagedIdentitySource.class);
   private final URI msiEndpoint;

   @Override
   public void createManagedIdentityRequest(String resource) {
      this.managedIdentityRequest.baseEndpoint = this.msiEndpoint;
      this.managedIdentityRequest.method = HttpMethod.GET;
      this.managedIdentityRequest.headers = new HashMap<>();
      this.managedIdentityRequest.headers.put("ContentType", "application/x-www-form-urlencoded");
      this.managedIdentityRequest.headers.put("Metadata", "true");
      this.managedIdentityRequest.queryParameters = new HashMap<>();
      this.managedIdentityRequest.queryParameters.put("resource", resource);
   }

   private CloudShellManagedIdentitySource(MsalRequest msalRequest, ServiceBundle serviceBundle, URI msiEndpoint) {
      super(msalRequest, serviceBundle, ManagedIdentitySourceType.CLOUD_SHELL);
      this.msiEndpoint = msiEndpoint;
      ManagedIdentityIdType idType = ((ManagedIdentityApplication)msalRequest.application()).getManagedIdentityId().getIdType();
      if (idType != ManagedIdentityIdType.SYSTEM_ASSIGNED) {
         throw new MsalServiceException(
            String.format(
               "[Managed Identity] User assigned identity is not supported by the %s Managed Identity. To authenticate with the system assigned identity use ManagedIdentityApplication.builder(ManagedIdentityId.systemAssigned()).build().",
               "cloud shell"
            ),
            "user_assigned_managed_identity_not_supported",
            ManagedIdentitySourceType.CLOUD_SHELL
         );
      }
   }

   static AbstractManagedIdentitySource create(MsalRequest msalRequest, ServiceBundle serviceBundle) {
      IEnvironmentVariables environmentVariables = getEnvironmentVariables();
      String msiEndpoint = environmentVariables.getEnvironmentVariable("MSI_ENDPOINT");
      if (StringHelper.isNullOrBlank(msiEndpoint)) {
         LOG.info("[Managed Identity] Cloud shell managed identity is unavailable.");
         return null;
      } else {
         return new CloudShellManagedIdentitySource(msalRequest, serviceBundle, validateAndGetUri(msiEndpoint));
      }
   }

   private static URI validateAndGetUri(String msiEndpoint) {
      try {
         URI endpointUri = new URI(msiEndpoint);
         LOG.info(
            String.format(
               "[Managed Identity] Environment variables validation passed for cloud shell managed identity. Endpoint URI: %s. Creating cloud shell managed identity.",
               endpointUri
            )
         );
         return endpointUri;
      } catch (URISyntaxException var2) {
         throw new MsalServiceException(
            String.format(
               "[Managed Identity] The environment variable %s contains an invalid Uri %s in %s managed identity source.",
               "MSI_ENDPOINT",
               msiEndpoint,
               "Cloud Shell"
            ),
            "invalid_managed_identity_endpoint",
            ManagedIdentitySourceType.CLOUD_SHELL
         );
      }
   }
}
