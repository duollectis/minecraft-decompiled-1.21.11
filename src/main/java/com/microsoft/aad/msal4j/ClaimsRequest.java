package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ClaimsRequest implements JsonSerializable<ClaimsRequest> {
   List<RequestedClaim> idTokenRequestedClaims = new ArrayList<>();
   List<RequestedClaim> userInfoRequestedClaims = new ArrayList<>();
   List<RequestedClaim> accessTokenRequestedClaims = new ArrayList<>();

   public void requestClaimInIdToken(String claim, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
      this.idTokenRequestedClaims.add(new RequestedClaim(claim, requestedClaimAdditionalInfo));
   }

   protected void requestClaimInUserInfo(String claim, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
      this.userInfoRequestedClaims.add(new RequestedClaim(claim, requestedClaimAdditionalInfo));
   }

   protected void requestClaimInAccessToken(String claim, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
      this.accessTokenRequestedClaims.add(new RequestedClaim(claim, requestedClaimAdditionalInfo));
   }

   public String formatAsJSONString() {
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
         JsonWriter jsonWriter = JsonProviders.createWriter(outputStream);
         Throwable var4 = null;

         String var5;
         try {
            this.toJson(jsonWriter);
            jsonWriter.flush();
            var5 = outputStream.toString(StandardCharsets.UTF_8.name());
         } catch (Throwable var30) {
            var4 = var30;
            throw var30;
         } finally {
            if (jsonWriter != null) {
               if (var4 != null) {
                  try {
                     jsonWriter.close();
                  } catch (Throwable var29) {
                     var4.addSuppressed(var29);
                  }
               } else {
                  jsonWriter.close();
               }
            }
         }

         return var5;
      } catch (IOException var34) {
         throw new MsalClientException("Could not convert ClaimsRequest to string: " + var34.getMessage(), "invalid_json");
      }
   }

   public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      this.writeClaimsToJsonWriter(jsonWriter, "id_token", this.idTokenRequestedClaims);
      this.writeClaimsToJsonWriter(jsonWriter, "userinfo", this.userInfoRequestedClaims);
      this.writeClaimsToJsonWriter(jsonWriter, "access_token", this.accessTokenRequestedClaims);
      jsonWriter.writeEndObject();
      return jsonWriter;
   }

   private void writeClaimsToJsonWriter(JsonWriter jsonWriter, String sectionName, List<RequestedClaim> claims) throws IOException {
      if (!claims.isEmpty()) {
         jsonWriter.writeStartObject(sectionName);

         for (RequestedClaim claim : claims) {
            if (claim.name != null) {
               if (claim.getRequestedClaimAdditionalInfo() != null) {
                  jsonWriter.writeJsonField(claim.name, claim.getRequestedClaimAdditionalInfo());
               } else {
                  jsonWriter.writeNullField(claim.name);
               }
            }
         }

         jsonWriter.writeEndObject();
      }
   }

   public static ClaimsRequest formatAsClaimsRequest(String claims) {
      try {
         JsonReader jsonReader = JsonProviders.createReader(claims);
         Throwable var2 = null;

         ClaimsRequest var4;
         try {
            ClaimsRequest claimsRequest = new ClaimsRequest();
            var4 = (ClaimsRequest)jsonReader.readObject(reader -> {
               if (reader.currentToken() != JsonToken.START_OBJECT) {
                  throw new IllegalStateException("Expected start of object but was " + reader.currentToken());
               } else {
                  while (reader.nextToken() != JsonToken.END_OBJECT) {
                     parseClaims(reader, claimsRequest, reader.getFieldName());
                  }

                  return claimsRequest;
               }
            });
         } catch (Throwable var14) {
            var2 = var14;
            throw var14;
         } finally {
            if (jsonReader != null) {
               if (var2 != null) {
                  try {
                     jsonReader.close();
                  } catch (Throwable var13) {
                     var2.addSuppressed(var13);
                  }
               } else {
                  jsonReader.close();
               }
            }
         }

         return var4;
      } catch (IOException var16) {
         throw new MsalClientException("Could not convert string to ClaimsRequest: " + var16.getMessage(), "invalid_json");
      }
   }

   private static void parseClaims(JsonReader jsonReader, ClaimsRequest claimsRequest, String section) throws IOException {
      if (jsonReader.currentToken() != JsonToken.FIELD_NAME) {
         jsonReader.nextToken();
      }

      jsonReader.nextToken();
      if (jsonReader.currentToken() != JsonToken.NULL) {
         if (jsonReader.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("Expected start of object but was " + jsonReader.currentToken());
         } else {
            while (jsonReader.nextToken() != JsonToken.END_OBJECT) {
               String claimName = jsonReader.getFieldName();
               jsonReader.nextToken();
               RequestedClaimAdditionalInfo claimInfo = null;
               if (jsonReader.currentToken() == JsonToken.START_OBJECT) {
                  boolean essential = false;
                  String value = null;
                  List<String> values = null;

                  while (jsonReader.nextToken() != JsonToken.END_OBJECT) {
                     String fieldName = jsonReader.getFieldName();
                     jsonReader.nextToken();
                     switch (fieldName) {
                        case "essential":
                           essential = jsonReader.getBoolean();
                           break;
                        case "value":
                           value = jsonReader.getString();
                           break;
                        case "values":
                           values = new ArrayList<>();
                           if (jsonReader.currentToken() == JsonToken.START_ARRAY) {
                              while (jsonReader.nextToken() != JsonToken.END_ARRAY) {
                                 values.add(jsonReader.getString());
                              }
                           }
                           break;
                        default:
                           jsonReader.skipChildren();
                     }
                  }

                  if (essential || value != null || values != null) {
                     claimInfo = new RequestedClaimAdditionalInfo(essential, value, values);
                  }
               }

               switch (section) {
                  case "access_token":
                     claimsRequest.requestClaimInAccessToken(claimName, claimInfo);
                     break;
                  case "id_token":
                     claimsRequest.requestClaimInIdToken(claimName, claimInfo);
                     break;
                  case "userinfo":
                     claimsRequest.requestClaimInUserInfo(claimName, claimInfo);
               }
            }
         }
      }
   }

   public List<RequestedClaim> getIdTokenRequestedClaims() {
      return this.idTokenRequestedClaims;
   }

   public void setIdTokenRequestedClaims(List<RequestedClaim> idTokenRequestedClaims) {
      this.idTokenRequestedClaims = idTokenRequestedClaims;
   }
}
