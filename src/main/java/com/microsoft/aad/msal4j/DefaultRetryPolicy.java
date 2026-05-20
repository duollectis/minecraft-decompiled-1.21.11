package com.microsoft.aad.msal4j;

class DefaultRetryPolicy implements IRetryPolicy {
   private static final int RETRY_NUM = 1;
   private static final int RETRY_DELAY_MS = 1000;

   @Override
   public boolean isRetryable(IHttpResponse httpResponse) {
      return HttpStatus.isServerError(httpResponse.statusCode()) && HttpHelper.getRetryAfterHeader(httpResponse) == null;
   }

   @Override
   public int getMaxRetryCount(IHttpResponse httpResponse) {
      return 1;
   }

   @Override
   public int getRetryDelayMs(IHttpResponse httpResponse) {
      return 1000;
   }
}
