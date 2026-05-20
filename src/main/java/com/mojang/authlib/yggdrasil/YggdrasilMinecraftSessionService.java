package com.mojang.authlib.yggdrasil;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.ProfileAction;
import com.mojang.util.UUIDTypeAdapter;
import com.mojang.util.UndashedUuid;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YggdrasilMinecraftSessionService implements MinecraftSessionService {
   private static final Logger LOGGER = LoggerFactory.getLogger(YggdrasilMinecraftSessionService.class);
   private final MinecraftClient client;
   private final ServicesKeySet servicesKeySet;
   private final String baseUrl;
   private final URL joinUrl;
   private final URL checkUrl;
   private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
   private final LoadingCache<UUID, Optional<ProfileResult>> insecureProfiles = CacheBuilder.newBuilder()
      .expireAfterWrite(6L, TimeUnit.HOURS)
      .build(new CacheLoader<UUID, Optional<ProfileResult>>() {
         public Optional<ProfileResult> load(UUID key) {
            return Optional.ofNullable(YggdrasilMinecraftSessionService.this.fetchProfileUncached(key, false));
         }
      });

   protected YggdrasilMinecraftSessionService(ServicesKeySet servicesKeySet, Proxy proxy, Environment env) {
      this.client = MinecraftClient.unauthenticated(proxy);
      this.servicesKeySet = servicesKeySet;
      this.baseUrl = env.sessionHost() + "/session/minecraft/";
      this.joinUrl = HttpAuthenticationService.constantURL(this.baseUrl + "join");
      this.checkUrl = HttpAuthenticationService.constantURL(this.baseUrl + "hasJoined");
   }

   @Override
   public void joinServer(UUID profileId, String authenticationToken, String serverId) throws AuthenticationException {
      JoinMinecraftServerRequest request = new JoinMinecraftServerRequest(authenticationToken, profileId, serverId);

      try {
         this.client.post(this.joinUrl, request, Void.class);
      } catch (MinecraftClientException var6) {
         throw var6.toAuthenticationException();
      }
   }

   @Nullable
   @Override
   public ProfileResult hasJoinedServer(String profileName, String serverId, @Nullable InetAddress address) throws AuthenticationUnavailableException {
      Map<String, Object> arguments = new HashMap<>();
      arguments.put("username", profileName);
      arguments.put("serverId", serverId);
      if (address != null) {
         arguments.put("ip", address.getHostAddress());
      }

      URL url = HttpAuthenticationService.concatenateURL(this.checkUrl, HttpAuthenticationService.buildQuery(arguments));

      try {
         HasJoinedMinecraftServerResponse response = this.client.get(url, HasJoinedMinecraftServerResponse.class);
         if (response != null && response.id() != null) {
            GameProfile result = new GameProfile(response.id(), profileName, Objects.requireNonNullElse(response.properties(), PropertyMap.EMPTY));
            Set<ProfileActionType> profileActions = extractProfileActionTypes(response.profileActions());
            return new ProfileResult(result, profileActions);
         } else {
            return null;
         }
      } catch (MinecraftClientException var9) {
         if (var9.toAuthenticationException() instanceof AuthenticationUnavailableException unavailable) {
            throw unavailable;
         } else {
            return null;
         }
      }
   }

   @Nullable
   @Override
   public Property getPackedTextures(GameProfile profile) {
      return (Property)Iterables.getFirst(profile.properties().get("textures"), null);
   }

   @Override
   public MinecraftProfileTextures unpackTextures(Property packedTextures) {
      String value = packedTextures.value();
      SignatureState signatureState = this.getPropertySignatureState(packedTextures);

      MinecraftTexturesPayload result;
      try {
         String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
         result = (MinecraftTexturesPayload)this.gson.fromJson(json, MinecraftTexturesPayload.class);
      } catch (IllegalArgumentException | JsonParseException var9) {
         LOGGER.error("Could not decode textures payload", var9);
         return MinecraftProfileTextures.EMPTY;
      }

      if (result != null && result.textures() != null && !result.textures().isEmpty()) {
         Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = result.textures();

         for (Entry<MinecraftProfileTexture.Type, MinecraftProfileTexture> entry : textures.entrySet()) {
            String url = entry.getValue().getUrl();
            if (url == null || !TextureUrlChecker.isAllowedTextureDomain(url)) {
               LOGGER.error("Textures payload url is invalid: {}", url);
               return MinecraftProfileTextures.EMPTY;
            }
         }

         return new MinecraftProfileTextures(
            textures.get(MinecraftProfileTexture.Type.SKIN),
            textures.get(MinecraftProfileTexture.Type.CAPE),
            textures.get(MinecraftProfileTexture.Type.ELYTRA),
            signatureState
         );
      } else {
         return MinecraftProfileTextures.EMPTY;
      }
   }

   @Nullable
   @Override
   public ProfileResult fetchProfile(UUID profileId, boolean requireSecure) {
      return !requireSecure
         ? (ProfileResult)((Optional)this.insecureProfiles.getUnchecked(profileId)).orElse(null)
         : this.fetchProfileUncached(profileId, true);
   }

   @Override
   public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException {
      switch (this.getPropertySignatureState(property)) {
         case UNSIGNED:
            throw new InsecurePublicKeyException.MissingException("Missing signature from \"" + property.name() + "\"");
         case INVALID:
            throw new InsecurePublicKeyException.InvalidException("Property \"" + property.name() + "\" has been tampered with (signature invalid)");
         case SIGNED:
            return property.value();
         default:
            throw new IncompatibleClassChangeError();
      }
   }

   private SignatureState getPropertySignatureState(Property property) {
      if (!property.hasSignature()) {
         return SignatureState.UNSIGNED;
      } else {
         return this.servicesKeySet.keys(ServicesKeyType.PROFILE_PROPERTY).stream().noneMatch(key -> key.validateProperty(property))
            ? SignatureState.INVALID
            : SignatureState.SIGNED;
      }
   }

   @Nullable
   private ProfileResult fetchProfileUncached(UUID profileId, boolean requireSecure) {
      try {
         URL url = HttpAuthenticationService.constantURL(this.baseUrl + "profile/" + UndashedUuid.toString(profileId));
         url = HttpAuthenticationService.concatenateURL(url, "unsigned=" + !requireSecure);
         MinecraftProfilePropertiesResponse response = this.client.get(url, MinecraftProfilePropertiesResponse.class);
         if (response == null) {
            LOGGER.debug("Couldn't fetch profile properties for {} as the profile does not exist", profileId);
            return null;
         } else {
            GameProfile profile = response.profile();
            Set<ProfileActionType> profileActions = extractProfileActionTypes(response.profileActions());
            LOGGER.debug("Successfully fetched profile properties for {}", profile);
            return new ProfileResult(profile, profileActions);
         }
      } catch (IllegalArgumentException | MinecraftClientException var7) {
         LOGGER.warn("Couldn't look up profile properties for {}", profileId, var7);
         return null;
      }
   }

   private static Set<ProfileActionType> extractProfileActionTypes(Set<ProfileAction> response) {
      return response.stream().map(ProfileAction::type).collect(Collectors.toSet());
   }
}
