package com.microsoft.aad.msal4j;

import java.net.SocketException;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractManagedIdentitySource {
   private static final Logger LOG = LoggerFactory.getLogger(AbstractManagedIdentitySource.class);
   private static final String MANAGED_IDENTITY_NO_RESPONSE_RECEIVED = "[Managed Identity] Authentication unavailable. No response received from the managed identity endpoint.";
   protected final ManagedIdentityRequest managedIdentityRequest;
   protected final ServiceBundle serviceBundle;
   ManagedIdentitySourceType managedIdentitySourceType;
   ManagedIdentityIdType idType;
   String userAssignedId;
   private boolean isUserAssignedManagedIdentity;
   private String managedIdentityUserAssignedClientId;
   private String managedIdentityUserAssignedResourceId;

   public AbstractManagedIdentitySource(MsalRequest msalRequest, ServiceBundle serviceBundle, ManagedIdentitySourceType sourceType) {
      this.managedIdentityRequest = (ManagedIdentityRequest)msalRequest;
      this.managedIdentitySourceType = sourceType;
      this.serviceBundle = serviceBundle;
      this.idType = ((ManagedIdentityApplication)msalRequest.application()).getManagedIdentityId().getIdType();
      this.userAssignedId = ((ManagedIdentityApplication)msalRequest.application()).getManagedIdentityId().getUserAssignedId();
   }

   public ManagedIdentityResponse getManagedIdentityResponse(ManagedIdentityParameters parameters) {
      this.createManagedIdentityRequest(parameters.resource);
      this.managedIdentityRequest.addTokenRevocationParametersToQuery(parameters);

      IHttpResponse response;
      try {
         HttpRequest httpRequest = new HttpRequest(
            this.managedIdentityRequest.method, this.managedIdentityRequest.computeURI().toString(), this.managedIdentityRequest.headers
         );
         response = this.serviceBundle.getHttpHelper().executeHttpRequest(httpRequest, this.managedIdentityRequest.requestContext(), this.serviceBundle);
      } catch (URISyntaxException var4) {
         throw new RuntimeException(var4);
      } catch (MsalClientException var5) {
         if (var5.getCause() instanceof SocketException) {
            throw new MsalServiceException(var5.getMessage(), "managed_identity_unreachable_network", this.managedIdentitySourceType);
         }

         throw var5;
      }

      return this.handleResponse(parameters, response);
   }

   public ManagedIdentityResponse handleResponse(ManagedIdentityParameters parameters, IHttpResponse response) {
      try {
         if (response.statusCode() == 200) {
            LOG.info("[Managed Identity] Successful response received.");
            return this.getSuccessfulResponse(response);
         } else {
            String message = this.getMessageFromErrorResponse(response);
            LOG.error(String.format("[Managed Identity] request failed, HttpStatusCode: %s, Error message: %s", response.statusCode(), message));
            throw new MsalServiceException(message, "managed_identity_request_failed", this.managedIdentitySourceType);
         }
      } catch (Exception var5) {
         if (!(var5 instanceof MsalServiceException)) {
            String message = String.format(
               "[Managed Identity] Unexpected exception occurred when parsing the response, HttpStatusCode: %s, Error message: %s",
               response.statusCode(),
               var5.getMessage()
            );
            throw new MsalServiceException(message, "managed_identity_request_failed", this.managedIdentitySourceType);
         } else {
            throw var5;
         }
      }
   }

   public abstract void createManagedIdentityRequest(String var1);

   protected ManagedIdentityResponse getSuccessfulResponse(IHttpResponse response) {
      ManagedIdentityResponse managedIdentityResponse;
      try {
         managedIdentityResponse = JsonHelper.convertJsonStringToJsonSerializableObject(response.body(), ManagedIdentityResponse::fromJson);
      } catch (MsalJsonParsingException var4) {
         throw new MsalJsonParsingException(
            String.format("[Managed Identity] MSI returned %s, but the response could not be parsed: %s", response.statusCode(), var4.getMessage()),
            "managed_identity_response_parse_failure",
            this.managedIdentitySourceType
         );
      }

      if (managedIdentityResponse != null
         && managedIdentityResponse.getAccessToken() != null
         && !managedIdentityResponse.getAccessToken().isEmpty()
         && managedIdentityResponse.getExpiresOn() != null
         && !managedIdentityResponse.getExpiresOn().isEmpty()) {
         return managedIdentityResponse;
      } else {
         throw new MsalServiceException(
            "[Managed Identity] Response is either null or insufficient for authentication.", "managed_identity_request_failed", this.managedIdentitySourceType
         );
      }
   }

   protected String getMessageFromErrorResponse(IHttpResponse response) {
      ManagedIdentityErrorResponse managedIdentityErrorResponse;
      try {
         managedIdentityErrorResponse = JsonHelper.convertJsonStringToJsonSerializableObject(response.body(), ManagedIdentityErrorResponse::fromJson);
      } catch (MsalJsonParsingException var4) {
         throw new MsalJsonParsingException(
            String.format("[Managed Identity] MSI returned %s, but the response could not be parsed: %s", response.statusCode(), var4.getMessage()),
            "managed_identity_response_parse_failure",
            this.managedIdentitySourceType
         );
      }

      if (managedIdentityErrorResponse == null) {
         return "[Managed Identity] Authentication unavailable. No response received from the managed identity endpoint.";
      } else {
         return managedIdentityErrorResponse.getMessage() != null && !managedIdentityErrorResponse.getMessage().isEmpty()
            ? String.format(
               "[Managed Identity] Error Message: %s Managed Identity Correlation ID: %s Use this Correlation ID for further investigation.",
               managedIdentityErrorResponse.getMessage(),
               managedIdentityErrorResponse.getCorrelationId()
            )
            : String.format(
               "[Managed Identity] Error Code: %s Error Message: %s",
               managedIdentityErrorResponse.getError(),
               managedIdentityErrorResponse.getErrorDescription()
            );
      }
   }

   protected static IEnvironmentVariables getEnvironmentVariables() {
      return (IEnvironmentVariables)(ManagedIdentityApplication.environmentVariables == null
         ? new EnvironmentVariables()
         : ManagedIdentityApplication.environmentVariables);
   }

   public boolean isUserAssignedManagedIdentity() {
      return this.isUserAssignedManagedIdentity;
   }

   public String getManagedIdentityUserAssignedClientId() {
      return this.managedIdentityUserAssignedClientId;
   }

   public String getManagedIdentityUserAssignedResourceId() {
      return this.managedIdentityUserAssignedResourceId;
   }

   public void setUserAssignedManagedIdentity(boolean isUserAssignedManagedIdentity) {
      this.isUserAssignedManagedIdentity = isUserAssignedManagedIdentity;
   }

   public void setManagedIdentityUserAssignedClientId(String managedIdentityUserAssignedClientId) {
      this.managedIdentityUserAssignedClientId = managedIdentityUserAssignedClientId;
   }

   public void setManagedIdentityUserAssignedResourceId(String managedIdentityUserAssignedResourceId) {
      this.managedIdentityUserAssignedResourceId = managedIdentityUserAssignedResourceId;
   }
}
