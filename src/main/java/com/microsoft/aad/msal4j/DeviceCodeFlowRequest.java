package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

class DeviceCodeFlowRequest extends MsalRequest {
   private AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference;
   private DeviceCodeFlowParameters parameters;
   private String scopesStr;

   DeviceCodeFlowRequest(
      DeviceCodeFlowParameters parameters,
      AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference,
      PublicClientApplication application,
      RequestContext requestContext
   ) {
      super(application, null, requestContext);
      this.parameters = parameters;
      this.scopesStr = String.join(" ", parameters.scopes());
      this.futureReference = futureReference;
   }

   DeviceCode acquireDeviceCode(String url, String clientId, Map<String, String> clientDataHeaders, ServiceBundle serviceBundle) {
      Map<String, String> headers = this.appendToHeaders(clientDataHeaders);
      String bodyParams = this.createQueryParams(clientId);
      HttpRequest httpRequest = new HttpRequest(HttpMethod.POST, url, headers, bodyParams);
      IHttpResponse response = serviceBundle.getHttpHelper().executeHttpRequest(httpRequest, this.requestContext(), serviceBundle);
      if (response.statusCode() != 200) {
         throw MsalServiceExceptionFactory.fromHttpResponse(response);
      } else {
         return this.parseJsonToDeviceCodeAndSetParameters(response.body(), headers, clientId);
      }
   }

   void createAuthenticationGrant(DeviceCode deviceCode) {
      Map<String, String> params = new LinkedHashMap<>();
      params.put("grant_type", "device_code");
      params.put("device_code", deviceCode.deviceCode());
      if (this.parameters.claims() != null) {
         params.put("claims", this.parameters.claims().formatAsJSONString());
      }

      this.msalAuthorizationGrant = new OAuthAuthorizationGrant(params, Collections.singleton(deviceCode.scopes()), this.parameters.claims());
   }

   private String createQueryParams(String clientId) {
      Map<String, String> queryParameters = new HashMap<>();
      queryParameters.put("client_id", clientId);
      String scopesParam = String.join(" ", AbstractMsalAuthorizationGrant.COMMON_SCOPES) + " " + this.scopesStr;
      queryParameters.put("scope", scopesParam);
      return StringHelper.serializeQueryParameters(queryParameters);
   }

   private Map<String, String> appendToHeaders(Map<String, String> clientDataHeaders) {
      Map<String, String> headers = new HashMap<>(clientDataHeaders);
      headers.put("Accept", "application/json");
      return headers;
   }

   private DeviceCode parseJsonToDeviceCodeAndSetParameters(String json, Map<String, String> headers, String clientId) {
      DeviceCode result = JsonHelper.convertJsonStringToJsonSerializableObject(json, DeviceCode::fromJson);
      String correlationIdHeader = headers.get("client-request-id");
      if (correlationIdHeader != null) {
         result.correlationId(correlationIdHeader);
      }

      result.clientId(clientId);
      result.scopes(this.scopesStr);
      return result;
   }

   AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference() {
      return this.futureReference;
   }

   DeviceCodeFlowParameters parameters() {
      return this.parameters;
   }

   String scopesStr() {
      return this.scopesStr;
   }
}
