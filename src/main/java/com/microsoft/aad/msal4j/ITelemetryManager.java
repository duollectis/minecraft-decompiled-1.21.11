package com.microsoft.aad.msal4j;

interface ITelemetryManager {
   String generateRequestId();

   TelemetryHelper createTelemetryHelper(String var1, String var2, Event var3, Boolean var4);
}
