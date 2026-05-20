package com.microsoft.aad.msal4j;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpListener {
   private static final Logger LOG = LoggerFactory.getLogger(HttpListener.class);
   private HttpServer server;
   private int port;

   void startListener(int port, HttpHandler httpHandler) {
      try {
         this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
         this.server.createContext("/", httpHandler);
         this.port = this.server.getAddress().getPort();
         this.server.start();
         LOG.debug("Http listener started. Listening on port: " + port);
      } catch (Exception var4) {
         throw new MsalClientException(var4.getMessage(), "unable_to_start_http_listener");
      }
   }

   void stopListener() {
      if (this.server != null) {
         this.server.stop(0);
         LOG.debug("Http listener stopped");
      }
   }

   int port() {
      return this.port;
   }
}
