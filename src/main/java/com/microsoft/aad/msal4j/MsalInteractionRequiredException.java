package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;

public class MsalInteractionRequiredException extends MsalServiceException {
   private final InteractionRequiredExceptionReason reason;

   public MsalInteractionRequiredException(ErrorResponse errorResponse, Map<String, List<String>> headerMap) {
      super(errorResponse, headerMap);
      this.reason = InteractionRequiredExceptionReason.fromSubErrorString(errorResponse.subError);
   }

   public InteractionRequiredExceptionReason reason() {
      return this.reason;
   }
}
