package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

public class HttpRequest {
   private HttpMethod httpMethod;
   private URL url;
   private Map<String, String> headers;
   private String body;

   HttpRequest(HttpMethod httpMethod, String url) {
      this.httpMethod = httpMethod;
      this.url = this.createUrlFromString(url);
   }

   HttpRequest(HttpMethod httpMethod, String url, Map<String, String> headers) {
      this.httpMethod = httpMethod;
      this.url = this.createUrlFromString(url);
      this.headers = headers;
   }

   HttpRequest(HttpMethod httpMethod, String url, String body) {
      this.httpMethod = httpMethod;
      this.url = this.createUrlFromString(url);
      this.body = body;
   }

   HttpRequest(HttpMethod httpMethod, String url, Map<String, String> headers, String body) {
      this.httpMethod = httpMethod;
      this.url = this.createUrlFromString(url);
      this.headers = headers;
      this.body = body;
   }

   public String headerValue(String headerName) {
      return headerName != null && this.headers != null ? this.headers.get(headerName) : null;
   }

   private URL createUrlFromString(String stringUrl) {
      try {
         return new URL(stringUrl);
      } catch (MalformedURLException var4) {
         throw new MsalClientException(var4);
      }
   }

   public HttpMethod httpMethod() {
      return this.httpMethod;
   }

   public URL url() {
      return this.url;
   }

   public Map<String, String> headers() {
      return this.headers;
   }

   public String body() {
      return this.body;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof HttpRequest)) {
         return false;
      } else {
         HttpRequest other = (HttpRequest)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (!Objects.equals(this.httpMethod(), other.httpMethod())) {
            return false;
         } else if (!Objects.equals(this.url(), other.url())) {
            return false;
         } else {
            return !Objects.equals(this.headers(), other.headers()) ? false : Objects.equals(this.body(), other.body());
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof HttpRequest;
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = result * 59 + (this.httpMethod == null ? 43 : this.httpMethod.hashCode());
      result = result * 59 + (this.url == null ? 43 : this.url.hashCode());
      result = result * 59 + (this.headers == null ? 43 : this.headers.hashCode());
      return result * 59 + (this.body == null ? 43 : this.body.hashCode());
   }
}
