package com.microsoft.aad.msal4j;

import java.net.URI;

public class PopParameters {
   HttpMethod httpMethod;
   URI uri;
   String nonce;

   public HttpMethod getHttpMethod() {
      return this.httpMethod;
   }

   public URI getUri() {
      return this.uri;
   }

   public String getNonce() {
      return this.nonce;
   }

   PopParameters(HttpMethod httpMethod, URI uri, String nonce) {
      this.validatePopAuthScheme(httpMethod, uri);
      this.httpMethod = httpMethod;
      this.uri = uri;
      this.nonce = nonce;
   }

   void validatePopAuthScheme(HttpMethod httpMethod, URI uri) {
      if (httpMethod == null || uri == null || uri.getHost() == null) {
         throw new MsalClientException("HTTP method and URI host must be non-null", "brokers_package_error");
      }
   }
}
