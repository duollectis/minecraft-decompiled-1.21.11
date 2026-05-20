package com.microsoft.aad.msal4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HttpResponse implements IHttpResponse {
   private int statusCode;
   private Map<String, List<String>> headers = new HashMap<>();
   private String body;

   public void addHeaders(Map<String, List<String>> responseHeaders) {
      for (Entry<String, List<String>> entry : responseHeaders.entrySet()) {
         if (entry.getKey() != null) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty() && values.get(0) != null) {
               this.addHeader(entry.getKey(), values.toArray(new String[0]));
            }
         }
      }
   }

   void addHeader(String name, String... values) {
      if (values != null && values.length > 0) {
         this.headers.put(name, Arrays.asList(values));
      } else {
         this.headers.remove(name);
      }
   }

   List<String> getHeader(String key) {
      return this.headers.get(key);
   }

   Map<String, String> getBodyAsMap() {
      return JsonHelper.convertJsonToMap(this.body);
   }

   @Override
   public int statusCode() {
      return this.statusCode;
   }

   @Override
   public Map<String, List<String>> headers() {
      return this.headers;
   }

   @Override
   public String body() {
      return this.body;
   }

   public HttpResponse statusCode(int statusCode) {
      this.statusCode = statusCode;
      return this;
   }

   public HttpResponse body(String body) {
      this.body = body;
      return this;
   }
}
