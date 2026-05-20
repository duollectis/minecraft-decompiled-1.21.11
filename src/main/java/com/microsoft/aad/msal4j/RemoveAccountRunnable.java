package com.microsoft.aad.msal4j;

import java.util.concurrent.CompletionException;

class RemoveAccountRunnable implements Runnable {
   private RequestContext requestContext;
   private AbstractApplicationBase clientApplication;
   IAccount account;

   RemoveAccountRunnable(MsalRequest msalRequest, IAccount account) {
      this.clientApplication = msalRequest.application();
      this.requestContext = msalRequest.requestContext();
      this.account = account;
   }

   @Override
   public void run() {
      try {
         this.clientApplication.tokenCache.removeAccount(this.clientApplication.clientId(), this.account);
      } catch (Exception var2) {
         this.clientApplication
            .log
            .warn(LogHelper.createMessage(String.format("Execution of %s failed: %s", this.getClass(), var2.getMessage()), this.requestContext.correlationId()));
         throw new CompletionException(var2);
      }
   }
}
