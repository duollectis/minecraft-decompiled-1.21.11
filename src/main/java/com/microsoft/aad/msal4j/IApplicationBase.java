package com.microsoft.aad.msal4j;

import java.net.Proxy;
import javax.net.ssl.SSLSocketFactory;

interface IApplicationBase {
   String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";

   boolean logPii();

   String correlationId();

   IHttpClient httpClient();

   Proxy proxy();

   SSLSocketFactory sslSocketFactory();
}
