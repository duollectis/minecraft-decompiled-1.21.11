package com.microsoft.aad.msal4j;

import java.util.HashMap;
import java.util.Map;

class UserDiscoveryRequest {
   private static final Map<String, String> HEADERS = new HashMap<>();

   private UserDiscoveryRequest() {
   }

   static UserDiscoveryResponse execute(String uri, Map<String, String> clientDataHeaders, RequestContext requestContext, ServiceBundle serviceBundle) {
      HashMap<String, String> headers = new HashMap<>(HEADERS);
      headers.putAll(clientDataHeaders);
      HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, uri, headers);
      IHttpResponse response = serviceBundle.getHttpHelper().executeHttpRequest(httpRequest, requestContext, serviceBundle);
      if (response.statusCode() != 200) {
         throw MsalServiceExceptionFactory.fromHttpResponse(response);
      } else {
         return JsonHelper.convertJsonStringToJsonSerializableObject(response.body(), UserDiscoveryResponse::fromJson);
      }
   }

   static {
      HEADERS.put("Accept", "application/json, text/javascript, */*");
   }
}
