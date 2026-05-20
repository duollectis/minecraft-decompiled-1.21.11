package com.microsoft.aad.msal4j;

public enum InteractionRequiredExceptionReason {
   NONE("none"),
   MESSAGE_ONLY("message_only"),
   BASIC_ACTION("basic_action"),
   ADDITIONAL_ACTION("additional_action"),
   CONSENT_REQUIRED("consent_required"),
   USER_PASSWORD_EXPIRED("user_password_expired");

   private String error;

   private InteractionRequiredExceptionReason(String error) {
      this.error = error;
   }

   static InteractionRequiredExceptionReason fromSubErrorString(String subError) {
      if (StringHelper.isBlank(subError)) {
         return NONE;
      } else {
         for (InteractionRequiredExceptionReason reason : values()) {
            if (reason.error.equalsIgnoreCase(subError)) {
               return reason;
            }
         }

         return NONE;
      }
   }
}
