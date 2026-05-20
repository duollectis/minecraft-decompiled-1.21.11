package com.mojang.authlib.minecraft.client;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.yggdrasil.response.ErrorResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftClient {
   private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftClient.class);
   public static final int CONNECT_TIMEOUT_MS = 5000;
   public static final int READ_TIMEOUT_MS = 5000;
   @Nullable
   private final String accessToken;
   private final Proxy proxy;
   private final ObjectMapper objectMapper = ObjectMapper.create();

   public MinecraftClient(@Nullable String accessToken, Proxy proxy) {
      this.accessToken = accessToken;
      this.proxy = (Proxy)Validate.notNull(proxy);
   }

   public static MinecraftClient unauthenticated(Proxy proxy) {
      return new MinecraftClient(null, proxy);
   }

   @Nullable
   public <T> T get(URL url, Class<T> responseClass) {
      Validate.notNull(url);
      Validate.notNull(responseClass);
      HttpURLConnection connection = this.createUrlConnection(url);
      if (this.accessToken != null) {
         connection.setRequestProperty("Authorization", "Bearer " + this.accessToken);
      }

      return this.readInputStream(url, responseClass, connection);
   }

   @Nullable
   public <T> T post(URL url, Class<T> responseClass) {
      Validate.notNull(url);
      Validate.notNull(responseClass);
      HttpURLConnection connection = this.postInternal(url, new byte[0]);
      return this.readInputStream(url, responseClass, connection);
   }

   @Nullable
   public <T> T post(URL url, Object body, Class<T> responseClass) {
      Validate.notNull(url);
      Validate.notNull(body);
      Validate.notNull(responseClass);
      String bodyAsJson = this.objectMapper.writeValueAsString(body);
      byte[] postAsBytes = bodyAsJson.getBytes(StandardCharsets.UTF_8);
      HttpURLConnection connection = this.postInternal(url, postAsBytes);
      return this.readInputStream(url, responseClass, connection);
   }

   @Nullable
   private <T> T readInputStream(URL url, Class<T> clazz, HttpURLConnection connection) {
      InputStream inputStream = null;

      String contentType;
      try {
         int status = connection.getResponseCode();
         if (status >= 400) {
            contentType = connection.getContentType();
            inputStream = connection.getErrorStream();
            if (inputStream != null) {
               String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
               if (contentType != null && contentType.startsWith("text/html")) {
                  LOGGER.error("Got an error with a html body connecting to {}: {}", url.toString(), result);
                  throw new MinecraftClientHttpException(status);
               }

               ErrorResponse errorResponse = this.objectMapper.readValue(result, ErrorResponse.class);
               throw new MinecraftClientHttpException(status, errorResponse);
            }

            throw new MinecraftClientHttpException(status);
         }

         inputStream = connection.getInputStream();
         String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
         if (!result.isEmpty()) {
            return this.objectMapper.readValue(result, clazz);
         }

         contentType = null;
      } catch (IOException var12) {
         throw new MinecraftClientException(
            MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE, "Failed to read from " + url + " due to " + var12.getMessage(), var12
         );
      } finally {
         IOUtils.closeQuietly(inputStream);
      }

      return (T)contentType;
   }

   private HttpURLConnection postInternal(URL url, byte[] postAsBytes) {
      HttpURLConnection connection = this.createUrlConnection(url);
      OutputStream outputStream = null;

      try {
         connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
         connection.setRequestProperty("Content-Length", postAsBytes.length + "");
         if (this.accessToken != null) {
            connection.setRequestProperty("Authorization", "Bearer " + this.accessToken);
         }

         connection.setRequestMethod("POST");
         connection.setDoOutput(true);
         outputStream = connection.getOutputStream();
         IOUtils.write(postAsBytes, outputStream);
      } catch (IOException var9) {
         throw new MinecraftClientException(MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE, "Failed to POST " + url, var9);
      } finally {
         IOUtils.closeQuietly(outputStream);
      }

      return connection;
   }

   private HttpURLConnection createUrlConnection(URL url) {
      try {
         LOGGER.debug("Connecting to {}", url);
         HttpURLConnection connection = (HttpURLConnection)url.openConnection(this.proxy);
         connection.setConnectTimeout(5000);
         connection.setReadTimeout(5000);
         connection.setUseCaches(false);
         return connection;
      } catch (IOException var3) {
         throw new MinecraftClientException(MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE, "Failed connecting to " + url, var3);
      }
   }
}
