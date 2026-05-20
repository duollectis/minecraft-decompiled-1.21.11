package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

abstract class Authority {
   private static final String ADFS_PATH_SEGMENT = "adfs";
   private static final String B2C_PATH_SEGMENT = "tfp";
   private static final String B2C_HOST_SEGMENT = "b2clogin.com";
   private static final String USER_REALM_ENDPOINT = "common/userrealm";
   private static final String userRealmEndpointFormat = "https://%s/common/userrealm/%s?api-version=1.0";
   String authority;
   final URL canonicalAuthorityUrl;
   protected final AuthorityType authorityType;
   String selfSignedJwtAudience;
   String host;
   String tenant;
   boolean isTenantless;
   String authorizationEndpoint;
   String tokenEndpoint;
   String deviceCodeEndpoint;

   URL tokenEndpointUrl() throws MalformedURLException {
      return new URL(this.tokenEndpoint);
   }

   Authority(URL canonicalAuthorityUrl, AuthorityType authorityType) {
      this.canonicalAuthorityUrl = canonicalAuthorityUrl;
      this.authorityType = authorityType;
      this.setCommonAuthorityProperties();
   }

   private void setCommonAuthorityProperties() {
      this.tenant = getTenant(this.canonicalAuthorityUrl, this.authorityType);
      this.host = this.canonicalAuthorityUrl.getAuthority().toLowerCase();
   }

   static Authority createAuthority(URL authorityUrl) throws MalformedURLException {
      AuthorityType authorityType = detectAuthorityType(authorityUrl);
      Authority createdAuthority;
      if (authorityType == AuthorityType.AAD) {
         createdAuthority = new AADAuthority(authorityUrl);
      } else if (authorityType == AuthorityType.B2C) {
         createdAuthority = new B2CAuthority(authorityUrl);
      } else if (authorityType == AuthorityType.ADFS) {
         createdAuthority = new ADFSAuthority(authorityUrl);
      } else {
         if (authorityType != AuthorityType.CIAM) {
            throw new IllegalArgumentException("Unsupported Authority Type");
         }

         createdAuthority = new CIAMAuthority(authorityUrl);
      }

      validateAuthority(createdAuthority.canonicalAuthorityUrl());
      return createdAuthority;
   }

   static AuthorityType detectAuthorityType(URL authorityUrl) {
      if (authorityUrl == null) {
         throw new NullPointerException("canonicalAuthorityUrl");
      } else {
         String path = authorityUrl.getPath().substring(1);
         if (StringHelper.isBlank(path)) {
            if (isCiamAuthority(authorityUrl.getHost())) {
               return AuthorityType.CIAM;
            } else {
               throw new IllegalArgumentException("authority Uri should have at least one segment in the path (i.e. https://<host>/<path>/...)");
            }
         } else {
            String host = authorityUrl.getHost();
            String firstPath = path.substring(0, path.indexOf("/"));
            if (isB2CAuthority(host, firstPath)) {
               return AuthorityType.B2C;
            } else if (isAdfsAuthority(firstPath)) {
               return AuthorityType.ADFS;
            } else {
               return isCiamAuthority(host) ? AuthorityType.CIAM : AuthorityType.AAD;
            }
         }
      }
   }

   static void validateAuthority(URL authorityUrl) {
      if (!authorityUrl.getProtocol().equalsIgnoreCase("https")) {
         throw new IllegalArgumentException("authority should use the 'https' scheme");
      } else if (authorityUrl.toString().contains("#")) {
         throw new IllegalArgumentException("authority is invalid format (contains fragment)");
      } else if (!StringHelper.isBlank(authorityUrl.getQuery())) {
         throw new IllegalArgumentException("authority cannot contain query parameters");
      } else {
         String path = authorityUrl.getPath();
         if (path.length() == 0) {
            throw new IllegalArgumentException("Authority Uri should have at least one segment in the path");
         } else {
            String[] segments = path.substring(1).split("/");
            if (segments.length == 0) {
               throw new IllegalArgumentException("Authority Uri must have at least one path segment. This is usually 'common' or the application's tenant id.");
            } else {
               for (String segment : segments) {
                  if (StringHelper.isBlank(segment)) {
                     throw new IllegalArgumentException("Authority Uri should not have empty path segments");
                  }
               }
            }
         }
      }
   }

   static Authority replaceTenant(Authority originalAuthority, String newTenant) throws MalformedURLException {
      String authorityString = originalAuthority.canonicalAuthorityUrl().toString();
      authorityString = authorityString.replace(originalAuthority.tenant, newTenant);
      return createAuthority(new URL(authorityString));
   }

   static String getTenant(URL authorityUrl, AuthorityType authorityType) {
      String[] segments = authorityUrl.getPath().substring(1).split("/");
      if (authorityType == AuthorityType.B2C) {
         return segments.length < 3 ? segments[0] : segments[1];
      } else {
         return segments[0];
      }
   }

   String getUserRealmEndpoint(String username) {
      return String.format("https://%s/common/userrealm/%s?api-version=1.0", this.host, username);
   }

   private static boolean isAdfsAuthority(String firstPath) {
      return firstPath.compareToIgnoreCase("adfs") == 0;
   }

   private static boolean isB2CAuthority(String host, String firstPath) {
      return host.contains("b2clogin.com") || firstPath.compareToIgnoreCase("tfp") == 0;
   }

   private static boolean isCiamAuthority(String host) {
      return host.endsWith(".ciamlogin.com");
   }

   String deviceCodeEndpoint() {
      return this.deviceCodeEndpoint;
   }

   protected static String enforceTrailingSlash(String authority) {
      authority = authority.toLowerCase();
      if (!authority.endsWith("/")) {
         authority = authority + "/";
      }

      return authority;
   }

   String authority() {
      return this.authority;
   }

   URL canonicalAuthorityUrl() {
      return this.canonicalAuthorityUrl;
   }

   AuthorityType authorityType() {
      return this.authorityType;
   }

   String selfSignedJwtAudience() {
      return this.selfSignedJwtAudience;
   }

   String host() {
      return this.host;
   }

   String tenant() {
      return this.tenant;
   }

   boolean isTenantless() {
      return this.isTenantless;
   }

   String authorizationEndpoint() {
      return this.authorizationEndpoint;
   }

   String tokenEndpoint() {
      return this.tokenEndpoint;
   }
}
