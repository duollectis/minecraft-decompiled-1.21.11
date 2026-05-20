package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ManagedIdentityClient {
   private static final Logger LOG = LoggerFactory.getLogger(ManagedIdentityClient.class);
   AbstractManagedIdentitySource managedIdentitySource;

   static ManagedIdentitySourceType getManagedIdentitySource() {
      IEnvironmentVariables environmentVariables = AbstractManagedIdentitySource.getEnvironmentVariables();
      if (!StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable("IDENTITY_ENDPOINT"))
         && !StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable("IDENTITY_HEADER"))) {
         return !StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable("IDENTITY_SERVER_THUMBPRINT"))
            ? ManagedIdentitySourceType.SERVICE_FABRIC
            : ManagedIdentitySourceType.APP_SERVICE;
      } else if (!StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable("MSI_ENDPOINT"))) {
         return ManagedIdentitySourceType.CLOUD_SHELL;
      } else {
         return !StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable("IDENTITY_ENDPOINT"))
               && !StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable("IMDS_ENDPOINT"))
            ? ManagedIdentitySourceType.AZURE_ARC
            : ManagedIdentitySourceType.DEFAULT_TO_IMDS;
      }
   }

   ManagedIdentityClient(MsalRequest msalRequest, ServiceBundle serviceBundle) {
      this.managedIdentitySource = createManagedIdentitySource(msalRequest, serviceBundle);
      ManagedIdentityApplication managedIdentityApplication = (ManagedIdentityApplication)msalRequest.application();
      ManagedIdentityIdType identityIdType = managedIdentityApplication.getManagedIdentityId().getIdType();
      if (!identityIdType.equals(ManagedIdentityIdType.SYSTEM_ASSIGNED)) {
         this.managedIdentitySource.setUserAssignedManagedIdentity(true);
      }
   }

   ManagedIdentityResponse getManagedIdentityResponse(ManagedIdentityParameters parameters) {
      return this.managedIdentitySource.getManagedIdentityResponse(parameters);
   }

   private static AbstractManagedIdentitySource createManagedIdentitySource(MsalRequest msalRequest, ServiceBundle serviceBundle) {
      switch (getManagedIdentitySource()) {
         case SERVICE_FABRIC:
            return ServiceFabricManagedIdentitySource.create(msalRequest, serviceBundle);
         case APP_SERVICE:
            return AppServiceManagedIdentitySource.create(msalRequest, serviceBundle);
         case CLOUD_SHELL:
            return CloudShellManagedIdentitySource.create(msalRequest, serviceBundle);
         case AZURE_ARC:
            return AzureArcManagedIdentitySource.create(msalRequest, serviceBundle);
         default:
            return new IMDSManagedIdentitySource(msalRequest, serviceBundle);
      }
   }
}
