package com.microsoft.aad.msal4j;

public class MsalJsonParsingException extends MsalServiceException {
   MsalJsonParsingException(String message, String error) {
      super(message, error);
   }

   MsalJsonParsingException(String message, String error, ManagedIdentitySourceType managedIdentitySource) {
      super(message, error, managedIdentitySource);
   }
}
