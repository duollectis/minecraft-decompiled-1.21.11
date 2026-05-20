package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import com.azure.json.ReadValueCallback;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JsonHelper {
   private static final Logger LOG = LoggerFactory.getLogger(JsonHelper.class);

   private JsonHelper() {
   }

   static IdToken createIdTokenFromEncodedTokenString(String token) {
      return convertJsonStringToJsonSerializableObject(getTokenPayloadClaims(token), IdToken::fromJson);
   }

   static String getTokenPayloadClaims(String token) {
      try {
         return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
      } catch (ArrayIndexOutOfBoundsException var2) {
         LOG.error("Error parsing ID token, missing payload section.");
         throw new MsalClientException("Error parsing ID token, missing payload section.", "invalid_jwt");
      }
   }

   static Map<String, Object> parseJsonToMap(String jsonString) {
      if (StringHelper.isBlank(jsonString)) {
         return new HashMap<>();
      } else {
         try {
            JsonReader jsonReader = JsonProviders.createReader(jsonString);
            Throwable var2 = null;

            Map var3;
            try {
               jsonReader.nextToken();
               var3 = parseJsonObject(jsonReader);
            } catch (Throwable var13) {
               var2 = var13;
               throw var13;
            } finally {
               if (jsonReader != null) {
                  if (var2 != null) {
                     try {
                        jsonReader.close();
                     } catch (Throwable var12) {
                        var2.addSuppressed(var12);
                     }
                  } else {
                     jsonReader.close();
                  }
               }
            }

            return var3;
         } catch (IOException var15) {
            LOG.error("JSON parsing error when attempting to convert JSON into a Map.");
            throw new MsalJsonParsingException(var15.getMessage(), "invalid_json");
         }
      }
   }

   private static Map<String, Object> parseJsonObject(JsonReader jsonReader) throws IOException {
      Map<String, Object> object = new HashMap<>();

      while (jsonReader.nextToken() != JsonToken.END_OBJECT) {
         String fieldName = jsonReader.getFieldName();
         Object value = parseValue(jsonReader);
         object.put(fieldName, handleSpecialFields(fieldName, value));
      }

      return object;
   }

   private static Object handleSpecialFields(String fieldName, Object value) {
      if ("aud".equals(fieldName) && value instanceof String) {
         ArrayList<String> list = new ArrayList<>();
         list.add((String)value);
         return list;
      } else {
         return isTimestampField(fieldName) && value instanceof Number ? new Date(((Number)value).longValue() * 1000L) : value;
      }
   }

   private static boolean isTimestampField(String fieldName) {
      return "exp".equals(fieldName) || "iat".equals(fieldName) || "nbf".equals(fieldName);
   }

   private static Object parseValue(JsonReader jsonReader) throws IOException {
      JsonToken token = jsonReader.currentToken();
      switch (token) {
         case STRING:
            return jsonReader.getString();
         case NUMBER:
            try {
               return jsonReader.getLong();
            } catch (ArithmeticException var3) {
               return jsonReader.getDouble();
            }
         case BOOLEAN:
            return jsonReader.getBoolean();
         case NULL:
            return null;
         case START_ARRAY:
            return jsonReader.readArray(JsonReader::readUntyped);
         case START_OBJECT:
            return parseJsonObject(jsonReader);
         default:
            jsonReader.skipChildren();
            return null;
      }
   }

   static <T extends JsonSerializable<T>> T convertJsonStringToJsonSerializableObject(String jsonResponse, ReadValueCallback<JsonReader, T> readFunction) {
      try {
         JsonReader jsonReader = JsonProviders.createReader(jsonResponse);
         Throwable var3 = null;

         JsonSerializable var4;
         try {
            var4 = (JsonSerializable)readFunction.read(jsonReader);
         } catch (Throwable var14) {
            var3 = var14;
            throw var14;
         } finally {
            if (jsonReader != null) {
               if (var3 != null) {
                  try {
                     jsonReader.close();
                  } catch (Throwable var13) {
                     var3.addSuppressed(var13);
                  }
               } else {
                  jsonReader.close();
               }
            }
         }

         return (T)var4;
      } catch (Exception var16) {
         throw new MsalJsonParsingException(var16.getMessage(), "invalid_json");
      }
   }

   static <T extends JsonSerializable<T>> String convertJsonSerializableObjectToString(T jsonSerializable) {
      try {
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         JsonWriter jsonWriter = JsonProviders.createWriter(outputStream);
         jsonSerializable.toJson(jsonWriter);
         jsonWriter.flush();
         return outputStream.toString(StandardCharsets.UTF_8.name());
      } catch (Exception var3) {
         throw new MsalClientException("Error serializing object to JSON: " + var3.getMessage(), "invalid_json");
      }
   }

   static Map<String, String> convertJsonToMap(String jsonString) {
      try {
         JsonReader reader = JsonProviders.createReader(jsonString);
         Throwable var2 = null;

         Map var3;
         try {
            reader.nextToken();
            var3 = reader.readMap(JsonReader::getString);
         } catch (Throwable var13) {
            var2 = var13;
            throw var13;
         } finally {
            if (reader != null) {
               if (var2 != null) {
                  try {
                     reader.close();
                  } catch (Throwable var12) {
                     var2.addSuppressed(var12);
                  }
               } else {
                  reader.close();
               }
            }
         }

         return var3;
      } catch (IOException var15) {
         throw new MsalClientException("Could not parse JSON from HttpResponse body: " + var15.getMessage(), "invalid_json");
      }
   }

   static void validateJsonFormat(String jsonString) {
      try {
         JsonReader reader = JsonProviders.createReader(jsonString);
         Throwable var2 = null;

         try {
            while (reader.nextToken() != JsonToken.END_DOCUMENT) {
               reader.skipChildren();
            }
         } catch (Throwable var12) {
            var2 = var12;
            throw var12;
         } finally {
            if (reader != null) {
               if (var2 != null) {
                  try {
                     reader.close();
                  } catch (Throwable var11) {
                     var2.addSuppressed(var11);
                  }
               } else {
                  reader.close();
               }
            }
         }
      } catch (IOException var14) {
         throw new MsalClientException(var14.getMessage(), "invalid_json");
      }
   }

   public static String formCapabilitiesJson(Set<String> clientCapabilities) {
      if (clientCapabilities != null && !clientCapabilities.isEmpty()) {
         ClaimsRequest cr = new ClaimsRequest();
         RequestedClaimAdditionalInfo capabilitiesValues = new RequestedClaimAdditionalInfo(false, null, new ArrayList<>(clientCapabilities));
         cr.requestClaimInAccessToken("xms_cc", capabilitiesValues);
         return cr.formatAsJSONString();
      } else {
         return null;
      }
   }

   static String mergeJSONString(String mainJsonString, String addJsonString) {
      try {
         Map<String, Object> mainMap = parseJsonToMap(mainJsonString);
         Map<String, Object> addMap = parseJsonToMap(addJsonString);
         mergeJsonMaps(mainMap, addMap);
         return writeJsonMap(mainMap);
      } catch (IOException var4) {
         throw new MsalClientException(var4.getMessage(), "invalid_json");
      }
   }

   private static void mergeJsonMaps(Map<String, Object> mainMap, Map<String, Object> addMap) {
      if (addMap != null) {
         for (Entry<String, Object> entry : addMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (mainMap.containsKey(key) && mainMap.get(key) instanceof Map && value instanceof Map) {
               mergeJsonMaps((Map<String, Object>)mainMap.get(key), (Map<String, Object>)value);
            } else {
               mainMap.put(key, value);
            }
         }
      }
   }

   static String writeJsonMap(Map<String, Object> map) throws IOException {
      StringWriter stringWriter = new StringWriter();

      try {
         JsonWriter jsonWriter = JsonProviders.createWriter(stringWriter);
         Throwable var3 = null;

         String var17;
         try {
            jsonWriter.writeStartObject();

            for (Entry<String, Object> entry : map.entrySet()) {
               jsonWriter.writeUntypedField(entry.getKey(), entry.getValue());
            }

            jsonWriter.writeEndObject();
            jsonWriter.flush();
            var17 = stringWriter.toString();
         } catch (Throwable var14) {
            var3 = var14;
            throw var14;
         } finally {
            if (jsonWriter != null) {
               if (var3 != null) {
                  try {
                     jsonWriter.close();
                  } catch (Throwable var13) {
                     var3.addSuppressed(var13);
                  }
               } else {
                  jsonWriter.close();
               }
            }
         }

         return var17;
      } catch (Exception var16) {
         throw new MsalClientException("Error writing JSON map to string: " + var16.getMessage(), "invalid_json");
      }
   }
}
