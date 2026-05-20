package com.microsoft.aad.msal4j;

import java.net.URL;

class ADFSAuthority extends Authority {
   static final String AUTHORIZATION_ENDPOINT = "oauth2/authorize";
   static final String TOKEN_ENDPOINT = "oauth2/token";
   static final String DEVICE_CODE_ENDPOINT = "oauth2/devicecode";
   private static final String ADFS_AUTHORITY_FORMAT = "https://%s/%s/";
   private static final String DEVICE_CODE_ENDPOINT_FORMAT = "https://%s/%s/oauth2/devicecode";

   ADFSAuthority(URL authorityUrl) {
      super(authorityUrl, AuthorityType.ADFS);
      this.authority = String.format("https://%s/%s/", this.host, this.tenant);
      this.authorizationEndpoint = this.authority + "oauth2/authorize";
      this.tokenEndpoint = this.authority + "oauth2/token";
      this.selfSignedJwtAudience = this.tokenEndpoint;
      this.deviceCodeEndpoint = String.format("https://%s/%s/oauth2/devicecode", this.host, this.tenant);
   }
}
