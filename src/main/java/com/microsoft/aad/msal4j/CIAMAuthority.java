package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

public class CIAMAuthority extends Authority {
   public static final String CIAM_HOST_SEGMENT = ".ciamlogin.com";
   static final String AUTHORIZATION_ENDPOINT = "oauth2/v2.0/authorize";
   static final String TOKEN_ENDPOINT = "oauth2/v2.0/token";
   static final String DEVICE_CODE_ENDPOINT = "oauth2/v2.0/devicecode";
   private static final String CIAM_AUTHORITY_FORMAT = "https://%s/%s/";
   private static final String DEVICE_CODE_ENDPOINT_FORMAT = "https://%s/%s/oauth2/v2.0/devicecode";
   private static final String CIAM_AUTHORIZATION_ENDPOINT_FORMAT = "https://%s/%s/oauth2/v2.0/authorize";
   private static final String CIAM_TOKEN_ENDPOINT_FORMAT = "https://%s/%s/oauth2/v2.0/token";

   CIAMAuthority(URL authorityUrl) throws MalformedURLException {
      super(transformAuthority(authorityUrl), AuthorityType.CIAM);
      this.setAuthorityProperties();
      this.authority = String.format("https://%s/%s/", this.host, this.tenant);
   }

   protected static URL transformAuthority(URL originalAuthority) throws MalformedURLException {
      String host = originalAuthority.getHost() + originalAuthority.getPath();
      String transformedAuthority = originalAuthority.toString();
      if (originalAuthority.getPath().equals("/")) {
         int ciamHostIndex = host.indexOf(".ciamlogin.com");
         String tenant = host.substring(0, ciamHostIndex);
         transformedAuthority = originalAuthority + tenant + ".onmicrosoft.com/";
      }

      return new URL(transformedAuthority);
   }

   private void setAuthorityProperties() {
      this.authorizationEndpoint = String.format("https://%s/%s/oauth2/v2.0/authorize", this.host, this.tenant);
      this.tokenEndpoint = String.format("https://%s/%s/oauth2/v2.0/token", this.host, this.tenant);
      this.deviceCodeEndpoint = String.format("https://%s/%s/oauth2/v2.0/devicecode", this.host, this.tenant);
      this.selfSignedJwtAudience = this.tokenEndpoint;
   }
}
