package com.microsoft.aad.msal4j;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServerSideTelemetry {
   private static final Logger log = LoggerFactory.getLogger(ServerSideTelemetry.class);
   private static final String SCHEMA_VERSION = "5";
   private static final String SCHEMA_PIPE_DELIMITER = "|";
   private static final String SCHEMA_COMMA_DELIMITER = ",";
   private static final String CURRENT_REQUEST_HEADER_NAME = "x-client-current-telemetry";
   private static final String LAST_REQUEST_HEADER_NAME = "x-client-last-telemetry";
   private static final int CURRENT_REQUEST_MAX_SIZE = 100;
   private static final int LAST_REQUEST_MAX_SIZE = 350;
   private CurrentRequest currentRequest;
   private AtomicInteger silentSuccessfulCount = new AtomicInteger(0);
   ConcurrentMap<String, String[]> previousRequests = new ConcurrentHashMap<>();
   ConcurrentMap<String, String[]> previousRequestInProgress = new ConcurrentHashMap<>();

   synchronized Map<String, String> getServerTelemetryHeaderMap() {
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put("x-client-current-telemetry", this.buildCurrentRequestHeader());
      headerMap.put("x-client-last-telemetry", this.buildLastRequestHeader());
      return headerMap;
   }

   void addFailedRequestTelemetry(String publicApiId, String correlationId, String error) {
      String[] previousRequest = new String[]{publicApiId, error};
      this.previousRequests.put(correlationId, previousRequest);
   }

   void incrementSilentSuccessfulCount() {
      this.silentSuccessfulCount.incrementAndGet();
   }

   synchronized CurrentRequest getCurrentRequest() {
      return this.currentRequest;
   }

   synchronized void setCurrentRequest(CurrentRequest currentRequest) {
      this.currentRequest = currentRequest;
   }

   private synchronized String buildCurrentRequestHeader() {
      if (this.currentRequest == null) {
         return StringHelper.EMPTY_STRING;
      } else {
         String currentRequestHeader = "5|"
            + this.currentRequest.publicApi().getApiId()
            + ","
            + this.currentRequest.cacheInfo().telemetryValue
            + ","
            + this.currentRequest.regionUsed()
            + ","
            + this.currentRequest.regionSource()
            + ","
            + this.currentRequest.regionOutcome()
            + "|";
         if (currentRequestHeader.getBytes(StandardCharsets.UTF_8).length > 100) {
            log.warn("Current request telemetry header greater than 100 bytes");
         }

         return currentRequestHeader;
      }
   }

   private synchronized String buildLastRequestHeader() {
      StringBuilder lastRequestBuilder = new StringBuilder();
      lastRequestBuilder.append("5").append("|").append(this.silentSuccessfulCount.getAndSet(0));
      int baseLength = lastRequestBuilder.toString().getBytes(StandardCharsets.UTF_8).length;
      if (this.previousRequests.isEmpty()) {
         return lastRequestBuilder.append("|").append("|").append("|").toString();
      } else {
         StringBuilder middleSegmentBuilder = new StringBuilder("|");
         StringBuilder errorSegmentBuilder = new StringBuilder("|");
         Iterator<String> it = this.previousRequests.keySet().iterator();
         String lastRequest = lastRequestBuilder.toString() + "|" + "|";

         while (it.hasNext()) {
            String correlationId = it.next();
            String[] previousRequest = this.previousRequests.get(correlationId);
            String apiId = (String)Array.get(previousRequest, 0);
            String error = (String)Array.get(previousRequest, 1);
            middleSegmentBuilder.append(apiId).append(",").append(correlationId);
            errorSegmentBuilder.append(error);
            int lastRequestLength = baseLength
               + middleSegmentBuilder.toString().getBytes(StandardCharsets.UTF_8).length
               + errorSegmentBuilder.toString().getBytes(StandardCharsets.UTF_8).length;
            if (lastRequestLength >= 349) {
               break;
            }

            lastRequest = lastRequestBuilder.toString() + middleSegmentBuilder.toString() + errorSegmentBuilder.toString();
            this.previousRequestInProgress.put(correlationId, previousRequest);
            it.remove();
            if (it.hasNext()) {
               middleSegmentBuilder.append(",");
               errorSegmentBuilder.append(",");
            }
         }

         return lastRequest + "|";
      }
   }
}
