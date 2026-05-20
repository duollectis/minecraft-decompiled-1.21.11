package com.microsoft.aad.msal4j;

class HttpStatus {
   static final int HTTP_OK = 200;
   static final int HTTP_FOUND = 302;
   static final int HTTP_BAD_REQUEST = 400;
   static final int HTTP_UNAUTHORIZED = 401;
   static final int HTTP_NOT_FOUND = 404;
   static final int HTTP_REQUEST_TIMEOUT = 408;
   static final int HTTP_GONE = 410;
   static final int HTTP_TOO_MANY_REQUESTS = 429;
   static final int HTTP_INTERNAL_ERROR = 500;
   static final int HTTP_UNAVAILABLE = 503;
   static final int HTTP_GATEWAY_TIMEOUT = 504;

   static boolean isServerError(int code) {
      return code >= 500 && code < 600;
   }
}
