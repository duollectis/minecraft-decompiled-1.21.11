package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;

public interface IHttpResponse {
   int statusCode();

   Map<String, List<String>> headers();

   String body();
}
