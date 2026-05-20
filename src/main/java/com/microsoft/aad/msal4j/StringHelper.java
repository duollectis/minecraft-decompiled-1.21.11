package com.microsoft.aad.msal4j;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

final class StringHelper {
   static String EMPTY_STRING = "";

   static boolean isBlank(String str) {
      return str == null || str.trim().isEmpty();
   }

   static String createBase64EncodedSha256Hash(String stringToHash) {
      return createSha256Hash(stringToHash, true);
   }

   static String createSha256Hash(String stringToHash) {
      return createSha256Hash(stringToHash, false);
   }

   private static String createSha256Hash(String stringToHash, boolean base64Encode) {
      String res;
      try {
         MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
         byte[] hash = messageDigest.digest(stringToHash.getBytes(StandardCharsets.UTF_8));
         if (base64Encode) {
            res = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
         } else {
            res = new String(hash, StandardCharsets.UTF_8);
         }
      } catch (NoSuchAlgorithmException var5) {
         res = null;
      }

      return res;
   }

   static String createSha256HashHexString(String stringToHash) {
      if (stringToHash != null && !stringToHash.isEmpty()) {
         try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(stringToHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
               String hex = Integer.toHexString(255 & b);
               if (hex.length() == 1) {
                  hexString.append('0');
               }

               hexString.append(hex);
            }

            return hexString.toString();
         } catch (NoSuchAlgorithmException var9) {
            throw new MsalClientException("Failed to create SHA-256 hash: " + var9.getMessage(), "crypto_error");
         }
      } else {
         throw new IllegalArgumentException("String to hash cannot be null or empty");
      }
   }

   static boolean isNullOrBlank(String str) {
      return str == null || str.trim().isEmpty();
   }

   static String serializeQueryParameters(Map<String, String> params) {
      if (params != null && !params.isEmpty()) {
         Map<String, String> encodedParams = urlEncodeMap(params);
         StringBuilder sb = new StringBuilder();

         for (Entry<String, String> entry : encodedParams.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
               String value = entry.getValue();
               if (sb.length() > 0) {
                  sb.append('&');
               }

               sb.append(entry.getKey());
               sb.append('=');
               sb.append(value);
            }
         }

         return sb.toString();
      } else {
         return "";
      }
   }

   static Map<String, String> urlEncodeMap(Map<String, String> params) {
      if (params != null && !params.isEmpty()) {
         Map<String, String> out = new LinkedHashMap<>();

         for (Entry<String, String> entry : params.entrySet()) {
            try {
               String newKey = entry.getKey() != null ? URLEncoder.encode(entry.getKey(), "utf-8") : null;
               String newValue = entry.getValue() != null ? URLEncoder.encode(entry.getValue(), "utf-8") : null;
               out.put(newKey, newValue);
            } catch (UnsupportedEncodingException var6) {
               throw new RuntimeException(var6);
            }
         }

         return out;
      } else {
         return params;
      }
   }

   static Map<String, String> parseQueryParameters(String query) {
      Map<String, String> params = new LinkedHashMap<>();
      if (isBlank(query)) {
         return params;
      } else {
         StringTokenizer st = new StringTokenizer(query.trim(), "&");

         while (st.hasMoreTokens()) {
            String param = st.nextToken();
            String[] pair = param.split("=", 2);

            String key;
            String value;
            try {
               key = URLDecoder.decode(pair[0], "utf-8");
               value = pair.length > 1 ? URLDecoder.decode(pair[1], "utf-8") : "";
            } catch (IllegalArgumentException | UnsupportedEncodingException var8) {
               continue;
            }

            params.put(key, value);
         }

         return params;
      }
   }

   static Map<String, List<String>> convertToMultiValueMap(Map<String, String> singleValueMap) {
      Map<String, List<String>> multiValueMap = new HashMap<>();
      if (singleValueMap != null) {
         for (Entry<String, String> entry : singleValueMap.entrySet()) {
            multiValueMap.put(entry.getKey(), Collections.singletonList(entry.getValue()));
         }
      }

      return multiValueMap;
   }
}
