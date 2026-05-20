package com.microsoft.aad.msal4j;

public class MsalThrottlingException extends MsalServiceException {
   private long retryInMs;

   public MsalThrottlingException(long retryInMs) {
      super("Request was throttled according to instructions from STS. Retry in " + retryInMs + " ms.", "throttled_request");
      this.retryInMs = retryInMs;
   }

   public long retryInMs() {
      return this.retryInMs;
   }
}
