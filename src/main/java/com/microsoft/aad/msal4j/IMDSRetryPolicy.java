package com.microsoft.aad.msal4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class IMDSRetryPolicy extends ManagedIdentityRetryPolicy {
   private static final int LINEAR_RETRY_NUM = 7;
   private static final int LINEAR_RETRY_DELAY_MS = 10000;
   private static final int EXPONENTIAL_RETRY_NUM = 3;
   private static final int EXPONENTIAL_RETRY_DELAY_MS = 1000;
   private static int currentLinearRetryDelayMs = 10000;
   private static int exponentialLinearRetryDelayMs = 1000;
   private int currentRetryCount;
   private int lastStatusCode;
   private static final Set<Integer> RETRYABLE_STATUS_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(404, 408, 410, 429)));

   @Override
   public boolean isRetryable(IHttpResponse httpResponse) {
      this.currentRetryCount++;
      this.lastStatusCode = httpResponse.statusCode();
      return HttpStatus.isServerError(this.lastStatusCode) || RETRYABLE_STATUS_CODES.contains(this.lastStatusCode);
   }

   @Override
   public int getMaxRetryCount(IHttpResponse httpResponse) {
      return httpResponse.statusCode() == 410 ? 7 : 3;
   }

   @Override
   public int getRetryDelayMs(IHttpResponse httpResponse) {
      return this.lastStatusCode == 410 ? currentLinearRetryDelayMs : (int)(Math.pow(2.0, this.currentRetryCount) * exponentialLinearRetryDelayMs);
   }

   static void setRetryDelayMs(int retryDelayMs) {
      currentLinearRetryDelayMs = retryDelayMs;
      exponentialLinearRetryDelayMs = retryDelayMs;
   }

   static void resetToDefaults() {
      currentLinearRetryDelayMs = 10000;
      exponentialLinearRetryDelayMs = 1000;
   }
}
