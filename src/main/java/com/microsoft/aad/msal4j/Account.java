package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Objects;

class Account implements IAccount {
   String homeAccountId;
   String environment;
   String username;
   Map<String, ITenantProfile> tenantProfiles;

   Account(String homeAccountId, String environment, String username, Map<String, ITenantProfile> tenantProfiles) {
      this.homeAccountId = homeAccountId;
      this.environment = environment;
      this.username = username;
      this.tenantProfiles = tenantProfiles;
   }

   @Override
   public Map<String, ITenantProfile> getTenantProfiles() {
      return this.tenantProfiles;
   }

   @Override
   public String homeAccountId() {
      return this.homeAccountId;
   }

   @Override
   public String environment() {
      return this.environment;
   }

   @Override
   public String username() {
      return this.username;
   }

   void username(String username) {
      this.username = username;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof Account)) {
         return false;
      } else {
         Account other = (Account)o;
         return Objects.equals(this.homeAccountId(), other.homeAccountId());
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      return result * 59 + (this.homeAccountId == null ? 43 : this.homeAccountId.hashCode());
   }
}
