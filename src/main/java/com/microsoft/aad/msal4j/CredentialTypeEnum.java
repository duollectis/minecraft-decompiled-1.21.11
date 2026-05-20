package com.microsoft.aad.msal4j;

enum CredentialTypeEnum {
   ACCESS_TOKEN("AccessToken"),
   REFRESH_TOKEN("RefreshToken"),
   ID_TOKEN("IdToken");

   private final String value;

   private CredentialTypeEnum(String value) {
      this.value = value;
   }

   String value() {
      return this.value;
   }
}
