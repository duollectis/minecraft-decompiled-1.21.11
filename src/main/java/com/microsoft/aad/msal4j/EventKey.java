package com.microsoft.aad.msal4j;

import java.util.Objects;

class EventKey {
   private String requestId;
   private String eventName;

   EventKey(String requestId, Event event) {
      this.requestId = requestId;
      this.eventName = event.get("event_name");
   }

   public String getRequestId() {
      return this.requestId;
   }

   public String getEventName() {
      return this.eventName;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      } else if (!(obj instanceof EventKey)) {
         return false;
      } else if (obj == this) {
         return true;
      } else {
         EventKey eventKey = (EventKey)obj;
         return Objects.equals(this.requestId, eventKey.getRequestId()) && Objects.equals(this.eventName, eventKey.getEventName());
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.requestId, this.eventName);
   }
}
