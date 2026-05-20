package com.microsoft.aad.msal4j;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AuthorizationResponseHandler implements HttpHandler {
   private static final Logger LOG = LoggerFactory.getLogger(AuthorizationResponseHandler.class);
   private static final String DEFAULT_SUCCESS_MESSAGE = "<html><head><title>Authentication Complete</title></head>  <body> Authentication complete. You can close the browser and return to the application.  </body></html>";
   private static final String DEFAULT_FAILURE_MESSAGE = "<html><head><title>Authentication Failed</title></head> <body> Authentication failed. You can return to the application. Feel free to close this browser tab. </br></br></br></br> Error details: error {0} error_description: {1} </body> </html>";
   private BlockingQueue<AuthorizationResult> authorizationResultQueue;
   private SystemBrowserOptions systemBrowserOptions;

   AuthorizationResponseHandler(BlockingQueue<AuthorizationResult> authorizationResultQueue, SystemBrowserOptions systemBrowserOptions) {
      this.authorizationResultQueue = authorizationResultQueue;
      this.systemBrowserOptions = systemBrowserOptions;
   }

   @Override
   public void handle(HttpExchange httpExchange) throws IOException {
      try {
         if (httpExchange.getRequestURI().getPath().equalsIgnoreCase("/")) {
            String responseBody = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody())).lines().collect(Collectors.joining("\n"));
            AuthorizationResult result = AuthorizationResult.fromResponseBody(responseBody);
            this.sendResponse(httpExchange, result);
            this.authorizationResultQueue.put(result);
            return;
         }

         httpExchange.sendResponseHeaders(200, 0L);
      } catch (InterruptedException var7) {
         LOG.error("Error reading response from socket: " + var7.getMessage());
         throw new MsalClientException(var7);
      } finally {
         httpExchange.close();
      }
   }

   private void sendResponse(HttpExchange httpExchange, AuthorizationResult result) throws IOException {
      switch (result.status()) {
         case Success:
            this.sendSuccessResponse(httpExchange, this.getSuccessfulResponseMessage());
            break;
         case ProtocolError:
         case UnknownError:
            this.sendErrorResponse(httpExchange, this.getErrorResponseMessage());
      }
   }

   private void sendSuccessResponse(HttpExchange httpExchange, String response) throws IOException {
      if (this.systemBrowserOptions != null && this.systemBrowserOptions.browserRedirectSuccess() != null) {
         this.send302Response(httpExchange, this.systemBrowserOptions().browserRedirectSuccess().toString());
      } else {
         this.send200Response(httpExchange, response);
      }
   }

   private void sendErrorResponse(HttpExchange httpExchange, String response) throws IOException {
      if (this.systemBrowserOptions != null && this.systemBrowserOptions.browserRedirectError() != null) {
         this.send302Response(httpExchange, this.systemBrowserOptions().browserRedirectError().toString());
      } else {
         this.send200Response(httpExchange, response);
      }
   }

   private void send302Response(HttpExchange httpExchange, String redirectUri) throws IOException {
      Headers responseHeaders = httpExchange.getResponseHeaders();
      responseHeaders.set("Location", redirectUri);
      httpExchange.sendResponseHeaders(302, 0L);
   }

   private void send200Response(HttpExchange httpExchange, String response) throws IOException {
      byte[] responseBytes = response.getBytes("UTF-8");
      httpExchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
      httpExchange.sendResponseHeaders(200, responseBytes.length);
      OutputStream os = httpExchange.getResponseBody();
      os.write(responseBytes);
      os.close();
   }

   private String getSuccessfulResponseMessage() {
      return this.systemBrowserOptions != null && this.systemBrowserOptions.htmlMessageSuccess() != null
         ? this.systemBrowserOptions().htmlMessageSuccess()
         : "<html><head><title>Authentication Complete</title></head>  <body> Authentication complete. You can close the browser and return to the application.  </body></html>";
   }

   private String getErrorResponseMessage() {
      return this.systemBrowserOptions != null && this.systemBrowserOptions.htmlMessageError() != null
         ? this.systemBrowserOptions().htmlMessageError()
         : "<html><head><title>Authentication Failed</title></head> <body> Authentication failed. You can return to the application. Feel free to close this browser tab. </br></br></br></br> Error details: error {0} error_description: {1} </body> </html>";
   }

   BlockingQueue<AuthorizationResult> authorizationResultQueue() {
      return this.authorizationResultQueue;
   }

   SystemBrowserOptions systemBrowserOptions() {
      return this.systemBrowserOptions;
   }
}
