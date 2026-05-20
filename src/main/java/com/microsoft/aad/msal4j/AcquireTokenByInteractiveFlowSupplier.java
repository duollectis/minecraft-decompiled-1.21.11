package com.microsoft.aad.msal4j;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AcquireTokenByInteractiveFlowSupplier extends AuthenticationResultSupplier {
   private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenByInteractiveFlowSupplier.class);
   private PublicClientApplication clientApplication;
   private InteractiveRequest interactiveRequest;
   private BlockingQueue<AuthorizationResult> authorizationResultQueue;
   private HttpListener httpListener;
   public static final String LINUX_XDG_OPEN = "linux_xdg_open_failed";
   public static final String LINUX_OPEN_AS_SUDO_NOT_SUPPORTED = "Unable to open a web page using xdg-open, gnome-open, kfmclient or wslview tools in sudo mode. Please run the process as non-sudo user.";

   AcquireTokenByInteractiveFlowSupplier(PublicClientApplication clientApplication, InteractiveRequest request) {
      super(clientApplication, request);
      this.clientApplication = clientApplication;
      this.interactiveRequest = request;
   }

   @Override
   AuthenticationResult execute() throws Exception {
      AuthorizationResult authorizationResult = this.getAuthorizationResult();
      this.validateState(authorizationResult);
      return this.acquireTokenWithAuthorizationCode(authorizationResult);
   }

   private AuthorizationResult getAuthorizationResult() {
      AuthorizationResult result;
      try {
         SystemBrowserOptions systemBrowserOptions = this.interactiveRequest.interactiveRequestParameters().systemBrowserOptions();
         this.authorizationResultQueue = new LinkedBlockingQueue<>();
         AuthorizationResponseHandler authorizationResponseHandler = new AuthorizationResponseHandler(this.authorizationResultQueue, systemBrowserOptions);
         this.startHttpListener(authorizationResponseHandler);
         if (systemBrowserOptions != null && systemBrowserOptions.openBrowserAction() != null) {
            this.interactiveRequest
               .interactiveRequestParameters()
               .systemBrowserOptions()
               .openBrowserAction()
               .openBrowser(this.interactiveRequest.authorizationUrl());
         } else {
            this.openDefaultSystemBrowser(this.interactiveRequest.authorizationUrl());
         }

         result = this.getAuthorizationResultFromHttpListener();
      } finally {
         if (this.httpListener != null) {
            this.httpListener.stopListener();
         }
      }

      return result;
   }

   private void validateState(AuthorizationResult authorizationResult) {
      if (StringHelper.isBlank(authorizationResult.state()) || !authorizationResult.state().equals(this.interactiveRequest.state())) {
         throw new MsalClientException(
            "State returned in authorization result is blank or does not match state sent on outgoing request", "invalid_authorization_result"
         );
      }
   }

   private void startHttpListener(AuthorizationResponseHandler handler) {
      int port = this.interactiveRequest.interactiveRequestParameters().redirectUri().getPort() == -1
         ? 0
         : this.interactiveRequest.interactiveRequestParameters().redirectUri().getPort();
      this.httpListener = new HttpListener();
      this.httpListener.startListener(port, handler);
      if (port != this.httpListener.port()) {
         this.updateRedirectUrl();
      }
   }

   private void updateRedirectUrl() {
      try {
         URI updatedRedirectUrl = new URI("http://localhost:" + this.httpListener.port());
         this.interactiveRequest.interactiveRequestParameters().redirectUri(updatedRedirectUrl);
         LOG.debug("Redirect URI updated to" + updatedRedirectUrl);
      } catch (URISyntaxException var2) {
         throw new MsalClientException("Error updating redirect URI. Not a valid URI format", "invalid_redirect_uri");
      }
   }

   private static List<String> getOpenToolsLinux() {
      return Arrays.asList("xdg-open", "gnome-open", "kfmclient", "microsoft-edge", "wslview");
   }

   private static String getExecutablePath(String executable) {
      String pathEnvVar = System.getenv("PATH");
      if (pathEnvVar != null) {
         String[] paths = pathEnvVar.split(File.pathSeparator);

         for (String basePath : paths) {
            String path = basePath + File.separator + executable;
            if (new File(path).exists()) {
               return path;
            }
         }
      }

      return null;
   }

   private void openDefaultSystemBrowser(URL url) {
      if (OSHelper.isWindows()) {
         openDefaultSystemBrowserInWindows(url);
      } else if (OSHelper.isMac()) {
         openDefaultSystemBrowserInMac(url);
      } else {
         if (!OSHelper.isLinux()) {
            throw new UnsupportedOperationException(OSHelper.getOs() + "Operating system not supported exception.");
         }

         openDefaultSystemBrowserInLinux(url);
      }
   }

   private static void openDefaultSystemBrowserInWindows(URL url) {
      try {
         if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
            Desktop.getDesktop().browse(url.toURI());
            LOG.debug("Opened default system browser");
         } else {
            throw new MsalClientException("Unable to open default system browser", "desktop_browser_not_supported");
         }
      } catch (IOException | URISyntaxException var2) {
         throw new MsalClientException(var2);
      }
   }

   private static void openDefaultSystemBrowserInMac(URL url) {
      Runtime runtime = Runtime.getRuntime();

      try {
         runtime.exec("open " + url);
      } catch (IOException var3) {
         throw new RuntimeException(var3);
      }
   }

   private static void openDefaultSystemBrowserInLinux(URL url) {
      String sudoUser = System.getenv("SUDO_USER");
      if (sudoUser != null && !sudoUser.isEmpty()) {
         throw new MsalClientException(
            "linux_xdg_open_failed",
            "Unable to open a web page using xdg-open, gnome-open, kfmclient or wslview tools in sudo mode. Please run the process as non-sudo user."
         );
      } else {
         boolean opened = false;

         for (String openTool : getOpenToolsLinux()) {
            String openToolPath = getExecutablePath(openTool);
            if (openToolPath != null) {
               Runtime runtime = Runtime.getRuntime();

               try {
                  runtime.exec(openTool + " " + url);
               } catch (IOException var9) {
                  throw new RuntimeException(var9);
               }

               opened = true;
               break;
            }
         }

         if (!opened) {
            throw new MsalClientException(
               "linux_xdg_open_failed",
               "Unable to open a web page using xdg-open, gnome-open, kfmclient or wslview tools in sudo mode. Please run the process as non-sudo user."
            );
         }
      }
   }

   private AuthorizationResult getAuthorizationResultFromHttpListener() {
      AuthorizationResult result = null;

      try {
         int timeFromParameters = this.interactiveRequest.interactiveRequestParameters().httpPollingTimeoutInSeconds();
         long expirationTime;
         if (timeFromParameters > 0) {
            LOG.debug(String.format("Listening for authorization result. Listener will timeout after %S seconds.", timeFromParameters));
            expirationTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + timeFromParameters;
         } else {
            LOG.warn("Listening for authorization result. Timeout configured to less than 1 second, listener will use a 1 second timeout instead.");
            expirationTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + 1L;
         }

         while (result == null && !this.interactiveRequest.futureReference().get().isDone()) {
            if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) > expirationTime) {
               LOG.warn(
                  String.format("Listener timed out after %S seconds, no authorization code was returned from the server during that time.", timeFromParameters)
               );
               break;
            }

            result = this.authorizationResultQueue.poll(100L, TimeUnit.MILLISECONDS);
         }
      } catch (Exception var5) {
         throw new MsalClientException(var5);
      }

      if (result != null && !StringHelper.isBlank(result.code())) {
         return result;
      } else {
         throw new MsalClientException("No Authorization code was returned from the server", "invalid_authorization_result");
      }
   }

   private AuthenticationResult acquireTokenWithAuthorizationCode(AuthorizationResult authorizationResult) throws Exception {
      AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
            authorizationResult.code(), this.interactiveRequest.interactiveRequestParameters().redirectUri()
         )
         .scopes(this.interactiveRequest.interactiveRequestParameters().scopes())
         .codeVerifier(this.interactiveRequest.verifier())
         .claims(this.interactiveRequest.interactiveRequestParameters().claims())
         .build();
      RequestContext context = new RequestContext(
         this.clientApplication, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters, this.interactiveRequest.requestContext().userIdentifier()
      );
      AuthorizationCodeRequest authCodeRequest = new AuthorizationCodeRequest(parameters, this.clientApplication, context);
      Authority authority;
      if (authorizationResult.environment() != null) {
         authority = Authority.createAuthority(
            new URL(
               this.clientApplication.authenticationAuthority.canonicalAuthorityUrl.getProtocol(),
               authorizationResult.environment(),
               this.clientApplication.authenticationAuthority.canonicalAuthorityUrl.getFile()
            )
         );
      } else {
         authority = this.clientApplication.authenticationAuthority;
      }

      AcquireTokenByAuthorizationGrantSupplier acquireTokenByAuthorizationGrantSupplier = new AcquireTokenByAuthorizationGrantSupplier(
         this.clientApplication, authCodeRequest, authority
      );
      return acquireTokenByAuthorizationGrantSupplier.execute();
   }
}
