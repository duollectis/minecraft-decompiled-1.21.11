package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ManagedIdentityResponse implements JsonSerializable<ManagedIdentityResponse> {
   private static final Logger LOG = LoggerFactory.getLogger(ManagedIdentityResponse.class);
   String tokenType;
   String accessToken;
   String expiresOn;
   String resource;
   String clientId;

   public static ManagedIdentityResponse fromJson(JsonReader jsonReader) throws IOException {
      ManagedIdentityResponse response = new ManagedIdentityResponse();
      return (ManagedIdentityResponse)jsonReader.readObject(reader -> {
         while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            switch (fieldName) {
               case "token_type":
                  response.tokenType = reader.getString();
                  break;
               case "access_token":
                  response.accessToken = reader.getString();
                  break;
               case "expires_on":
                  response.expiresOn = reader.getString();
                  break;
               case "resource":
                  response.resource = reader.getString();
                  break;
               case "client_id":
                  response.clientId = reader.getString();
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
      jsonWriter.writeStringField("token_type", this.tokenType);
      jsonWriter.writeStringField("access_token", this.accessToken);
      jsonWriter.writeStringField("expires_on", this.expiresOn);
      jsonWriter.writeStringField("resource", this.resource);
      jsonWriter.writeStringField("client_id", this.clientId);
      jsonWriter.writeEndObject();
      return jsonWriter;
   }

   public String getTokenType() {
      return this.tokenType;
   }

   public String getAccessToken() {
      return this.accessToken;
   }

   public String getExpiresOn() {
      return this.expiresOn;
   }

   public String getResource() {
      return this.resource;
   }

   public String getClientId() {
      return this.clientId;
   }
}
