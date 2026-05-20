package net.minecraft.client.realms.dto;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public record UploadInfo(boolean worldClosed, @Nullable String token, URI uploadEndpoint) {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String HTTP_PROTOCOL = "http://";
   private static final int PORT = 8080;
   private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^[a-zA-Z][-a-zA-Z0-9+.]+:");

   public static @Nullable UploadInfo parse(String json) {
      try {
         JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
         String string = JsonUtils.getNullableStringOr("uploadEndpoint", jsonObject, null);
         if (string != null) {
            int i = JsonUtils.getIntOr("port", jsonObject, -1);
            URI uRI = getUrl(string, i);
            if (uRI != null) {
               boolean bl = JsonUtils.getBooleanOr("worldClosed", jsonObject, false);
               String string2 = JsonUtils.getNullableStringOr("token", jsonObject, null);
               return new UploadInfo(bl, string2, uRI);
            }
         }
      } catch (Exception var7) {
         LOGGER.error("Could not parse UploadInfo", var7);
      }

      return null;
   }

   @VisibleForTesting
   public static @Nullable URI getUrl(String url, int port) {
      Matcher matcher = PROTOCOL_PATTERN.matcher(url);
      String string = getUrlWithProtocol(url, matcher);

      try {
         URI uRI = new URI(string);
         int i = getPort(port, uRI.getPort());
         return i != uRI.getPort() ? new URI(uRI.getScheme(), uRI.getUserInfo(), uRI.getHost(), i, uRI.getPath(), uRI.getQuery(), uRI.getFragment()) : uRI;
      } catch (URISyntaxException var6) {
         LOGGER.warn("Failed to parse URI {}", string, var6);
         return null;
      }
   }

   private static int getPort(int port, int urlPort) {
      if (port != -1) {
         return port;
      } else {
         return urlPort != -1 ? urlPort : 8080;
      }
   }

   private static String getUrlWithProtocol(String url, Matcher matcher) {
      return matcher.find() ? url : "http://" + url;
   }

   public static String createRequestContent(@Nullable String token) {
      JsonObject jsonObject = new JsonObject();
      if (token != null) {
         jsonObject.addProperty("token", token);
      }

      return jsonObject.toString();
   }
}
