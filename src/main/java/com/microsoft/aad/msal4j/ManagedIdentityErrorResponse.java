package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;

public class ManagedIdentityErrorResponse implements JsonSerializable<ManagedIdentityErrorResponse> {
   private String message;
   private String correlationId;
   private String error;
   private String errorDescription;

   public static ManagedIdentityErrorResponse fromJson(JsonReader jsonReader) throws IOException {
      ManagedIdentityErrorResponse response = new ManagedIdentityErrorResponse();
      return (ManagedIdentityErrorResponse)jsonReader.readObject(reader -> {
         while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            switch (fieldName) {
               case "message":
                  response.message = reader.getString();
                  break;
               case "correlationId":
                  response.correlationId = reader.getString();
                  break;
               case "error":
                  if (reader.currentToken() == JsonToken.START_OBJECT) {
                     ManagedIdentityErrorResponse.ErrorField errorField = ManagedIdentityErrorResponse.ErrorField.fromJson(reader);
                     response.error = errorField.getCode();
                     response.message = errorField.getMessage();
                     break;
                  }

                  response.error = reader.getString();
                  break;
               case "error_description":
                  response.errorDescription = reader.getString();
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
      jsonWriter.writeStringField("message", this.message);
      jsonWriter.writeStringField("correlationId", this.correlationId);
      jsonWriter.writeStringField("error", this.error);
      jsonWriter.writeStringField("error_description", this.errorDescription);
      jsonWriter.writeEndObject();
      return jsonWriter;
   }

   public String getMessage() {
      return this.message;
   }

   public String getCorrelationId() {
      return this.correlationId;
   }

   public String getError() {
      return this.error;
   }

   public String getErrorDescription() {
      return this.errorDescription;
   }

   private static class ErrorField {
      private String code;
      private String message;

      static ManagedIdentityErrorResponse.ErrorField fromJson(JsonReader jsonReader) throws IOException {
         ManagedIdentityErrorResponse.ErrorField errorField = new ManagedIdentityErrorResponse.ErrorField();
         return (ManagedIdentityErrorResponse.ErrorField)jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
               String fieldName = reader.getFieldName();
               reader.nextToken();
               switch (fieldName) {
                  case "code":
                     errorField.code = reader.getString();
                     break;
                  case "message":
                     errorField.message = reader.getString();
                     break;
                  default:
                     reader.skipChildren();
               }
            }

            return errorField;
         });
      }

      String getCode() {
         return this.code;
      }

      String getMessage() {
         return this.message;
      }
   }
}
