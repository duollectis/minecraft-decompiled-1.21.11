package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

public class OidcAuthority extends Authority {
   private static final String WELL_KNOWN_OPENID_CONFIGURATION = ".well-known/openid-configuration";
   private static final String AUTHORITY_FORMAT = "https://%s/%s/";
   private static final String CIAM_AUTHORITY_FORMAT = "https://%s.ciamlogin.com/%s";
   private String issuerFromOidcDiscovery;

   OidcAuthority(URL authorityUrl) throws MalformedURLException {
      super(createOidcDiscoveryUrl(authorityUrl), AuthorityType.OIDC);
      this.authority = String.format("https://%s/%s/", this.host, this.tenant);
   }

   private static URL createOidcDiscoveryUrl(URL originalAuthority) throws MalformedURLException {
      String authority = originalAuthority.toString();
      authority = authority + ".well-known/openid-configuration";
      return new URL(authority);
   }

   void setAuthorityProperties(OidcDiscoveryResponse instanceDiscoveryResponse) {
      this.authorizationEndpoint = instanceDiscoveryResponse.authorizationEndpoint();
      this.tokenEndpoint = instanceDiscoveryResponse.tokenEndpoint();
      this.deviceCodeEndpoint = instanceDiscoveryResponse.deviceCodeEndpoint();
      this.selfSignedJwtAudience = this.tokenEndpoint;
      this.issuerFromOidcDiscovery = instanceDiscoveryResponse.issuer();
      this.validateIssuer();
   }

   private void validateIssuer() {
      if (!this.isIssuerValid()) {
         throw new MsalClientException(
            String.format(
               "Invalid issuer from OIDC discovery. Issuer %s does not match authority %s, or is in an unexpected format",
               this.issuerFromOidcDiscovery,
               this.canonicalAuthorityUrl
            ),
            "issuer_validation"
         );
      }
   }

   private boolean isIssuerValid() {
      if (this.issuerFromOidcDiscovery == null) {
         return false;
      } else {
         String authorityWithoutWellKnown = this.canonicalAuthorityUrl.toString();
         if (authorityWithoutWellKnown.endsWith(".well-known/openid-configuration")) {
            authorityWithoutWellKnown = authorityWithoutWellKnown.substring(0, authorityWithoutWellKnown.length() - ".well-known/openid-configuration".length());
            String normalizedAuthority = Authority.enforceTrailingSlash(authorityWithoutWellKnown);
            String normalizedIssuer = Authority.enforceTrailingSlash(this.issuerFromOidcDiscovery);
            if (normalizedIssuer.equals(normalizedAuthority)) {
               return true;
            }
         }

         if (!StringHelper.isNullOrBlank(this.tenant)) {
            String ciamPattern = String.format("https://%s.ciamlogin.com/%s", this.tenant, this.tenant);
            return this.issuerFromOidcDiscovery.startsWith(ciamPattern);
         } else {
            return false;
         }
      }
   }
}
