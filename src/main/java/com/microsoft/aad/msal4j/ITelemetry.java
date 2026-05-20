package com.microsoft.aad.msal4j;

interface ITelemetry {
   void startEvent(String var1, Event var2);

   void stopEvent(String var1, Event var2);

   void flush(String var1, String var2);
}
