package com.microsoft.aad.msal4j;

import java.util.Objects;

final class ClientSecret implements IClientSecret {
   private final String clientSecret;

   ClientSecret(String clientSecret) {
      if (StringHelper.isBlank(clientSecret)) {
         throw new IllegalArgumentException("clientSecret is null or empty");
      } else {
         this.clientSecret = clientSecret;
      }
   }

   @Override
   public String clientSecret() {
      return this.clientSecret;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof ClientSecret)) {
         return false;
      } else {
         ClientSecret other = (ClientSecret)o;
         return Objects.equals(this.clientSecret, other.clientSecret);
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      return result * 59 + (this.clientSecret == null ? 43 : this.clientSecret.hashCode());
   }
}
