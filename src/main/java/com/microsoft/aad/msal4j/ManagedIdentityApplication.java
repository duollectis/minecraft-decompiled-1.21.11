package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.LoggerFactory;

public class ManagedIdentityApplication extends AbstractApplicationBase implements IManagedIdentityApplication {
   private final ManagedIdentityId managedIdentityId;
   private List<String> clientCapabilities;
   static TokenCache sharedTokenCache = new TokenCache();
   @Deprecated
   ManagedIdentitySourceType managedIdentitySource = ManagedIdentityClient.getManagedIdentitySource();
   static IEnvironmentVariables environmentVariables;

   static void setEnvironmentVariables(IEnvironmentVariables environmentVariables) {
      ManagedIdentityApplication.environmentVariables = environmentVariables;
   }

   private ManagedIdentityApplication(ManagedIdentityApplication.Builder builder) {
      super(builder);
      super.tokenCache = sharedTokenCache;
      super.serviceBundle = new ServiceBundle(
         builder.executorService,
         new TelemetryManager(this.telemetryConsumer, builder.onlySendFailureTelemetry),
         new HttpHelper(this, new ManagedIdentityRetryPolicy())
      );
      this.log = LoggerFactory.getLogger(ManagedIdentityApplication.class);
      this.managedIdentityId = builder.managedIdentityId;
      this.tenant = "managed_identity";
      this.clientCapabilities = builder.clientCapabilities;
   }

   public static TokenCache getSharedTokenCache() {
      return sharedTokenCache;
   }

   static IEnvironmentVariables getEnvironmentVariables() {
      return environmentVariables;
   }

   public ManagedIdentityId getManagedIdentityId() {
      return this.managedIdentityId;
   }

   public List<String> getClientCapabilities() {
      return this.clientCapabilities;
   }

   @Override
   public CompletableFuture<IAuthenticationResult> acquireTokenForManagedIdentity(ManagedIdentityParameters managedIdentityParameters) throws Exception {
      RequestContext requestContext = new RequestContext(
         this,
         this.managedIdentityId.getIdType() == ManagedIdentityIdType.SYSTEM_ASSIGNED
            ? PublicApi.ACQUIRE_TOKEN_BY_SYSTEM_ASSIGNED_MANAGED_IDENTITY
            : PublicApi.ACQUIRE_TOKEN_BY_USER_ASSIGNED_MANAGED_IDENTITY,
         managedIdentityParameters
      );
      ManagedIdentityRequest managedIdentityRequest = new ManagedIdentityRequest(this, requestContext);
      return this.executeRequest(managedIdentityRequest);
   }

   public static ManagedIdentityApplication.Builder builder(ManagedIdentityId managedIdentityId) {
      return new ManagedIdentityApplication.Builder(managedIdentityId);
   }

   public static ManagedIdentitySourceType getManagedIdentitySource() {
      return ManagedIdentityClient.getManagedIdentitySource();
   }

   public static class Builder extends AbstractApplicationBase.Builder<ManagedIdentityApplication.Builder> {
      private String resource;
      private ManagedIdentityId managedIdentityId;
      private List<String> clientCapabilities;

      private Builder(ManagedIdentityId managedIdentityId) {
         super(
            managedIdentityId.getIdType() == ManagedIdentityIdType.SYSTEM_ASSIGNED ? "system_assigned_managed_identity" : managedIdentityId.getUserAssignedId()
         );
         this.managedIdentityId = managedIdentityId;
      }

      public ManagedIdentityApplication.Builder resource(String resource) {
         this.resource = resource;
         return this.self();
      }

      public ManagedIdentityApplication.Builder clientCapabilities(List<String> clientCapabilities) {
         this.clientCapabilities = clientCapabilities;
         return this.self();
      }

      public ManagedIdentityApplication build() {
         return new ManagedIdentityApplication(this);
      }

      protected ManagedIdentityApplication.Builder self() {
         return this;
      }
   }
}
