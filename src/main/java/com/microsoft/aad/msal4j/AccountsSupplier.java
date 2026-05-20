package com.microsoft.aad.msal4j;

import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

class AccountsSupplier implements Supplier<Set<IAccount>> {
   AbstractClientApplicationBase clientApplication;
   MsalRequest msalRequest;

   AccountsSupplier(AbstractClientApplicationBase clientApplication, MsalRequest msalRequest) {
      this.clientApplication = clientApplication;
      this.msalRequest = msalRequest;
   }

   public Set<IAccount> get() {
      try {
         return this.clientApplication.tokenCache.getAccounts(this.clientApplication.clientId());
      } catch (Exception var2) {
         this.clientApplication
            .log
            .warn(
               LogHelper.createMessage(
                  String.format("Execution of %s failed: %s", this.getClass(), var2.getMessage()), this.msalRequest.headers().getHeaderCorrelationIdValue()
               )
            );
         throw new CompletionException(var2);
      }
   }
}
