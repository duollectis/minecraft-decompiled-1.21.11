package com.microsoft.aad.msal4j;

import java.util.Map;

class DefaultEvent extends Event {
   private static final String CLIENT_ID_KEY = "msal.client_id";
   private static final String SDK_PLATFORM_KEY = "msal.sdk_platform";
   private static final String SDK_VERSION_KEY = "msal.sdk_version";
   private static final String HTTP_EVENT_COUNT_KEY = "msal.http_event_count";
   private static final String CACHE_EVENT_COUNT_KEY = "msal.cache_event_count";
   private Map<String, Integer> eventCount;

   public DefaultEvent(String clientId, Map<String, Integer> eventCount) {
      super("msal.default_event");
      this.setClientId(clientId);
      this.setSdkPlatform();
      this.setSdkVersion();
      this.eventCount = eventCount;
      this.setHttpEventCount();
      this.setCacheEventCount();
   }

   private void setClientId(String clientId) {
      this.put("msal.client_id", clientId);
   }

   private void setSdkPlatform() {
      this.put("msal.sdk_platform", System.getProperty("os.name"));
   }

   private void setSdkVersion() {
      this.put("msal.sdk_version", this.getClass().getPackage().getImplementationVersion());
   }

   private void setHttpEventCount() {
      this.put("msal.http_event_count", this.getEventCount("msal.http_event"));
   }

   private void setCacheEventCount() {
      this.put("msal.cache_event_count", this.getEventCount("msal.cache_event"));
   }

   private String getEventCount(String eventName) {
      return this.eventCount.getOrDefault(eventName, 0).toString();
   }
}
