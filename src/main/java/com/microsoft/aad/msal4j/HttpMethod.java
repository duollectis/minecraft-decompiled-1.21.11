package com.microsoft.aad.msal4j;

public enum HttpMethod {
   CONNECT("CONNECT"),
   DELETE("DELETE"),
   GET("GET"),
   HEAD("HEAD"),
   OPTIONS("OPTIONS"),
   POST("POST"),
   PUT("PUT"),
   TRACE("TRACE");

   public final String methodName;

   private HttpMethod(String methodName) {
      this.methodName = methodName;
   }
}
