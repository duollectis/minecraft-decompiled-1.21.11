package com.microsoft.aad.msal4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class MsalServiceExceptionFactory {
   private MsalServiceExceptionFactory() {
   }

   static MsalServiceException fromHttpResponse(IHttpResponse response) {
      String responseBody = response.body();
      if (StringHelper.isBlank(responseBody)) {
         return new MsalServiceException(
            String.format("Unknown service exception. Http request returned status code %s with no response body", response.statusCode()), "unknown"
         );
      } else {
         ErrorResponse errorResponse = JsonHelper.convertJsonStringToJsonSerializableObject(responseBody, ErrorResponse::fromJson);
         if (errorResponse.error() != null && errorResponse.error().equalsIgnoreCase("invalid_grant") && isInteractionRequired(errorResponse.subError)) {
            return new MsalInteractionRequiredException(errorResponse, response.headers());
         } else if (!StringHelper.isBlank(errorResponse.error()) && !StringHelper.isBlank(errorResponse.errorDescription)) {
            errorResponse.statusCode(response.statusCode());
            return new MsalServiceException(errorResponse, response.headers());
         } else {
            return new MsalServiceException(
               String.format("Unknown service exception. Http request returned status code: %s with http body: %s", response.statusCode(), responseBody),
               "unknown"
            );
         }
      }
   }

   private static boolean isInteractionRequired(String subError) {
      String[] nonUiSubErrors = new String[]{"client_mismatch", "protection_policy_required"};
      Set<String> set = new HashSet<>(Arrays.asList(nonUiSubErrors));
      return StringHelper.isBlank(subError) ? true : !set.contains(subError);
   }
}
