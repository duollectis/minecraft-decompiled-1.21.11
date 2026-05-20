package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class AccessTokenCacheEntity extends Credential implements JsonSerializable<Credential> {
   private String credentialType;
   protected String realm;
   private String target;
   private String cachedAt;
   private String expiresOn;
   private String extExpiresOn;
   private String refreshOn;

   String getKey() {
      List<String> keyParts = new ArrayList<>();
      keyParts.add(StringHelper.isBlank(this.homeAccountId) ? "" : this.homeAccountId);
      keyParts.add(this.environment);
      keyParts.add(this.credentialType);
      keyParts.add(this.clientId);
      keyParts.add(this.realm);
      keyParts.add(this.target);
      return String.join("-", keyParts).toLowerCase();
   }

   static AccessTokenCacheEntity fromJson(JsonReader jsonReader) throws IOException {
      AccessTokenCacheEntity entity = new AccessTokenCacheEntity();
      return (AccessTokenCacheEntity)jsonReader.readObject(reader -> {
         while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            switch (fieldName) {
               case "home_account_id":
                  entity.homeAccountId = reader.getString();
                  break;
               case "environment":
                  entity.environment = reader.getString();
                  break;
               case "credential_type":
                  entity.credentialType = reader.getString();
                  break;
               case "client_id":
                  entity.clientId = reader.getString();
                  break;
               case "secret":
                  entity.secret = reader.getString();
                  break;
               case "realm":
                  entity.realm = reader.getString();
                  break;
               case "target":
                  entity.target = reader.getString();
                  break;
               case "cached_at":
                  entity.cachedAt = reader.getString();
                  break;
               case "expires_on":
                  entity.expiresOn = reader.getString();
                  break;
               case "extended_expires_on":
                  entity.extExpiresOn = reader.getString();
                  break;
               case "refresh_on":
                  entity.refreshOn = reader.getString();
                  break;
               case "user_assertion_hash":
                  entity.userAssertionHash = reader.getString();
                  break;
               default:
                  reader.skipChildren();
            }
         }

         return entity;
      });
   }

   @Override
   public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeStringField("home_account_id", this.homeAccountId);
      jsonWriter.writeStringField("environment", this.environment);
      jsonWriter.writeStringField("credential_type", this.credentialType);
      jsonWriter.writeStringField("client_id", this.clientId);
      jsonWriter.writeStringField("secret", this.secret);
      jsonWriter.writeStringField("realm", this.realm);
      jsonWriter.writeStringField("target", this.target);
      jsonWriter.writeStringField("cached_at", this.cachedAt);
      jsonWriter.writeStringField("expires_on", this.expiresOn);
      jsonWriter.writeStringField("extended_expires_on", this.extExpiresOn);
      jsonWriter.writeStringField("refresh_on", this.refreshOn);
      jsonWriter.writeStringField("user_assertion_hash", this.userAssertionHash);
      jsonWriter.writeEndObject();
      return jsonWriter;
   }

   String target() {
      return this.target;
   }

   String cachedAt() {
      return this.cachedAt;
   }

   String expiresOn() {
      return this.expiresOn;
   }

   String extExpiresOn() {
      return this.extExpiresOn;
   }

   String refreshOn() {
      return this.refreshOn;
   }

   void credentialType(String credentialType) {
      this.credentialType = credentialType;
   }

   void realm(String realm) {
      this.realm = realm;
   }

   void target(String target) {
      this.target = target;
   }

   void cachedAt(String cachedAt) {
      this.cachedAt = cachedAt;
   }

   void expiresOn(String expiresOn) {
      this.expiresOn = expiresOn;
   }

   void extExpiresOn(String extExpiresOn) {
      this.extExpiresOn = extExpiresOn;
   }

   void refreshOn(String refreshOn) {
      this.refreshOn = refreshOn;
   }
}
