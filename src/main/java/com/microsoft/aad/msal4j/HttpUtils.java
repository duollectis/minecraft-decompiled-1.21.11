package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;

class HttpUtils {
   static String headerValue(Map<String, List<String>> headers, String headerName) {
      if (headerName != null && headers != null) {
         List<String> headerValue = headers.get(headerName);
         return headerValue != null && !headerValue.isEmpty() ? String.join(",", headerValue) : null;
      } else {
         return null;
      }
   }
}
