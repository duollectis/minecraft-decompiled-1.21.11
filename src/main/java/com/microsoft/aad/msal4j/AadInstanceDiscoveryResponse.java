package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;
import java.util.List;

class AadInstanceDiscoveryResponse implements JsonSerializable<AadInstanceDiscoveryResponse> {
   private String tenantDiscoveryEndpoint;
   private List<InstanceDiscoveryMetadataEntry> metadata;
   private String errorDescription;
   private List<Long> errorCodes;
   private String error;
   private String correlationId;

   public static AadInstanceDiscoveryResponse fromJson(JsonReader jsonReader) throws IOException {
      AadInstanceDiscoveryResponse response = new AadInstanceDiscoveryResponse();
      return (AadInstanceDiscoveryResponse)jsonReader.readObject(reader -> {
         while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            switch (fieldName) {
               case "tenant_discovery_endpoint":
                  response.tenantDiscoveryEndpoint = reader.getString();
                  break;
               case "metadata":
                  response.metadata = reader.readArray(InstanceDiscoveryMetadataEntry::fromJson);
                  break;
               case "error_description":
                  response.errorDescription = reader.getString();
                  break;
               case "error_codes":
                  response.errorCodes = reader.readArray(JsonReader::getLong);
                  break;
               case "error":
                  response.error = reader.getString();
                  break;
               case "correlation_id":
                  response.correlationId = reader.getString();
                  break;
               default:
                  reader.skipChildren();
            }
         }

         return response;
      });
   }

   public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeStringField("tenant_discovery_endpoint", this.tenantDiscoveryEndpoint);
      jsonWriter.writeArrayField("metadata", this.metadata, JsonWriter::writeJson);
      jsonWriter.writeStringField("error_description", this.errorDescription);
      jsonWriter.writeArrayField("error_codes", this.errorCodes, JsonWriter::writeLong);
      jsonWriter.writeStringField("error", this.error);
      jsonWriter.writeStringField("correlation_id", this.correlationId);
      jsonWriter.writeEndObject();
      return jsonWriter;
   }

   String tenantDiscoveryEndpoint() {
      return this.tenantDiscoveryEndpoint;
   }

   List<InstanceDiscoveryMetadataEntry> metadata() {
      return this.metadata;
   }

   String errorDescription() {
      return this.errorDescription;
   }

   List<Long> errorCodes() {
      return this.errorCodes;
   }

   String error() {
      return this.error;
   }

   String correlationId() {
      return this.correlationId;
   }
}
