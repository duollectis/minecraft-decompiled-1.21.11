package com.microsoft.aad.msal4j;

public class MsalClientException extends MsalException {
   public MsalClientException(Throwable throwable) {
      super(throwable);
   }

   public MsalClientException(String message, String errorCode) {
      super(message, errorCode);
   }
}
