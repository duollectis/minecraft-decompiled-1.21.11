package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class RequestedClaim implements JsonSerializable<RequestedClaim> {
   public String name;
   private RequestedClaimAdditionalInfo requestedClaimAdditionalInfo;

   RequestedClaim() {
   }

   public RequestedClaim(String name, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
      this.name = name;
      this.requestedClaimAdditionalInfo = requestedClaimAdditionalInfo;
   }

   static RequestedClaim fromJson(JsonReader jsonReader) throws IOException {
      RequestedClaim claim = new RequestedClaim();
      return (RequestedClaim)jsonReader.readObject(reader -> {
         if (reader.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("Expected start of object but was " + reader.currentToken());
         } else {
            claim.name = reader.getFieldName();
            RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(false, null, null);
            claim.requestedClaimAdditionalInfo = info.fromJson(reader);
            return claim;
         }
      });
   }

   public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      if (this.name != null && this.requestedClaimAdditionalInfo != null) {
         jsonWriter.writeString(this.name);
         this.requestedClaimAdditionalInfo.toJson(jsonWriter);
      }

      jsonWriter.writeEndObject();
      return jsonWriter;
   }

   RequestedClaimAdditionalInfo getRequestedClaimAdditionalInfo() {
      return this.requestedClaimAdditionalInfo;
   }

   void setRequestedClaimAdditionalInfo(RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
      this.requestedClaimAdditionalInfo = requestedClaimAdditionalInfo;
   }

   protected Map<String, Object> any() {
      return Collections.singletonMap(this.name, this.requestedClaimAdditionalInfo);
   }
}
