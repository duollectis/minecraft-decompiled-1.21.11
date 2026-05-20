package com.microsoft.aad.msal4j;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

abstract class Event extends HashMap<String, String> {
   static final String EVENT_NAME_KEY = "event_name";
   static final String START_TIME_KEY = "start_time";
   static final String ELAPSED_TIME_KEY = "elapsed_time";
   private static final String TENANT_PLACEHOLDER = "<tenant>";
   private static final String USERNAME_PLACEHOLDER = "<user>";
   private long startTimeStamp;

   Event(String eventName) {
      this(eventName, new HashMap<>());
   }

   Event(String eventName, Map<String, String> predefined) {
      super(predefined);
      this.put("event_name", eventName);
      this.startTimeStamp = Instant.now().toEpochMilli();
      this.put("start_time", Long.toString(this.startTimeStamp));
      this.put("elapsed_time", "-1");
   }

   void stop() {
      long duration = Instant.now().toEpochMilli() - this.startTimeStamp;
      this.put("elapsed_time", Long.toString(duration));
   }

   static String scrubTenant(URI uri) {
      if (!uri.isAbsolute()) {
         throw new IllegalArgumentException("Requires an absolute URI");
      } else if (!AadInstanceDiscoveryProvider.TRUSTED_HOSTS_SET.contains(uri.getHost())) {
         return null;
      } else {
         String[] segment = uri.getPath().split("/");
         if (segment.length >= 2) {
            if (segment[1].equals("tfp") && segment.length >= 3) {
               segment[2] = "<tenant>";
            } else {
               segment[1] = "<tenant>";
            }

            if (segment.length >= 4 && segment[2].equals("userrealm")) {
               segment[3] = "<user>";
            }
         }

         String scrubbedPath = String.join("/", segment);
         return uri.getScheme() + "://" + uri.getAuthority() + scrubbedPath;
      }
   }
}
