package com.microsoft.aad.msal4j;

import java.net.URL;

class B2CAuthority extends Authority {
   private static final String AUTHORIZATION_ENDPOINT = "/oauth2/v2.0/authorize";
   private static final String TOKEN_ENDPOINT = "/oauth2/v2.0/token";
   private static final String B2C_AUTHORIZATION_ENDPOINT_FORMAT = "https://%s/%s/%s/oauth2/v2.0/authorize";
   private static final String B2C_TOKEN_ENDPOINT_FORMAT = "https://%s/%s/oauth2/v2.0/token?p=%s";
   private String policy;

   B2CAuthority(URL authorityUrl) {
      super(authorityUrl, AuthorityType.B2C);
      this.setAuthorityProperties();
   }

   private void validatePathSegments(String[] segments) {
      if (segments.length < 2) {
         throw new IllegalArgumentException(
            "Valid B2C 'authority' URLs should follow either of these formats: https://<host>/<tenant>/<policy>/... or https://<host>/something/<tenant>/<policy>/..."
         );
      }
   }

   private void setAuthorityProperties() {
      String[] segments = this.canonicalAuthorityUrl.getPath().substring(1).split("/");
      this.validatePathSegments(segments);

      try {
         this.policy = segments[2];
         this.authority = String.format("https://%s/%s/%s/%s/", this.canonicalAuthorityUrl.getAuthority(), segments[0], segments[1], segments[2]);
      } catch (IndexOutOfBoundsException var3) {
         this.policy = segments[1];
         this.authority = String.format("https://%s/%s/%s/", this.canonicalAuthorityUrl.getAuthority(), segments[0], segments[1]);
      }

      this.authorizationEndpoint = String.format("https://%s/%s/%s/oauth2/v2.0/authorize", this.host, this.tenant, this.policy);
      this.tokenEndpoint = String.format("https://%s/%s/oauth2/v2.0/token?p=%s", this.host, this.tenant, this.policy);
      this.selfSignedJwtAudience = this.tokenEndpoint;
   }

   String policy() {
      return this.policy;
   }
}
