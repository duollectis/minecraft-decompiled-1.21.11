package com.microsoft.aad.msal4j;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AadInstanceDiscoveryProvider {
   private static final String DEFAULT_TRUSTED_HOST = "login.microsoftonline.com";
   private static final String AUTHORIZE_ENDPOINT_TEMPLATE = "https://{host}/{tenant}/oauth2/v2.0/authorize";
   private static final String INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE = "https://{host}:{port}/common/discovery/instance";
   private static final String INSTANCE_DISCOVERY_REQUEST_PARAMETERS_TEMPLATE = "?api-version=1.1&authorization_endpoint={authorizeEndpoint}";
   private static final String HOST_TEMPLATE_WITH_REGION = "{region}.login.microsoft.com";
   private static final String SOVEREIGN_HOST_TEMPLATE_WITH_REGION = "{region}.{host}";
   private static final String REGION_NAME = "REGION_NAME";
   private static final int PORT_NOT_SET = -1;
   private static final String DEFAULT_API_VERSION = "2020-06-01";
   private static final String IMDS_ENDPOINT = "http://169.254.169.254/metadata/instance/compute/location?api-version=2020-06-01&format=text";
   private static final int IMDS_TIMEOUT = 2;
   private static final TimeUnit IMDS_TIMEOUT_UNIT = TimeUnit.SECONDS;
   static final TreeSet<String> TRUSTED_HOSTS_SET = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
   static final TreeSet<String> TRUSTED_SOVEREIGN_HOSTS_SET = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
   private static final Logger log = LoggerFactory.getLogger(AadInstanceDiscoveryProvider.class);
   private static boolean instanceDiscoveryFailed = false;
   static ConcurrentHashMap<String, InstanceDiscoveryMetadataEntry> cache = new ConcurrentHashMap<>();

   static InstanceDiscoveryMetadataEntry getMetadataEntry(URL authorityUrl, boolean validateAuthority, MsalRequest msalRequest, ServiceBundle serviceBundle) {
      String host = authorityUrl.getHost();
      if (!(msalRequest.application() instanceof ManagedIdentityApplication) && ((AbstractClientApplicationBase)msalRequest.application()).instanceDiscovery()) {
         if (shouldUseRegionalEndpoint(msalRequest) && ((AbstractClientApplicationBase)msalRequest.application()).azureRegion() != null) {
            host = getRegionalizedHost(authorityUrl.getHost(), ((AbstractClientApplicationBase)msalRequest.application()).azureRegion());
         }

         if (cache.get(host) == null) {
            log.debug("No cached instance metadata, will attempt instance discovery.");
            if (shouldUseRegionalEndpoint(msalRequest)) {
               log.debug("Region API used, will attempt to discover Azure region.");
               String detectedRegion = discoverRegion(msalRequest, serviceBundle);
               if (((AbstractClientApplicationBase)msalRequest.application()).azureRegion() == null
                  && ((AbstractClientApplicationBase)msalRequest.application()).autoDetectRegion()
                  && detectedRegion != null) {
                  log.debug(String.format("Region autodetection found %s, this region will be used for future calls.", detectedRegion));
                  ((AbstractClientApplicationBase)msalRequest.application()).azureRegion = detectedRegion;
                  host = getRegionalizedHost(authorityUrl.getHost(), ((AbstractClientApplicationBase)msalRequest.application()).azureRegion());
               }

               cacheRegionInstanceMetadata(authorityUrl.getHost(), host);
               serviceBundle.getServerSideTelemetry()
                  .getCurrentRequest()
                  .regionOutcome(
                     determineRegionOutcome(
                        detectedRegion,
                        ((AbstractClientApplicationBase)msalRequest.application()).azureRegion(),
                        ((AbstractClientApplicationBase)msalRequest.application()).autoDetectRegion()
                     )
                  );
            }

            doInstanceDiscoveryAndCache(authorityUrl, validateAuthority, msalRequest, serviceBundle);
         }

         return cache.get(host);
      } else {
         if (cache.get(host) == null) {
            log.debug("Instance discovery set to false, caching a default entry.");
            cacheInstanceDiscoveryMetadata(host);
         }

         return cache.get(host);
      }
   }

   static Set<String> getAliases(String host) {
      return cache.containsKey(host) ? cache.get(host).aliases() : Collections.singleton(host);
   }

   static AadInstanceDiscoveryResponse parseInstanceDiscoveryMetadata(String instanceDiscoveryJson) {
      try {
         return JsonHelper.convertJsonStringToJsonSerializableObject(instanceDiscoveryJson, AadInstanceDiscoveryResponse::fromJson);
      } catch (Exception var2) {
         throw new MsalClientException(
            "Error parsing instance discovery response. Data must be in valid JSON format. For more information, see https://aka.ms/msal4j-instance-discovery",
            "invalid_instance_discovery_metadata"
         );
      }
   }

   static void cacheInstanceDiscoveryResponse(String host, AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse) {
      if (aadInstanceDiscoveryResponse != null && aadInstanceDiscoveryResponse.metadata() != null) {
         for (InstanceDiscoveryMetadataEntry entry : aadInstanceDiscoveryResponse.metadata()) {
            for (String alias : entry.aliases()) {
               cache.put(alias, entry);
            }
         }
      }

      cacheInstanceDiscoveryMetadata(host);
   }

   static void cacheInstanceDiscoveryMetadata(String host) {
      cache.putIfAbsent(host, new InstanceDiscoveryMetadataEntry(host, host, Collections.singleton(host)));
   }

   private static boolean shouldUseRegionalEndpoint(MsalRequest msalRequest) {
      if (((AbstractClientApplicationBase)msalRequest.application()).azureRegion() == null
         && !((AbstractClientApplicationBase)msalRequest.application()).autoDetectRegion()) {
         return false;
      } else if (msalRequest.getClass() == ClientCredentialRequest.class) {
         return true;
      } else {
         if (msalRequest.getClass() != SilentRequest.class) {
            log.warn(
               "Regional endpoints are only available for client credential flow, request will fall back to using the global endpoint. See here for more information about supported scenarios: https://aka.ms/msal4j-azure-regions"
            );
         }

         return false;
      }
   }

   static void cacheRegionInstanceMetadata(String originalHost, String regionalHost) {
      Set<String> aliases = new HashSet<>();
      aliases.add(originalHost);
      cache.putIfAbsent(regionalHost, new InstanceDiscoveryMetadataEntry(regionalHost, originalHost, aliases));
   }

   private static String getRegionalizedHost(String host, String region) {
      if (region == null) {
         return host;
      } else if (host.contains(region)) {
         return host;
      } else {
         String regionalizedHost;
         if (TRUSTED_HOSTS_SET.contains(host) && !TRUSTED_SOVEREIGN_HOSTS_SET.contains(host)) {
            regionalizedHost = "{region}.login.microsoft.com".replace("{region}", region);
         } else {
            regionalizedHost = "{region}.{host}".replace("{region}", region).replace("{host}", host);
         }

         return regionalizedHost;
      }
   }

   private static String getAuthorizeEndpoint(String host, String tenant) {
      return "https://{host}/{tenant}/oauth2/v2.0/authorize".replace("{host}", host).replace("{tenant}", tenant);
   }

   private static String getInstanceDiscoveryEndpoint(URL authorityUrl) {
      String discoveryHost = TRUSTED_HOSTS_SET.contains(authorityUrl.getHost()) ? authorityUrl.getHost() : "login.microsoftonline.com";
      int port = authorityUrl.getPort() == -1 ? authorityUrl.getDefaultPort() : authorityUrl.getPort();
      return "https://{host}:{port}/common/discovery/instance".replace("{host}", discoveryHost).replace("{port}", String.valueOf(port));
   }

   static AadInstanceDiscoveryResponse sendInstanceDiscoveryRequest(URL authorityUrl, MsalRequest msalRequest, ServiceBundle serviceBundle) {
      String instanceDiscoveryRequestUrl = getInstanceDiscoveryEndpoint(authorityUrl) + formInstanceDiscoveryParameters(authorityUrl);
      IHttpResponse httpResponse = executeRequest(instanceDiscoveryRequestUrl, msalRequest.headers().getReadonlyHeaderMap(), msalRequest, serviceBundle);
      AadInstanceDiscoveryResponse response = JsonHelper.convertJsonStringToJsonSerializableObject(httpResponse.body(), AadInstanceDiscoveryResponse::fromJson);
      if (httpResponse.statusCode() != 200) {
         if (httpResponse.statusCode() == 400 && response.error().equals("invalid_instance")) {
            throw MsalServiceExceptionFactory.fromHttpResponse(httpResponse);
         }

         log.debug("Instance discovery failed due to an unknown error, no more instance discovery attempts will be made.");
         cacheInstanceDiscoveryMetadata(authorityUrl.getHost());
      }

      return response;
   }

   private static int determineRegionOutcome(String detectedRegion, String providedRegion, boolean autoDetect) {
      int regionOutcomeTelemetryValue = 0;
      if (providedRegion != null) {
         if (detectedRegion == null) {
            regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_DEVELOPER_AUTODETECT_FAILED.telemetryValue;
         } else if (providedRegion.equals(detectedRegion)) {
            regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_DEVELOPER_AUTODETECT_MATCH.telemetryValue;
         } else {
            regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_DEVELOPER_AUTODETECT_MISMATCH.telemetryValue;
         }
      } else if (autoDetect) {
         if (detectedRegion == null) {
            regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_AUTODETECT_FAILED.telemetryValue;
         } else {
            regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_AUTODETECT_SUCCESS.telemetryValue;
         }
      }

      return regionOutcomeTelemetryValue;
   }

   private static String formInstanceDiscoveryParameters(URL authorityUrl) {
      return "?api-version=1.1&authorization_endpoint={authorizeEndpoint}"
         .replace(
            "{authorizeEndpoint}", getAuthorizeEndpoint(authorityUrl.getHost(), Authority.getTenant(authorityUrl, Authority.detectAuthorityType(authorityUrl)))
         );
   }

   private static IHttpResponse executeRequest(String requestUrl, Map<String, String> headers, MsalRequest msalRequest, ServiceBundle serviceBundle) {
      HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, requestUrl, headers);
      return serviceBundle.getHttpHelper().executeHttpRequest(httpRequest, msalRequest.requestContext(), serviceBundle);
   }

   static String discoverRegion(MsalRequest msalRequest, ServiceBundle serviceBundle) {
      CurrentRequest currentRequest = serviceBundle.getServerSideTelemetry().getCurrentRequest();
      if (System.getenv("REGION_NAME") != null) {
         log.info(String.format("Region found in environment variable: %s", System.getenv("REGION_NAME")));
         currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_ENV_VARIABLE.telemetryValue);
         return System.getenv("REGION_NAME");
      } else {
         Map<String, String> headers = new HashMap<>();
         headers.put("Metadata", "true");
         ExecutorService executor = Executors.newSingleThreadExecutor();
         Future<IHttpResponse> future = executor.submit(
            () -> executeRequest(
               "http://169.254.169.254/metadata/instance/compute/location?api-version=2020-06-01&format=text", headers, msalRequest, serviceBundle
            )
         );

         String var7;
         try {
            log.info("Starting call to IMDS endpoint.");
            IHttpResponse httpResponse = future.get(2L, IMDS_TIMEOUT_UNIT);
            if (httpResponse.statusCode() != 200 || httpResponse.body().isEmpty()) {
               log.warn(String.format("Call to local IMDS failed with status code: %s, or response was empty", httpResponse.statusCode()));
               currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_FAILED_AUTODETECT.telemetryValue);
               return null;
            }

            log.info(String.format("Region retrieved from IMDS endpoint: %s", httpResponse.body()));
            currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_IMDS.telemetryValue);
            var7 = httpResponse.body();
         } catch (Exception var11) {
            log.warn(String.format("Exception during call to local IMDS endpoint: %s", var11.getMessage()));
            currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_FAILED_AUTODETECT.telemetryValue);
            future.cancel(true);
            return null;
         } finally {
            executor.shutdownNow();
         }

         return var7;
      }
   }

   private static void doInstanceDiscoveryAndCache(URL authorityUrl, boolean validateAuthority, MsalRequest msalRequest, ServiceBundle serviceBundle) {
      AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse = null;
      if (msalRequest.application().authenticationAuthority.authorityType.equals(AuthorityType.AAD)) {
         aadInstanceDiscoveryResponse = sendInstanceDiscoveryRequest(authorityUrl, msalRequest, serviceBundle);
         if (validateAuthority) {
            validate(aadInstanceDiscoveryResponse);
         }
      }

      cacheInstanceDiscoveryResponse(authorityUrl.getHost(), aadInstanceDiscoveryResponse);
   }

   private static void validate(AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse) {
      if (StringHelper.isBlank(aadInstanceDiscoveryResponse.tenantDiscoveryEndpoint())) {
         throw new MsalServiceException(aadInstanceDiscoveryResponse);
      }
   }

   static {
      TRUSTED_SOVEREIGN_HOSTS_SET.addAll(
         Arrays.asList("login.chinacloudapi.cn", "login-us.microsoftonline.com", "login.microsoftonline.de", "login.microsoftonline.us")
      );
      TRUSTED_HOSTS_SET.addAll(Arrays.asList("login.windows.net", "login.microsoftonline.com", "login.microsoft.com", "sts.windows.net"));
      TRUSTED_HOSTS_SET.addAll(TRUSTED_SOVEREIGN_HOSTS_SET);
   }
}
