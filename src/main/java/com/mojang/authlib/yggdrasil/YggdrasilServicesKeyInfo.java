package com.mojang.authlib.yggdrasil;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YggdrasilServicesKeyInfo implements ServicesKeyInfo {
   private static final Logger LOGGER = LoggerFactory.getLogger(YggdrasilServicesKeyInfo.class);
   private static final ScheduledExecutorService FETCHER_EXECUTOR = Executors.newScheduledThreadPool(
      1, new ThreadFactoryBuilder().setNameFormat("Yggdrasil Key Fetcher").setDaemon(true).build()
   );
   private static final int KEY_SIZE_BITS = 4096;
   private static final String KEY_ALGORITHM = "RSA";
   private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
   private static final int REFRESH_INTERVAL_HOURS = 24;
   private static final int BASE_FAILURE_INTERVAL_MINUTES = 5;
   private static final int MAX_BACKOFF_EXPONENT = 6;
   private final PublicKey publicKey;

   private YggdrasilServicesKeyInfo(PublicKey publicKey) {
      this.publicKey = publicKey;
      String algorithm = publicKey.getAlgorithm();
      if (!algorithm.equals("RSA")) {
         throw new IllegalArgumentException("Expected RSA key, got " + algorithm);
      }
   }

   public static ServicesKeyInfo parse(byte[] keyBytes) {
      try {
         X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
         KeyFactory keyFactory = KeyFactory.getInstance("RSA");
         PublicKey publicKey = keyFactory.generatePublic(spec);
         return new YggdrasilServicesKeyInfo(publicKey);
      } catch (InvalidKeySpecException | NoSuchAlgorithmException var4) {
         throw new IllegalArgumentException("Invalid yggdrasil public key!", var4);
      }
   }

   private static List<ServicesKeyInfo> parseList(@Nullable List<YggdrasilServicesKeyInfo.KeyData> keys) {
      return keys == null ? List.of() : keys.stream().map(data -> parse(data.publicKey.array())).toList();
   }

   public static ServicesKeySet get(final URL url, final MinecraftClient client) {
      final CompletableFuture<?> ready = new CompletableFuture();
      final AtomicReference<ServicesKeySet> keySet = new AtomicReference<>();
      FETCHER_EXECUTOR.execute(new Runnable() {
         private final AtomicInteger failureCount = new AtomicInteger();

         @Override
         public void run() {
            YggdrasilServicesKeyInfo.fetch(url, client).ifPresent(keySet::set);
            ready.complete(null);
            this.reschedule();
         }

         private void reschedule() {
            if (keySet.get() == null) {
               int backoffExponent = Math.min(this.failureCount.getAndIncrement(), 6);
               int delayMinutes = 5 * (1 << backoffExponent);
               YggdrasilServicesKeyInfo.FETCHER_EXECUTOR.schedule(this, (long)delayMinutes, TimeUnit.MINUTES);
            } else {
               YggdrasilServicesKeyInfo.FETCHER_EXECUTOR.schedule(this, 24L, TimeUnit.HOURS);
            }
         }
      });
      return ServicesKeySet.lazy(() -> {
         ready.join();
         return Objects.requireNonNullElse(keySet.get(), ServicesKeySet.EMPTY);
      });
   }

   private static Optional<ServicesKeySet> fetch(URL url, MinecraftClient client) {
      YggdrasilServicesKeyInfo.KeySetResponse response;
      try {
         response = client.get(url, YggdrasilServicesKeyInfo.KeySetResponse.class);
      } catch (MinecraftClientException var6) {
         LOGGER.error("Failed to request yggdrasil public key", var6);
         return Optional.empty();
      }

      if (response == null) {
         return Optional.empty();
      } else {
         try {
            List<ServicesKeyInfo> profilePropertyKeys = parseList(response.profilePropertyKeys);
            List<ServicesKeyInfo> playerCertificateKeys = parseList(response.playerCertificateKeys);
            return Optional.of(type -> {
               return switch (type) {
                  case PROFILE_PROPERTY -> profilePropertyKeys;
                  case PROFILE_KEY -> playerCertificateKeys;
               };
            });
         } catch (Exception var5) {
            LOGGER.error("Received malformed yggdrasil public key data", var5);
            return Optional.empty();
         }
      }
   }

   @Override
   public Signature signature() {
      try {
         Signature signature = Signature.getInstance("SHA1withRSA");
         signature.initVerify(this.publicKey);
         return signature;
      } catch (InvalidKeyException | NoSuchAlgorithmException var2) {
         throw new AssertionError("Failed to create signature", var2);
      }
   }

   @Override
   public int keyBitCount() {
      return 4096;
   }

   @Override
   public boolean validateProperty(Property property) {
      Signature signature = this.signature();

      byte[] expected;
      try {
         expected = Base64.getDecoder().decode(property.signature());
      } catch (IllegalArgumentException var6) {
         LOGGER.error("Malformed signature encoding on property {}", property, var6);
         return false;
      }

      try {
         signature.update(property.value().getBytes());
         return signature.verify(expected);
      } catch (SignatureException var5) {
         LOGGER.error("Failed to verify signature on property {}", property, var5);
         return false;
      }
   }

   private record KeyData(@SerializedName("publicKey") ByteBuffer publicKey) {
   }

   private record KeySetResponse(
      @Nullable @SerializedName("profilePropertyKeys") List<YggdrasilServicesKeyInfo.KeyData> profilePropertyKeys,
      @Nullable @SerializedName("playerCertificateKeys") List<YggdrasilServicesKeyInfo.KeyData> playerCertificateKeys
   ) {
   }
}
