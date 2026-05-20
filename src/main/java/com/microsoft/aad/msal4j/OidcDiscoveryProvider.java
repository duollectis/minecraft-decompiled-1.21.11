package com.microsoft.aad.msal4j;

class OidcDiscoveryProvider {
   static OidcDiscoveryResponse performOidcDiscovery(OidcAuthority authority, AbstractClientApplicationBase clientApplication) {
      HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, authority.canonicalAuthorityUrl.toString());
      IHttpResponse httpResponse = ((HttpHelper)clientApplication.serviceBundle.getHttpHelper()).executeHttpRequest(httpRequest);
      OidcDiscoveryResponse response = JsonHelper.convertJsonStringToJsonSerializableObject(httpResponse.body(), OidcDiscoveryResponse::fromJson);
      if (httpResponse.statusCode() != 200) {
         throw MsalServiceExceptionFactory.fromHttpResponse(httpResponse);
      } else {
         return response;
      }
   }
}
