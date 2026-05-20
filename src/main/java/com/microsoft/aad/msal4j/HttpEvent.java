package com.microsoft.aad.msal4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

class HttpEvent extends Event {
   private static final String HTTP_PATH_KEY = "msal.http_path";
   private static final String USER_AGENT_KEY = "msal.user_agent";
   private static final String QUERY_PARAMETERS_KEY = "msal.query_parameters";
   private static final String API_VERSION_KEY = "msal.api_version";
   private static final String RESPONSE_CODE_KEY = "msal.response_code";
   private static final String OAUTH_ERROR_CODE_KEY = "msal.oauth_error_code";
   private static final String HTTP_METHOD_KEY = "msal.http_method";
   private static final String REQUEST_ID_HEADER_KEY = "msal.request_id_header";
   private static final String TOKEN_AGEN_KEY = "msal.token_age";
   private static final String SPE_INFO_KEY = "msal.spe_info";
   private static final String SERVER_ERROR_CODE_KEY = "msal.server_error_code";
   private static final String SERVER_SUB_ERROR_CODE_KEY = "msal.server_sub_error_code";

   HttpEvent() {
      super("msal.http_event");
   }

   void setHttpPath(URI httpPath) {
      this.put("msal.http_path", scrubTenant(httpPath));
   }

   void setUserAgent(String userAgent) {
      this.put("msal.user_agent", userAgent.toLowerCase(Locale.ROOT));
   }

   void setQueryParameters(String queryParameters) {
      this.put("msal.query_parameters", String.join("&", this.parseQueryParametersAndReturnKeys(queryParameters)));
   }

   void setApiVersion(String apiVersion) {
      this.put("msal.api_version", apiVersion.toLowerCase());
   }

   void setHttpResponseStatus(Integer httpResponseStatus) {
      this.put("msal.response_code", httpResponseStatus.toString().toLowerCase());
   }

   void setHttpMethod(String httpMethod) {
      this.put("msal.http_method", httpMethod);
   }

   void setOauthErrorCode(String oauthErrorCode) {
      this.put("msal.oauth_error_code", oauthErrorCode.toLowerCase());
   }

   void setRequestIdHeader(String requestIdHeader) {
      this.put("msal.request_id_header", requestIdHeader.toLowerCase());
   }

   private void setTokenAge(String tokenAge) {
      this.put("msal.token_age", tokenAge.toLowerCase());
   }

   private void setSpeInfo(String speInfo) {
      this.put("msal.spe_info", speInfo.toLowerCase());
   }

   private void setServerErrorCode(String serverErrorCode) {
      this.put("msal.server_error_code", serverErrorCode.toLowerCase());
   }

   private void setSubServerErrorCode(String subServerErrorCode) {
      this.put("msal.server_sub_error_code", subServerErrorCode.toLowerCase());
   }

   void setXmsClientTelemetryInfo(XmsClientTelemetryInfo xmsClientTelemetryInfo) {
      this.setTokenAge(xmsClientTelemetryInfo.getTokenAge());
      this.setSpeInfo(xmsClientTelemetryInfo.getSpeInfo());
      this.setServerErrorCode(xmsClientTelemetryInfo.getServerErrorCode());
      this.setSubServerErrorCode(xmsClientTelemetryInfo.getServerSubErrorCode());
   }

   private ArrayList<String> parseQueryParametersAndReturnKeys(String queryParams) {
      ArrayList<String> queryKeys = new ArrayList<>();
      String[] queryStrings = queryParams.split("&");

      for (String queryString : queryStrings) {
         String[] queryPairs = queryString.split("=");
         if (queryPairs.length == 2 && !StringHelper.isBlank(queryPairs[0]) && !StringHelper.isBlank(queryPairs[1])) {
            queryKeys.add(queryPairs[0].toLowerCase(Locale.ROOT));
         }
      }

      return queryKeys;
   }
}
