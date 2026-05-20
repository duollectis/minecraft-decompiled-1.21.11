package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RequestedClaimAdditionalInfo implements JsonSerializable<RequestedClaimAdditionalInfo> {
   private boolean essential;
   private String value;
   private List<String> values;

   public RequestedClaimAdditionalInfo(boolean essential, String value, List<String> values) {
      this.essential = essential;
      this.value = value;
      this.values = values;
   }

   public RequestedClaimAdditionalInfo fromJson(JsonReader jsonReader) throws IOException {
      if (jsonReader.currentToken() != JsonToken.START_OBJECT) {
         jsonReader.nextToken();
         if (jsonReader.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("Expected start of object but was " + jsonReader.currentToken());
         }
      }

      while (jsonReader.nextToken() != JsonToken.END_OBJECT) {
         String fieldName = jsonReader.getFieldName();
         jsonReader.nextToken();
         switch (fieldName) {
            case "essential":
               this.essential = jsonReader.getBoolean();
               break;
            case "value":
               this.value = jsonReader.getString();
               break;
            case "values":
               this.values = new ArrayList<>();

               while (jsonReader.nextToken() != JsonToken.END_ARRAY) {
                  this.values.add(jsonReader.getString());
               }
               break;
            default:
               jsonReader.skipChildren();
         }
      }

      return this;
   }

   public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      if (this.essential) {
         jsonWriter.writeBooleanField("essential", this.essential);
      }

      if (this.value != null) {
         jsonWriter.writeStringField("value", this.value);
      }

      if (this.values != null && !this.values.isEmpty()) {
         jsonWriter.writeStartArray("values");

         for (String val : this.values) {
            jsonWriter.writeString(val);
         }

         jsonWriter.writeEndArray();
      }

      jsonWriter.writeEndObject();
      return jsonWriter;
   }

   public boolean isEssential() {
      return this.essential;
   }

   public String getValue() {
      return this.value;
   }

   public List<String> getValues() {
      return this.values;
   }

   public void setEssential(boolean essential) {
      this.essential = essential;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public void setValues(List<String> values) {
      this.values = values;
   }
}
