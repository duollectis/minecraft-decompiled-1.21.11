package com.mojang.authlib.yggdrasil;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YggdrasilGameProfileRepository implements GameProfileRepository {
   private static final Logger LOGGER = LoggerFactory.getLogger(YggdrasilGameProfileRepository.class);
   private static final int ENTRIES_PER_PAGE = 2;
   private static final int MAX_FAIL_COUNT = 3;
   private static final int DELAY_BETWEEN_PAGES = 100;
   private static final int DELAY_BETWEEN_FAILURES = 750;
   private final MinecraftClient client;
   private final URL searchPageUrl;
   private final String nameLookupUrl;

   public YggdrasilGameProfileRepository(Proxy proxy, Environment environment) {
      this.client = MinecraftClient.unauthenticated(proxy);
      this.searchPageUrl = HttpAuthenticationService.constantURL(environment.profilesHost() + "/minecraft/profile/lookup/bulk/byname");
      this.nameLookupUrl = environment.profilesHost() + "/minecraft/profile/lookup/name/";
   }

   @Override
   public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
      Set<String> criteria = Arrays.stream(names).filter(namex -> !Strings.isNullOrEmpty(namex)).collect(Collectors.toSet());
      int page = 0;

      for (List<String> request : Iterables.partition(criteria, 2)) {
         List<String> normalizedRequest = request.stream().map(YggdrasilGameProfileRepository::normalizeName).toList();
         int failCount = 0;

         boolean failed;
         do {
            failed = false;

            try {
               ProfileSearchResultsResponse response = this.client.post(this.searchPageUrl, normalizedRequest, ProfileSearchResultsResponse.class);
               List<NameAndId> results = response != null ? response.profiles() : List.of();
               failCount = 0;
               LOGGER.debug("Page {} returned {} results, parsing", 0, results.size());
               Set<String> received = new HashSet<>(results.size());

               for (NameAndId profile : results) {
                  LOGGER.debug("Successfully looked up profile {}", profile);
                  received.add(normalizeName(profile.name()));
                  callback.onProfileLookupSucceeded(profile.name(), profile.id());
               }

               for (String name : request) {
                  if (!received.contains(normalizeName(name))) {
                     LOGGER.debug("Couldn't find profile {}", name);
                     callback.onProfileLookupFailed(name, new ProfileNotFoundException("Server did not find the requested profile"));
                  }
               }

               try {
                  Thread.sleep(100L);
               } catch (InterruptedException var16) {
               }
            } catch (MinecraftClientException var17) {
               MinecraftClientException e = var17;
               if (++failCount == 3) {
                  for (String namex : request) {
                     LOGGER.debug("Couldn't find profile {} because of a server error", namex);
                     callback.onProfileLookupFailed(namex, e.toAuthenticationException());
                  }
               } else {
                  try {
                     Thread.sleep(750L);
                  } catch (InterruptedException var15) {
                  }

                  failed = true;
               }
            }
         } while (failed);
      }
   }

   @Override
   public Optional<NameAndId> findProfileByName(String name) {
      try {
         return Optional.ofNullable(this.client.get(HttpAuthenticationService.constantURL(this.nameLookupUrl + normalizeName(name)), NameAndId.class));
      } catch (MinecraftClientException var3) {
         LOGGER.warn("Couldn't find profile with name: {}", name, var3);
         return Optional.empty();
      }
   }

   private static String normalizeName(String name) {
      return name.toLowerCase(Locale.ROOT);
   }
}
