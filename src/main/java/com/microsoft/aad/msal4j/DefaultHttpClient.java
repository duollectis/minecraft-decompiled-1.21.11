package com.microsoft.aad.msal4j;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Map.Entry;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultHttpClient implements IHttpClient {
   private static final Logger LOG = LoggerFactory.getLogger(DefaultHttpClient.class);
   final Proxy proxy;
   final SSLSocketFactory sslSocketFactory;
   int connectTimeout = 0;
   int readTimeout = 0;

   DefaultHttpClient(Proxy proxy, SSLSocketFactory sslSocketFactory, Integer connectTimeout, Integer readTimeout) {
      this.proxy = proxy;
      this.sslSocketFactory = sslSocketFactory;
      if (connectTimeout != null) {
         this.connectTimeout = connectTimeout;
      }

      if (readTimeout != null) {
         this.readTimeout = readTimeout;
      }
   }

   @Override
   public IHttpResponse send(HttpRequest httpRequest) throws Exception {
      HttpResponse response = null;
      if (httpRequest.httpMethod() == HttpMethod.GET) {
         response = this.executeHttpGet(httpRequest);
      } else if (httpRequest.httpMethod() == HttpMethod.POST) {
         response = this.executeHttpPost(httpRequest);
      }

      return response;
   }

   private HttpResponse executeHttpGet(HttpRequest httpRequest) throws Exception {
      HttpURLConnection conn = this.openConnection(httpRequest.url());
      this.configureAdditionalHeaders(conn, httpRequest);
      return this.readResponseFromConnection(conn);
   }

   private HttpResponse executeHttpPost(HttpRequest httpRequest) throws Exception {
      HttpURLConnection conn = this.openConnection(httpRequest.url());
      this.configureAdditionalHeaders(conn, httpRequest);
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);
      DataOutputStream wr = null;

      HttpResponse var4;
      try {
         wr = new DataOutputStream(conn.getOutputStream());
         wr.writeBytes(httpRequest.body());
         wr.flush();
         var4 = this.readResponseFromConnection(conn);
      } finally {
         if (wr != null) {
            wr.close();
         }
      }

      return var4;
   }

   HttpURLConnection openConnection(URL finalURL) throws IOException {
      URLConnection connection;
      if (this.proxy != null) {
         connection = finalURL.openConnection(this.proxy);
      } else {
         connection = finalURL.openConnection();
      }

      connection.setConnectTimeout(this.connectTimeout);
      connection.setReadTimeout(this.readTimeout);
      if (connection instanceof HttpsURLConnection) {
         HttpsURLConnection httpsConnection = (HttpsURLConnection)connection;
         if (this.sslSocketFactory != null) {
            httpsConnection.setSSLSocketFactory(this.sslSocketFactory);
         }

         return httpsConnection;
      } else {
         return (HttpURLConnection)connection;
      }
   }

   private void configureAdditionalHeaders(HttpURLConnection conn, HttpRequest httpRequest) {
      if (httpRequest.headers() != null) {
         for (Entry<String, String> entry : httpRequest.headers().entrySet()) {
            if (entry.getValue() != null) {
               conn.addRequestProperty(entry.getKey(), entry.getValue());
            }
         }
      }
   }

   private HttpResponse readResponseFromConnection(HttpURLConnection conn) throws IOException {
      InputStream is = null;

      HttpResponse var5;
      try {
         HttpResponse httpResponse = new HttpResponse();
         int responseCode = conn.getResponseCode();
         httpResponse.statusCode(responseCode);
         if (responseCode == 200) {
            is = conn.getInputStream();
            httpResponse.addHeaders(conn.getHeaderFields());
            httpResponse.body(this.inputStreamToString(is));
            return httpResponse;
         }

         is = conn.getErrorStream();
         if (is != null) {
            httpResponse.addHeaders(conn.getHeaderFields());
            httpResponse.body(this.inputStreamToString(is));
         }

         var5 = httpResponse;
      } catch (SocketTimeoutException var10) {
         LOG.error(
            "Timeout while waiting for response from service. If custom timeouts were set, increasing them may resolve this issue. See https://aka.ms/msal4j-http-client for more information and solutions."
         );
         throw var10;
      } catch (ConnectException var11) {
         LOG.error(
            "Exception while connecting to service, there may be network issues preventing MSAL Java from connecting. See https://aka.ms/msal4j-http-client for more information and solutions."
         );
         throw var11;
      } finally {
         if (is != null) {
            is.close();
         }
      }

      return var5;
   }

   private String inputStreamToString(InputStream is) {
      Scanner s = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
      return s.hasNext() ? s.next() : "";
   }
}
