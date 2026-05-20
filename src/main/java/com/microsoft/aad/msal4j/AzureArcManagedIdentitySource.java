package com.microsoft.aad.msal4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AzureArcManagedIdentitySource extends AbstractManagedIdentitySource {
   private static final Logger LOG = LoggerFactory.getLogger(AzureArcManagedIdentitySource.class);
   private static final String ARC_API_VERSION = "2019-11-01";
   private static final String AZURE_ARC = "Azure Arc";
   private static final String WINDOWS_PATH = System.getenv("ProgramData") + "/AzureConnectedMachineAgent/Tokens/";
   private static final String LINUX_PATH = "/var/opt/azcmagent/tokens/";
   private static final String FILE_EXTENSION = ".key";
   private static final int MAX_FILE_SIZE_BYTES = 4096;
   private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
   private final URI MSI_ENDPOINT;

   static AbstractManagedIdentitySource create(MsalRequest msalRequest, ServiceBundle serviceBundle) {
      IEnvironmentVariables environmentVariables = getEnvironmentVariables();
      String identityEndpoint = environmentVariables.getEnvironmentVariable("IDENTITY_ENDPOINT");
      String imdsEndpoint = environmentVariables.getEnvironmentVariable("IMDS_ENDPOINT");
      URI validatedUri = validateAndGetUri(identityEndpoint, imdsEndpoint);
      return validatedUri == null ? null : new AzureArcManagedIdentitySource(validatedUri, msalRequest, serviceBundle);
   }

   private static URI validateAndGetUri(String identityEndpoint, String imdsEndpoint) {
      if (!StringHelper.isNullOrBlank(identityEndpoint) && !StringHelper.isNullOrBlank(imdsEndpoint)) {
         URI endpointUri;
         try {
            endpointUri = new URI(identityEndpoint);
         } catch (URISyntaxException var4) {
            throw new MsalServiceException(
               String.format(
                  "[Managed Identity] The environment variable %s contains an invalid Uri %s in %s managed identity source.",
                  "IDENTITY_ENDPOINT",
                  identityEndpoint,
                  "Azure Arc"
               ),
               "invalid_managed_identity_endpoint",
               ManagedIdentitySourceType.AZURE_ARC
            );
         }

         LOG.info(String.format("[Managed Identity] Creating Azure Arc managed identity. Endpoint URI: %s", endpointUri));
         return endpointUri;
      } else {
         LOG.info("[Managed Identity] Azure Arc managed identity is unavailable.");
         return null;
      }
   }

   private AzureArcManagedIdentitySource(URI endpoint, MsalRequest msalRequest, ServiceBundle serviceBundle) {
      super(msalRequest, serviceBundle, ManagedIdentitySourceType.AZURE_ARC);
      this.MSI_ENDPOINT = endpoint;
      ManagedIdentityIdType idType = ((ManagedIdentityApplication)msalRequest.application()).getManagedIdentityId().getIdType();
      if (idType != ManagedIdentityIdType.SYSTEM_ASSIGNED) {
         throw new MsalServiceException(
            String.format(
               "[Managed Identity] User assigned identity is not supported by the %s Managed Identity. To authenticate with the system assigned identity use ManagedIdentityApplication.builder(ManagedIdentityId.systemAssigned()).build().",
               "Azure Arc"
            ),
            "user_assigned_managed_identity_not_supported",
            ManagedIdentitySourceType.AZURE_ARC
         );
      }
   }

   @Override
   public void createManagedIdentityRequest(String resource) {
      this.managedIdentityRequest.baseEndpoint = this.MSI_ENDPOINT;
      this.managedIdentityRequest.method = HttpMethod.GET;
      this.managedIdentityRequest.headers = new HashMap<>();
      this.managedIdentityRequest.headers.put("Metadata", "true");
      this.managedIdentityRequest.queryParameters = new HashMap<>();
      this.managedIdentityRequest.queryParameters.put("api-version", "2019-11-01");
      this.managedIdentityRequest.queryParameters.put("resource", resource);
   }

   @Override
   public ManagedIdentityResponse handleResponse(ManagedIdentityParameters parameters, IHttpResponse response) {
      LOG.info("[Managed Identity] Response received. Status code: {}", response.statusCode());
      if (response.statusCode() == 401) {
         String challenge = this.readChallengeFrom(response)
            .orElseGet(
               () -> {
                  LOG.error("[Managed Identity] {} is expected but not found.", "WWW-Authenticate");
                  throw new MsalServiceException(
                     "[Managed Identity] Did not receive expected WWW-Authenticate header in the response from Azure Arc Managed Identity Endpoint.",
                     "managed_identity_request_failed",
                     ManagedIdentitySourceType.AZURE_ARC
                  );
               }
            );
         String[] splitChallenge = challenge.split("=");
         if (splitChallenge.length != 2) {
            LOG.error("[Managed Identity] The {} header for Azure arc managed identity is not an expected format.", "WWW-Authenticate");
            throw new MsalServiceException(
               "[Managed Identity] The WWW-Authenticate header in the response from Azure Arc Managed Identity Endpoint did not match the expected format.",
               "managed_identity_request_failed",
               ManagedIdentitySourceType.AZURE_ARC
            );
         } else {
            Path path = Paths.get(splitChallenge[1]).normalize();
            this.validateFile(path);
            if (!path.toFile().exists()) {
               LOG.error("[Managed Identity] The WWW-Authenticate header specifies a file that does not exist");
               throw new MsalServiceException(
                  "[Managed Identity] The file on the file path in the WWW-Authenticate header is not secure or could not be found.",
                  "managed_identity_file_read_error",
                  ManagedIdentitySourceType.AZURE_ARC
               );
            } else {
               String authHeaderValue = null;

               try {
                  authHeaderValue = "Basic " + new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
               } catch (IOException var9) {
                  throw new MsalServiceException(var9.getMessage(), "managed_identity_file_read_error", ManagedIdentitySourceType.AZURE_ARC);
               }

               this.createManagedIdentityRequest(parameters.resource);
               LOG.info("[Managed Identity] Adding authorization header to the request.");
               this.managedIdentityRequest.headers.put("Authorization", authHeaderValue);

               try {
                  response = this.serviceBundle
                     .getHttpHelper()
                     .executeHttpRequest(
                        new HttpRequest(HttpMethod.GET, this.managedIdentityRequest.computeURI().toString(), this.managedIdentityRequest.headers),
                        this.managedIdentityRequest.requestContext(),
                        this.serviceBundle
                     );
               } catch (URISyntaxException var8) {
                  throw new MsalServiceException(
                     "[Managed Identity] The environment variable %s contains an invalid Uri %s in %s managed identity source.",
                     "invalid_managed_identity_endpoint",
                     this.managedIdentitySourceType
                  );
               }

               return super.handleResponse(parameters, response);
            }
         }
      } else {
         return super.handleResponse(parameters, response);
      }
   }

   private Optional<String> readChallengeFrom(IHttpResponse response) {
      return response.headers()
         .entrySet()
         .stream()
         .filter(entry -> "WWW-Authenticate".equalsIgnoreCase(entry.getKey()))
         .map(Entry::getValue)
         .flatMap(Collection::stream)
         .findFirst();
   }

   private void validateFile(Path path) {
      String osName = System.getProperty("os.name").toLowerCase();
      if (!osName.contains("windows") && !osName.contains("linux")) {
         LOG.error(String.format("[Managed Identity] Unsupported platform: %s", osName));
         throw new MsalServiceException(
            "[Managed Identity] This managed identity source is not available on this platform.",
            "managed_identity_file_read_error",
            ManagedIdentitySourceType.AZURE_ARC
         );
      } else if (!this.isValidWindowsPath(path) && !this.isValidLinuxPath(path)) {
         LOG.error("[Managed Identity] Invalid filepath.");
         throw new MsalServiceException(
            "[Managed Identity] The file on the file path in the WWW-Authenticate header is not secure or could not be found.",
            "managed_identity_file_read_error",
            ManagedIdentitySourceType.AZURE_ARC
         );
      } else if (path.toFile().length() > 4096L) {
         LOG.error(String.format("[Managed Identity] File is larger than %s bytes.", 4096));
         throw new MsalServiceException(
            "[Managed Identity] The file on the file path in the WWW-Authenticate header is not secure or could not be found.",
            "managed_identity_file_read_error",
            ManagedIdentitySourceType.AZURE_ARC
         );
      } else {
         LOG.info("[Managed Identity] Path passed validation.");
      }
   }

   private boolean isValidWindowsPath(Path path) {
      return path.startsWith(WINDOWS_PATH) && path.toString().toLowerCase().endsWith(".key");
   }

   private boolean isValidLinuxPath(Path path) {
      return path.startsWith("/var/opt/azcmagent/tokens/") && path.toString().toLowerCase().endsWith(".key");
   }
}
