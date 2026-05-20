package com.microsoft.aad.msal4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IMDSManagedIdentitySource extends AbstractManagedIdentitySource {
   private static final Logger LOG = LoggerFactory.getLogger(IMDSManagedIdentitySource.class);
   private static final URI DEFAULT_IMDS_ENDPOINT;
   private static final String IMDS_TOKEN_PATH = "/metadata/identity/oauth2/token";
   private static final String IMDS_API_VERSION = "2018-02-01";
   private URI imdsEndpoint;

   public IMDSManagedIdentitySource(MsalRequest msalRequest, ServiceBundle serviceBundle) {
      super(msalRequest, serviceBundle, ManagedIdentitySourceType.IMDS);
      IEnvironmentVariables environmentVariables = getEnvironmentVariables();
      IHttpHelper httpHelper = serviceBundle.getHttpHelper();
      if (httpHelper instanceof HttpHelper) {
         ((HttpHelper)httpHelper).setRetryPolicy(new IMDSRetryPolicy());
      }

      if (!StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable("AZURE_POD_IDENTITY_AUTHORITY_HOST"))) {
         LOG.info(
            String.format(
               "[Managed Identity] Environment variable AZURE_POD_IDENTITY_AUTHORITY_HOST for IMDS returned endpoint: %s",
               environmentVariables.getEnvironmentVariable("AZURE_POD_IDENTITY_AUTHORITY_HOST")
            )
         );

         try {
            this.imdsEndpoint = new URI(environmentVariables.getEnvironmentVariable("AZURE_POD_IDENTITY_AUTHORITY_HOST"));
         } catch (URISyntaxException var8) {
            throw new RuntimeException(var8);
         }

         StringBuilder builder = new StringBuilder(environmentVariables.getEnvironmentVariable("AZURE_POD_IDENTITY_AUTHORITY_HOST"));
         builder.append("//metadata/identity/oauth2/token");

         try {
            this.imdsEndpoint = new URI(builder.toString());
         } catch (URISyntaxException var7) {
            throw new MsalServiceException(
               String.format(
                  "[Managed Identity] The environment variable %s contains an invalid Uri %s in %s managed identity source.",
                  "AZURE_POD_IDENTITY_AUTHORITY_HOST",
                  builder.toString(),
                  ManagedIdentitySourceType.IMDS
               ),
               "invalid_managed_identity_endpoint",
               ManagedIdentitySourceType.IMDS
            );
         }
      } else {
         LOG.info("[Managed Identity] Unable to find AZURE_POD_IDENTITY_AUTHORITY_HOST environment variable for IMDS, using the default endpoint.");
         this.imdsEndpoint = DEFAULT_IMDS_ENDPOINT;
      }

      LOG.info(String.format("[Managed Identity] Creating IMDS managed identity source. Endpoint URI: %s", this.imdsEndpoint));
   }

   @Override
   public void createManagedIdentityRequest(String resource) {
      this.managedIdentityRequest.baseEndpoint = this.imdsEndpoint;
      this.managedIdentityRequest.method = HttpMethod.GET;
      this.managedIdentityRequest.headers = new HashMap<>();
      this.managedIdentityRequest.headers.put("Metadata", "true");
      this.managedIdentityRequest.queryParameters = new HashMap<>();
      this.managedIdentityRequest.queryParameters.put("api-version", "2018-02-01");
      this.managedIdentityRequest.queryParameters.put("resource", resource);
      if (this.idType != null && !StringHelper.isNullOrBlank(this.userAssignedId)) {
         LOG.info("[Managed Identity] Adding user assigned ID to the request for IMDS Managed Identity.");
         this.managedIdentityRequest.addUserAssignedIdToQuery(this.idType, this.userAssignedId);
      }
   }

   @Override
   public ManagedIdentityResponse handleResponse(ManagedIdentityParameters parameters, IHttpResponse response) {
      String baseMessage;
      if (response.statusCode() == 400) {
         baseMessage = "[Managed Identity] Authentication unavailable. The requested identity has not been assigned to this resource.";
      } else if (response.statusCode() != 502 && response.statusCode() != 504) {
         baseMessage = null;
      } else {
         baseMessage = "[Managed Identity] Authentication unavailable. The request failed due to a gateway error.";
      }

      if (baseMessage != null) {
         String message = createRequestFailedMessage(response, baseMessage);
         String errorContentMessage = this.getMessageFromErrorResponse(response);
         message = message + " " + errorContentMessage;
         LOG.error(String.format("Error message: %s Http status code: %s", message, response.statusCode()));
         throw new MsalServiceException(message, "managed_identity_request_failed", ManagedIdentitySourceType.IMDS);
      } else {
         return super.handleResponse(parameters, response);
      }
   }

   private static String createRequestFailedMessage(IHttpResponse response, String message) {
      StringBuilder messageBuilder = new StringBuilder();
      messageBuilder.append(StringHelper.isNullOrBlank(message) ? "[Managed Identity] Service request failed." : message);
      messageBuilder.append("Status: ");
      messageBuilder.append(response.statusCode());
      if (response.body() != null) {
         messageBuilder.append("Content:").append(response.body());
      }

      messageBuilder.append("Headers:");

      for (String key : response.headers().keySet()) {
         messageBuilder.append(key).append(response.headers().get(key));
      }

      return messageBuilder.toString();
   }

   static {
      try {
         DEFAULT_IMDS_ENDPOINT = new URI("http://169.254.169.254/metadata/identity/oauth2/token");
      } catch (URISyntaxException var1) {
         throw new MsalServiceException(var1.getMessage(), "invalid_managed_identity_endpoint", ManagedIdentitySourceType.IMDS);
      }
   }
}
