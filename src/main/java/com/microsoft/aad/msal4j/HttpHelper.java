package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpHelper implements IHttpHelper {
   private static final Logger log = LoggerFactory.getLogger(HttpHelper.class);
   public static final String RETRY_AFTER_HEADER = "Retry-After";
   private IHttpClient httpClient;
   private IRetryPolicy retryPolicy;
   private boolean retryDisabled;

   HttpHelper(IHttpClient httpClient, IRetryPolicy retryPolicy) {
      this.httpClient = httpClient;
      this.retryPolicy = (IRetryPolicy)(retryPolicy != null ? retryPolicy : new DefaultRetryPolicy());
   }

   HttpHelper(AbstractApplicationBase application, IRetryPolicy retryPolicy) {
      this.httpClient = application.httpClient();
      this.retryDisabled = application.isRetryDisabled();
      this.retryPolicy = (IRetryPolicy)(retryPolicy != null ? retryPolicy : new DefaultRetryPolicy());
   }

   @Override
   public IHttpResponse executeHttpRequest(HttpRequest httpRequest, RequestContext requestContext, ServiceBundle serviceBundle) {
      this.checkForThrottling(requestContext);
      HttpEvent httpEvent = new HttpEvent();

      IHttpResponse httpResponse;
      try (TelemetryHelper telemetryHelper = serviceBundle.getTelemetryManager()
            .createTelemetryHelper(requestContext.telemetryRequestId(), requestContext.clientId(), httpEvent, false)) {
         this.addRequestInfoToTelemetry(httpRequest, httpEvent);

         try {
            httpResponse = this.executeHttpRequestWithRetries(httpRequest, this.httpClient);
         } catch (Exception var17) {
            httpEvent.setOauthErrorCode("unknown");
            throw new MsalClientException(var17);
         }

         this.addResponseInfoToTelemetry(httpResponse, httpEvent);
         if (httpResponse.headers() != null) {
            verifyReturnedCorrelationId(httpRequest, httpResponse);
         }
      }

      this.processThrottlingInstructions(httpResponse, requestContext);
      return httpResponse;
   }

   IHttpResponse executeHttpRequest(HttpRequest httpRequest, RequestContext requestContext, TelemetryManager telemetryManager, IHttpClient httpClient) {
      this.checkForThrottling(requestContext);
      HttpEvent httpEvent = new HttpEvent();

      IHttpResponse httpResponse;
      try (TelemetryHelper telemetryHelper = telemetryManager.createTelemetryHelper(
            requestContext.telemetryRequestId(), requestContext.clientId(), httpEvent, false
         )) {
         this.addRequestInfoToTelemetry(httpRequest, httpEvent);

         try {
            httpResponse = this.executeHttpRequestWithRetries(httpRequest, httpClient);
         } catch (Exception var18) {
            httpEvent.setOauthErrorCode("unknown");
            throw new MsalClientException(var18);
         }

         this.addResponseInfoToTelemetry(httpResponse, httpEvent);
         if (httpResponse.headers() != null) {
            verifyReturnedCorrelationId(httpRequest, httpResponse);
         }
      }

      this.processThrottlingInstructions(httpResponse, requestContext);
      return httpResponse;
   }

   IHttpResponse executeHttpRequest(HttpRequest httpRequest) {
      IHttpResponse httpResponse;
      try {
         httpResponse = this.executeHttpRequestWithRetries(httpRequest, this.httpClient);
      } catch (Exception var4) {
         throw new MsalClientException(var4);
      }

      if (httpResponse.headers() != null) {
         verifyReturnedCorrelationId(httpRequest, httpResponse);
      }

      return httpResponse;
   }

   private String getRequestThumbprint(RequestContext requestContext) {
      StringBuilder sb = new StringBuilder();
      sb.append(requestContext.clientId() + ".");
      sb.append(requestContext.authority() + ".");
      IAcquireTokenParameters apiParameters = requestContext.apiParameters();
      if (apiParameters instanceof SilentParameters) {
         IAccount account = ((SilentParameters)apiParameters).account();
         if (account != null) {
            sb.append(account.homeAccountId() + ".");
         }
      }

      Set<String> sortedScopes = new TreeSet<>(apiParameters.scopes());
      sb.append(String.join(" ", sortedScopes));
      return StringHelper.createSha256Hash(sb.toString());
   }

   IHttpResponse executeHttpRequestWithRetries(HttpRequest httpRequest, IHttpClient httpClient) throws Exception {
      IHttpResponse httpResponse = httpClient.send(httpRequest);
      if (this.retryDisabled) {
         return httpResponse;
      } else {
         int retryCount = 0;

         for (int maxRetries = this.retryPolicy.getMaxRetryCount(httpResponse);
            this.retryPolicy.isRetryable(httpResponse) && retryCount < maxRetries;
            httpResponse = httpClient.send(httpRequest)
         ) {
            Thread.sleep(this.retryPolicy.getRetryDelayMs(httpResponse));
            retryCount++;
         }

         return httpResponse;
      }
   }

   private void checkForThrottling(RequestContext requestContext) {
      if (requestContext.clientApplication() instanceof PublicClientApplication && requestContext.apiParameters() != null) {
         String requestThumbprint = this.getRequestThumbprint(requestContext);
         long retryInMs = ThrottlingCache.retryInMs(requestThumbprint);
         if (retryInMs > 0L) {
            throw new MsalThrottlingException(retryInMs);
         }
      }
   }

   private void processThrottlingInstructions(IHttpResponse httpResponse, RequestContext requestContext) {
      if (requestContext.clientApplication() instanceof PublicClientApplication) {
         Long expirationTimestamp = null;
         Integer retryAfterHeaderVal = getRetryAfterHeader(httpResponse);
         if (retryAfterHeaderVal != null) {
            expirationTimestamp = System.currentTimeMillis() + retryAfterHeaderVal * 1000;
         } else if (httpResponse.statusCode() == 429 || httpResponse.statusCode() >= 500) {
            expirationTimestamp = System.currentTimeMillis() + ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC * 1000;
         }

         if (expirationTimestamp != null) {
            ThrottlingCache.set(this.getRequestThumbprint(requestContext), expirationTimestamp);
         }
      }
   }

   static Integer getRetryAfterHeader(IHttpResponse httpResponse) {
      if (httpResponse.headers() != null) {
         TreeMap<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
         headers.putAll(httpResponse.headers());
         if (headers.containsKey("Retry-After") && headers.get("Retry-After").size() == 1) {
            try {
               int headerValue = Integer.parseInt(headers.get("Retry-After").get(0));
               if (headerValue > 0 && headerValue <= 3600) {
                  return headerValue;
               }
            } catch (NumberFormatException var3) {
               log.warn("Failed to parse value of Retry-After header - NumberFormatException");
            }
         }
      }

      return null;
   }

   private void addRequestInfoToTelemetry(HttpRequest httpRequest, HttpEvent httpEvent) {
      try {
         httpEvent.setHttpPath(httpRequest.url().toURI());
         httpEvent.setHttpMethod(httpRequest.httpMethod().toString());
         if (!StringHelper.isBlank(httpRequest.url().getQuery())) {
            httpEvent.setQueryParameters(httpRequest.url().getQuery());
         }
      } catch (Exception var5) {
         String correlationId = httpRequest.headerValue("client-request-id");
         log.warn(
            LogHelper.createMessage("Setting URL telemetry fields failed: " + LogHelper.getPiiScrubbedDetails(var5), correlationId != null ? correlationId : "")
         );
      }
   }

   private void addResponseInfoToTelemetry(IHttpResponse httpResponse, HttpEvent httpEvent) {
      httpEvent.setHttpResponseStatus(httpResponse.statusCode());
      Map<String, List<String>> headers = httpResponse.headers();
      String userAgent = HttpUtils.headerValue(headers, "User-Agent");
      if (!StringHelper.isBlank(userAgent)) {
         httpEvent.setUserAgent(userAgent);
      }

      String xMsRequestId = HttpUtils.headerValue(headers, "x-ms-request-id");
      if (!StringHelper.isBlank(xMsRequestId)) {
         httpEvent.setRequestIdHeader(xMsRequestId);
      }

      String xMsClientTelemetry = HttpUtils.headerValue(headers, "x-ms-clitelem");
      if (xMsClientTelemetry != null) {
         XmsClientTelemetryInfo xmsClientTelemetryInfo = XmsClientTelemetryInfo.parseXmsTelemetryInfo(xMsClientTelemetry);
         if (xmsClientTelemetryInfo != null) {
            httpEvent.setXmsClientTelemetryInfo(xmsClientTelemetryInfo);
         }
      }
   }

   private static void verifyReturnedCorrelationId(HttpRequest httpRequest, IHttpResponse httpResponse) {
      String sentCorrelationId = httpRequest.headerValue("client-request-id");
      String returnedCorrelationId = HttpUtils.headerValue(httpResponse.headers(), "client-request-id");
      if (StringHelper.isBlank(returnedCorrelationId) || !returnedCorrelationId.equals(sentCorrelationId)) {
         String msg = LogHelper.createMessage(
            String.format("Sent (%s) Correlation Id is not same as received (%s).", sentCorrelationId, returnedCorrelationId), sentCorrelationId
         );
         log.info(msg);
      }
   }

   void setRetryPolicy(IRetryPolicy retryPolicy) {
      this.retryPolicy = retryPolicy;
   }
}
