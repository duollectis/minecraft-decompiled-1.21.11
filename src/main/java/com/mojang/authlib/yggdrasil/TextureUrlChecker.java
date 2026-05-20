package com.mojang.authlib.yggdrasil;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TextureUrlChecker {
   private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
   private static final List<String> ALLOWED_DOMAINS = List.of(".minecraft.net", ".mojang.com");
   private static final List<String> BLOCKED_DOMAINS = List.of("bugs.mojang.com", "education.minecraft.net", "feedback.minecraft.net");

   public static boolean isAllowedTextureDomain(String url) {
      URI uri;
      try {
         uri = new URI(url).normalize();
      } catch (URISyntaxException var6) {
         return false;
      }

      String scheme = uri.getScheme();
      if (scheme != null && ALLOWED_SCHEMES.contains(scheme)) {
         String domain = uri.getHost();
         if (domain == null) {
            return false;
         } else {
            String decodedDomain = IDN.toUnicode(domain);
            String lowerCaseDomain = decodedDomain.toLowerCase(Locale.ROOT);
            return !lowerCaseDomain.equals(decodedDomain)
               ? false
               : isDomainOnList(decodedDomain, ALLOWED_DOMAINS) && !isDomainOnList(decodedDomain, BLOCKED_DOMAINS);
         }
      } else {
         return false;
      }
   }

   private static boolean isDomainOnList(String domain, List<String> list) {
      for (String entry : list) {
         if (domain.endsWith(entry)) {
            return true;
         }
      }

      return false;
   }
}
