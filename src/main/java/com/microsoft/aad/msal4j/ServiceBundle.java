package com.microsoft.aad.msal4j;

import java.util.concurrent.ExecutorService;

class ServiceBundle {
   private ExecutorService executorService;
   private TelemetryManager telemetryManager;
   private IHttpHelper httpHelper;
   private ServerSideTelemetry serverSideTelemetry;

   ServiceBundle(ExecutorService executorService, TelemetryManager telemetryManager, IHttpHelper httpHelper) {
      this.executorService = executorService;
      this.telemetryManager = telemetryManager;
      this.httpHelper = httpHelper;
      this.serverSideTelemetry = new ServerSideTelemetry();
   }

   ExecutorService getExecutorService() {
      return this.executorService;
   }

   TelemetryManager getTelemetryManager() {
      return this.telemetryManager;
   }

   IHttpHelper getHttpHelper() {
      return this.httpHelper;
   }

   ServerSideTelemetry getServerSideTelemetry() {
      return this.serverSideTelemetry;
   }
}
