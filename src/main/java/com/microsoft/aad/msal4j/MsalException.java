package com.microsoft.aad.msal4j;

public class MsalException extends RuntimeException {
   private static final long serialVersionUID = 1L;
   private String errorCode;

   public MsalException(Throwable throwable) {
      super(throwable);
   }

   public MsalException(String message, String errorCode) {
      super(message);
      this.errorCode = errorCode;
   }

   public String errorCode() {
      return this.errorCode;
   }
}
