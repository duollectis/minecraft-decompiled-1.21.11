package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class DeviceCodeFlowParameters implements IAcquireTokenParameters {
   private Set<String> scopes;
   private Consumer<DeviceCode> deviceCodeConsumer;
   private ClaimsRequest claims;
   private Map<String, String> extraHttpHeaders;
   private Map<String, String> extraQueryParameters;
   private String tenant;

   private DeviceCodeFlowParameters(
      Set<String> scopes,
      Consumer<DeviceCode> deviceCodeConsumer,
      ClaimsRequest claims,
      Map<String, String> extraHttpHeaders,
      Map<String, String> extraQueryParameters,
      String tenant
   ) {
      this.scopes = scopes;
      this.deviceCodeConsumer = deviceCodeConsumer;
      this.claims = claims;
      this.extraHttpHeaders = extraHttpHeaders;
      this.extraQueryParameters = extraQueryParameters;
      this.tenant = tenant;
   }

   private static DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder builder() {
      return new DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder();
   }

   public static DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder builder(Set<String> scopes, Consumer<DeviceCode> deviceCodeConsumer) {
      ParameterValidationUtils.validateNotNull("scopes", scopes);
      return builder().scopes(scopes).deviceCodeConsumer(deviceCodeConsumer);
   }

   @Override
   public Set<String> scopes() {
      return this.scopes;
   }

   public Consumer<DeviceCode> deviceCodeConsumer() {
      return this.deviceCodeConsumer;
   }

   @Override
   public ClaimsRequest claims() {
      return this.claims;
   }

   @Override
   public Map<String, String> extraHttpHeaders() {
      return this.extraHttpHeaders;
   }

   @Override
   public Map<String, String> extraQueryParameters() {
      return this.extraQueryParameters;
   }

   @Override
   public String tenant() {
      return this.tenant;
   }

   public static class DeviceCodeFlowParametersBuilder {
      private Set<String> scopes;
      private Consumer<DeviceCode> deviceCodeConsumer;
      private ClaimsRequest claims;
      private Map<String, String> extraHttpHeaders;
      private Map<String, String> extraQueryParameters;
      private String tenant;

      DeviceCodeFlowParametersBuilder() {
      }

      public DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder scopes(Set<String> scopes) {
         ParameterValidationUtils.validateNotNull("scopes", scopes);
         this.scopes = scopes;
         return this;
      }

      public DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder deviceCodeConsumer(Consumer<DeviceCode> deviceCodeConsumer) {
         ParameterValidationUtils.validateNotNull("deviceCodeConsumer", this.scopes);
         this.deviceCodeConsumer = deviceCodeConsumer;
         return this;
      }

      public DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder claims(ClaimsRequest claims) {
         this.claims = claims;
         return this;
      }

      public DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
         this.extraHttpHeaders = extraHttpHeaders;
         return this;
      }

      public DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
         this.extraQueryParameters = extraQueryParameters;
         return this;
      }

      public DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder tenant(String tenant) {
         this.tenant = tenant;
         return this;
      }

      public DeviceCodeFlowParameters build() {
         return new DeviceCodeFlowParameters(this.scopes, this.deviceCodeConsumer, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant);
      }

      @Override
      public String toString() {
         return "DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder(scopes="
            + this.scopes
            + ", deviceCodeConsumer="
            + this.deviceCodeConsumer
            + ", claims="
            + this.claims
            + ", extraHttpHeaders="
            + this.extraHttpHeaders
            + ", extraQueryParameters="
            + this.extraQueryParameters
            + ", tenant="
            + this.tenant
            + ")";
      }
   }
}
