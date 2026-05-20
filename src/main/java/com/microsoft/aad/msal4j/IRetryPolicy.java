package com.microsoft.aad.msal4j;

interface IRetryPolicy {
   boolean isRetryable(IHttpResponse var1);

   int getMaxRetryCount(IHttpResponse var1);

   int getRetryDelayMs(IHttpResponse var1);
}
