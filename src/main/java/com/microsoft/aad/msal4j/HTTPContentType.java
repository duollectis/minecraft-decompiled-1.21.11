package com.microsoft.aad.msal4j;

enum HTTPContentType {
   ApplicationURLEncoded("application/x-www-form-urlencoded; charset=UTF-8"),
   ApplicationJSON("application/json; charset=UTF-8");

   public final String contentType;

   private HTTPContentType(String contentType) {
      this.contentType = contentType;
   }
}
