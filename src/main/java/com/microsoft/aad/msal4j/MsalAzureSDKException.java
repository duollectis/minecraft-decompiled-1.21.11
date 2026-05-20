package com.microsoft.aad.msal4j;

public class MsalAzureSDKException extends MsalException {
   public MsalAzureSDKException(Throwable throwable) {
      super(throwable);
   }

   public MsalAzureSDKException(String message, String errorCode) {
      super(message, errorCode);
   }
}
