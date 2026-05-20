package net.minecraft.server.dedicated.management.network;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import net.minecraft.network.encryption.BearerToken;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Sharable
public class BearerAuthenticationHandler extends ChannelDuplexHandler {
   private final Logger LOGGER = LogUtils.getLogger();
   private static final AttributeKey<Boolean> AUTHENTICATED_KEY = AttributeKey.valueOf("authenticated");
   private static final AttributeKey<Boolean> WEBSOCKET_AUTH_ALLOWED_KEY = AttributeKey.valueOf("websocket_auth_allowed");
   private static final String PROTOCOL = "minecraft-v1";
   private static final String PROTOCOL_PREFIX = "minecraft-v1,";
   public static final String BEARER_PREFIX = "Bearer ";
   private final BearerToken token;
   private final Set<String> allowedOrigins;

   public BearerAuthenticationHandler(BearerToken token, String allowedOrigins) {
      this.token = token;
      this.allowedOrigins = Sets.newHashSet(allowedOrigins.split(","));
   }

   public void channelRead(ChannelHandlerContext context, Object object) throws Exception {
      String string = this.getHostAddress(context);
      if (object instanceof HttpRequest httpRequest) {
         BearerAuthenticationHandler.Result result = this.authenticate(httpRequest);
         if (!result.isSuccessful()) {
            this.LOGGER.debug("Authentication rejected for connection with ip {}: {}", string, result.getMessage());
            context.channel().attr(AUTHENTICATED_KEY).set(false);
            this.sendUnauthorizedError(context, result.getMessage());
            return;
         }

         context.channel().attr(AUTHENTICATED_KEY).set(true);
         if (result.mustReturnProtocol()) {
            context.channel().attr(WEBSOCKET_AUTH_ALLOWED_KEY).set(Boolean.TRUE);
         }
      }

      Boolean boolean_ = (Boolean)context.channel().attr(AUTHENTICATED_KEY).get();
      if (Boolean.TRUE.equals(boolean_)) {
         super.channelRead(context, object);
      } else {
         this.LOGGER.debug("Dropping unauthenticated connection with ip {}", string);
         context.close();
      }
   }

   public void write(ChannelHandlerContext context, Object value, ChannelPromise promise) throws Exception {
      if (value instanceof HttpResponse httpResponse
         && httpResponse.status().code() == HttpResponseStatus.SWITCHING_PROTOCOLS.code()
         && context.channel().attr(WEBSOCKET_AUTH_ALLOWED_KEY).get() != null
         && ((Boolean)context.channel().attr(WEBSOCKET_AUTH_ALLOWED_KEY).get()).equals(Boolean.TRUE)) {
         httpResponse.headers().set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, "minecraft-v1");
      }

      super.write(context, value, promise);
   }

   private BearerAuthenticationHandler.Result authenticate(HttpRequest request) {
      String string = this.getBearerToken(request);
      if (string != null) {
         return this.tokenMatches(string) ? BearerAuthenticationHandler.Result.success() : BearerAuthenticationHandler.Result.failure("Invalid API key");
      } else {
         String string2 = this.getProtocolToken(request);
         if (string2 != null) {
            if (!this.isOriginAllowed(request)) {
               return BearerAuthenticationHandler.Result.failure("Origin Not Allowed");
            } else {
               return this.tokenMatches(string2)
                  ? BearerAuthenticationHandler.Result.success(true)
                  : BearerAuthenticationHandler.Result.failure("Invalid API key");
            }
         } else {
            return BearerAuthenticationHandler.Result.failure("Missing API key");
         }
      }
   }

   private boolean isOriginAllowed(HttpRequest request) {
      String string = request.headers().get(HttpHeaderNames.ORIGIN);
      return string != null && !string.isEmpty() ? this.allowedOrigins.contains(string) : false;
   }

   private @Nullable String getBearerToken(HttpRequest request) {
      String string = request.headers().get(HttpHeaderNames.AUTHORIZATION);
      return string != null && string.startsWith("Bearer ") ? string.substring("Bearer ".length()).trim() : null;
   }

   private @Nullable String getProtocolToken(HttpRequest request) {
      String string = request.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
      return string != null && string.startsWith("minecraft-v1,") ? string.substring("minecraft-v1,".length()).trim() : null;
   }

   public boolean tokenMatches(String requestToken) {
      if (requestToken.isEmpty()) {
         return false;
      } else {
         byte[] bs = requestToken.getBytes(StandardCharsets.UTF_8);
         byte[] cs = this.token.secretKey().getBytes(StandardCharsets.UTF_8);
         return MessageDigest.isEqual(bs, cs);
      }
   }

   private String getHostAddress(ChannelHandlerContext context) {
      InetSocketAddress inetSocketAddress = (InetSocketAddress)context.channel().remoteAddress();
      return inetSocketAddress.getAddress().getHostAddress();
   }

   private void sendUnauthorizedError(ChannelHandlerContext context, String message) {
      String string = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
      byte[] bs = string.getBytes(StandardCharsets.UTF_8);
      DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(
         HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, Unpooled.wrappedBuffer(bs)
      );
      defaultFullHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
      defaultFullHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, bs.length);
      defaultFullHttpResponse.headers().set(HttpHeaderNames.CONNECTION, "close");
      context.writeAndFlush(defaultFullHttpResponse).addListener(future -> context.close());
   }

   static class Result {
      private final boolean successful;
      private final String message;
      private final boolean mustReturnProtocol;

      private Result(boolean successful, String message, boolean mustReturnProtocol) {
         this.successful = successful;
         this.message = message;
         this.mustReturnProtocol = mustReturnProtocol;
      }

      public static BearerAuthenticationHandler.Result success() {
         return new BearerAuthenticationHandler.Result(true, null, false);
      }

      public static BearerAuthenticationHandler.Result success(boolean mustReturnProtocol) {
         return new BearerAuthenticationHandler.Result(true, null, mustReturnProtocol);
      }

      public static BearerAuthenticationHandler.Result failure(String message) {
         return new BearerAuthenticationHandler.Result(false, message, false);
      }

      public boolean isSuccessful() {
         return this.successful;
      }

      public String getMessage() {
         return this.message;
      }

      public boolean mustReturnProtocol() {
         return this.mustReturnProtocol;
      }
   }
}
