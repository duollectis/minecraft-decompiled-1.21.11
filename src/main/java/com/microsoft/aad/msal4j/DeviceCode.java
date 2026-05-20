package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;

public final class DeviceCode implements JsonSerializable<DeviceCode> {
   private String userCode;
   private String deviceCode;
   private String verificationUri;
   private long expiresIn;
   private long interval;
   private String message;
   private String correlationId = null;
   private String clientId = null;
   private String scopes = null;

   public static DeviceCode fromJson(JsonReader jsonReader) throws IOException {
      DeviceCode deviceCode = new DeviceCode();
      return (DeviceCode)jsonReader.readObject(reader -> {
         while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            switch (fieldName) {
               case "user_code":
                  deviceCode.userCode = reader.getString();
                  break;
               case "device_code":
                  deviceCode.deviceCode = reader.getString();
                  break;
               case "verification_uri":
                  deviceCode.verificationUri = reader.getString();
                  break;
               case "expires_in":
                  deviceCode.expiresIn = reader.getLong();
                  break;
               case "interval":
                  deviceCode.interval = reader.getLong();
                  break;
               case "message":
                  deviceCode.message = reader.getString();
                  break;
               default:
                  reader.skipChildren();
            }
         }

         return deviceCode;
      });
   }

   public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeStringField("user_code", this.userCode);
      jsonWriter.writeStringField("device_code", this.deviceCode);
      jsonWriter.writeStringField("verification_uri", this.verificationUri);
      jsonWriter.writeNumberField("expires_in", this.expiresIn);
      jsonWriter.writeNumberField("interval", this.interval);
      jsonWriter.writeStringField("message", this.message);
      jsonWriter.writeEndObject();
      return jsonWriter;
   }

   public String userCode() {
      return this.userCode;
   }

   public String deviceCode() {
      return this.deviceCode;
   }

   public String verificationUri() {
      return this.verificationUri;
   }

   public long expiresIn() {
      return this.expiresIn;
   }

   public long interval() {
      return this.interval;
   }

   public String message() {
      return this.message;
   }

   protected String correlationId() {
      return this.correlationId;
   }

   protected String clientId() {
      return this.clientId;
   }

   protected String scopes() {
      return this.scopes;
   }

   protected DeviceCode correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
   }

   protected DeviceCode clientId(String clientId) {
      this.clientId = clientId;
      return this;
   }

   protected DeviceCode scopes(String scopes) {
      this.scopes = scopes;
      return this;
   }
}
