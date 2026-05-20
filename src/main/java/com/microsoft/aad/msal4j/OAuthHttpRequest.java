package com.microsoft.aad.msal4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class OAuthHttpRequest {
   final HttpMethod method;
   final URL url;
   String query;
   private final Map<String, String> extraHeaderParams;
   private final ServiceBundle serviceBundle;
   private final RequestContext requestContext;

   OAuthHttpRequest(HttpMethod method, URL url, Map<String, String> extraHeaderParams, RequestContext requestContext, ServiceBundle serviceBundle) {
      this.method = method;
      this.url = url;
      this.extraHeaderParams = extraHeaderParams;
      this.requestContext = requestContext;
      this.serviceBundle = serviceBundle;
   }

   public HttpResponse send() throws IOException {
      Map<String, String> httpHeaders = this.configureHttpHeaders();
      HttpRequest httpRequest = new HttpRequest(HttpMethod.POST, this.url.toString(), httpHeaders, this.query);
      IHttpResponse httpResponse = this.serviceBundle.getHttpHelper().executeHttpRequest(httpRequest, this.requestContext, this.serviceBundle);
      return this.createOauthHttpResponseFromHttpResponse(httpResponse);
   }

   private Map<String, String> configureHttpHeaders() {
      Map<String, String> httpHeaders = new HashMap<>(this.extraHeaderParams);
      httpHeaders.put("Content-Type", HTTPContentType.ApplicationURLEncoded.contentType);
      Map<String, String> telemetryHeaders = this.serviceBundle.getServerSideTelemetry().getServerTelemetryHeaderMap();
      httpHeaders.putAll(telemetryHeaders);
      return httpHeaders;
   }

   private HttpResponse createOauthHttpResponseFromHttpResponse(IHttpResponse httpResponse) throws IOException {
      HttpResponse response = new HttpResponse();
      response.statusCode(httpResponse.statusCode());
      String location = HttpUtils.headerValue(httpResponse.headers(), "Location");
      if (!StringHelper.isBlank(location)) {
         try {
            response.addHeader("Location", new URI(location).toString());
         } catch (URISyntaxException var9) {
            throw new IOException("Invalid location URI " + location, var9);
         }
      }

      String contentType = HttpUtils.headerValue(httpResponse.headers(), "Content-Type");
      if (!StringHelper.isBlank(contentType)) {
         response.addHeader("Content-Type", contentType);
      }

      Map<String, List<String>> headers = httpResponse.headers();

      for (Entry<String, List<String>> header : headers.entrySet()) {
         if (!StringHelper.isBlank(header.getKey())) {
            List<String> headerValue = response.getHeader(header.getKey());
            if (headerValue == null) {
               response.addHeader(header.getKey(), header.getValue().toArray(new String[0]));
            }
         }
      }

      if (!StringHelper.isBlank(httpResponse.body())) {
         response.body(httpResponse.body());
      }

      return response;
   }

   void setQuery(String query) {
      this.query = query;
   }

   Map<String, String> getExtraHeaderParams() {
      return this.extraHeaderParams;
   }
}
