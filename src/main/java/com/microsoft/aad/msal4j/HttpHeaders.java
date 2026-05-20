package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

final class HttpHeaders {
   static final String PRODUCT_HEADER_NAME = "x-client-SKU";
   static final String PRODUCT_HEADER_VALUE = "MSAL.Java";
   static final String PRODUCT_VERSION_HEADER_NAME = "x-client-VER";
   static final String PRODUCT_VERSION_HEADER_VALUE = getProductVersion();
   static final String OS_HEADER_NAME = "x-client-OS";
   static final String OS_HEADER_VALUE = System.getProperty("os.name");
   static final String APPLICATION_NAME_HEADER_NAME = "x-app-name";
   private final String applicationNameHeaderValue;
   static final String APPLICATION_VERSION_HEADER_NAME = "x-app-ver";
   private final String applicationVersionHeaderValue;
   static final String CORRELATION_ID_HEADER_NAME = "client-request-id";
   private final String correlationIdHeaderValue;
   private static final String REQUEST_CORRELATION_ID_IN_RESPONSE_HEADER_NAME = "return-client-request-id";
   private static final String REQUEST_CORRELATION_ID_IN_RESPONSE_HEADER_VALUE = "true";
   private static final String X_MS_LIB_CAPABILITY_NAME = "x-ms-lib-capability";
   private static final String X_MS_LIB_CAPABILITY_VALUE = "retry-after, h429";
   static final String X_ANCHOR_MAILBOX = "X-AnchorMailbox";
   static final String X_ANCHOR_MAILBOX_OID_FORMAT = "oid:%s";
   static final String X_ANCHOR_MAILBOX_UPN_FORMAT = "upn:%s";
   private String anchorMailboxHeaderValue = null;
   private String headerValues;
   private Map<String, String> headerMap = new HashMap<>();

   HttpHeaders(RequestContext requestContext) {
      this.correlationIdHeaderValue = requestContext.correlationId();
      this.applicationNameHeaderValue = requestContext.applicationName();
      this.applicationVersionHeaderValue = requestContext.applicationVersion();
      if (requestContext.userIdentifier() != null) {
         String upn = requestContext.userIdentifier().upn();
         String oid = requestContext.userIdentifier().oid();
         if (!StringHelper.isBlank(upn)) {
            this.anchorMailboxHeaderValue = String.format("upn:%s", upn);
         } else if (!StringHelper.isBlank(oid)) {
            this.anchorMailboxHeaderValue = String.format("oid:%s", oid);
         }
      }

      Map<String, String> extraHttpHeaders = requestContext.apiParameters() == null ? null : requestContext.apiParameters().extraHttpHeaders();
      this.initializeHeaders(extraHttpHeaders);
   }

   private void initializeHeaders(Map<String, String> extraHttpHeaders) {
      StringBuilder sb = new StringBuilder();
      BiConsumer<String, String> init = (key, val) -> {
         this.headerMap.put(key, val);
         sb.append(key).append("=").append(val).append(";");
      };
      init.accept("x-client-SKU", "MSAL.Java");
      init.accept("x-client-VER", PRODUCT_VERSION_HEADER_VALUE);
      init.accept("x-client-OS", OS_HEADER_VALUE);
      init.accept("return-client-request-id", "true");
      init.accept("client-request-id", this.correlationIdHeaderValue);
      if (!StringHelper.isBlank(this.applicationNameHeaderValue)) {
         init.accept("x-app-name", this.applicationNameHeaderValue);
      }

      if (!StringHelper.isBlank(this.applicationVersionHeaderValue)) {
         init.accept("x-app-ver", this.applicationVersionHeaderValue);
      }

      if (!StringHelper.isBlank(this.anchorMailboxHeaderValue)) {
         init.accept("X-AnchorMailbox", this.anchorMailboxHeaderValue);
      }

      init.accept("x-ms-lib-capability", "retry-after, h429");
      if (extraHttpHeaders != null) {
         extraHttpHeaders.forEach(init);
      }

      this.headerValues = sb.toString();
   }

   Map<String, String> getReadonlyHeaderMap() {
      return Collections.unmodifiableMap(this.headerMap);
   }

   String getHeaderCorrelationIdValue() {
      return this.correlationIdHeaderValue;
   }

   @Override
   public String toString() {
      return this.headerValues;
   }

   private static String getProductVersion() {
      return HttpHeaders.class.getPackage().getImplementationVersion() == null ? "1.0" : HttpHeaders.class.getPackage().getImplementationVersion();
   }
}
