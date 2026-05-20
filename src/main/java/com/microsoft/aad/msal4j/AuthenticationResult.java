package com.microsoft.aad.msal4j;

import java.util.Date;
import java.util.Objects;

final class AuthenticationResult implements IAuthenticationResult {
   private static final long serialVersionUID = 1L;
   private final String accessToken;
   private final long expiresOn;
   private final long extExpiresOn;
   private final String refreshToken;
   private final Long refreshOn;
   private final String familyId;
   private final String idToken;
   private final IdToken idTokenObject = this.getIdTokenObj();
   private final AccountCacheEntity accountCacheEntity;
   private final IAccount account = this.getAccount();
   private final ITenantProfile tenantProfile = this.getTenantProfile();
   private String environment;
   private final Date expiresOnDate;
   private final String scopes;
   private final AuthenticationResultMetadata metadata;
   private final Boolean isPopAuthorization;

   AuthenticationResult(
      String accessToken,
      long expiresOn,
      long extExpiresOn,
      String refreshToken,
      Long refreshOn,
      String familyId,
      String idToken,
      AccountCacheEntity accountCacheEntity,
      String environment,
      String scopes,
      AuthenticationResultMetadata metadata,
      Boolean isPopAuthorization
   ) {
      this.accessToken = accessToken;
      this.expiresOn = expiresOn;
      this.extExpiresOn = extExpiresOn;
      this.refreshToken = refreshToken;
      this.refreshOn = refreshOn;
      this.familyId = familyId;
      this.idToken = idToken;
      this.accountCacheEntity = accountCacheEntity;
      this.environment = environment;
      this.scopes = scopes;
      this.metadata = metadata == null ? AuthenticationResultMetadata.builder().build() : metadata;
      this.isPopAuthorization = isPopAuthorization;
      this.expiresOnDate = new Date(expiresOn * 1000L);
   }

   private IdToken getIdTokenObj() {
      return StringHelper.isBlank(this.idToken) ? null : JsonHelper.createIdTokenFromEncodedTokenString(this.idToken);
   }

   private IAccount getAccount() {
      return this.accountCacheEntity == null ? null : this.accountCacheEntity.toAccount();
   }

   private ITenantProfile getTenantProfile() {
      return StringHelper.isBlank(this.idToken)
         ? null
         : new TenantProfile(JsonHelper.parseJsonToMap(JsonHelper.getTokenPayloadClaims(this.idToken)), this.getAccount().environment());
   }

   @Override
   public String accessToken() {
      return this.accessToken;
   }

   String refreshToken() {
      return this.refreshToken;
   }

   Long refreshOn() {
      return this.refreshOn;
   }

   @Override
   public String idToken() {
      return this.idToken;
   }

   @Override
   public String environment() {
      return this.environment;
   }

   @Override
   public String scopes() {
      return this.scopes;
   }

   @Override
   public AuthenticationResultMetadata metadata() {
      return this.metadata;
   }

   long expiresOn() {
      return this.expiresOn;
   }

   long extExpiresOn() {
      return this.extExpiresOn;
   }

   String familyId() {
      return this.familyId;
   }

   IdToken idTokenObject() {
      return this.idTokenObject;
   }

   AccountCacheEntity accountCacheEntity() {
      return this.accountCacheEntity;
   }

   @Override
   public IAccount account() {
      return this.getAccount();
   }

   @Override
   public ITenantProfile tenantProfile() {
      return this.tenantProfile;
   }

   @Override
   public Date expiresOnDate() {
      return this.expiresOnDate;
   }

   Boolean isPopAuthorization() {
      return this.isPopAuthorization;
   }

   static AuthenticationResult.AuthenticationResultBuilder builder() {
      return new AuthenticationResult.AuthenticationResultBuilder();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof AuthenticationResult)) {
         return false;
      } else {
         AuthenticationResult other = (AuthenticationResult)o;
         if (this.expiresOn() != other.expiresOn()) {
            return false;
         } else if (this.extExpiresOn() != other.extExpiresOn()) {
            return false;
         } else if (!Objects.equals(this.refreshOn, other.refreshOn)) {
            return false;
         } else if (!Objects.equals(this.isPopAuthorization, other.isPopAuthorization)) {
            return false;
         } else if (!Objects.equals(this.accessToken, other.accessToken)) {
            return false;
         } else if (!Objects.equals(this.refreshToken, other.refreshToken)) {
            return false;
         } else if (!Objects.equals(this.familyId, other.familyId)) {
            return false;
         } else if (!Objects.equals(this.idToken, other.idToken)) {
            return false;
         } else if (!Objects.equals(this.idTokenObject, other.idTokenObject)) {
            return false;
         } else if (!Objects.equals(this.accountCacheEntity, other.accountCacheEntity)) {
            return false;
         } else if (!Objects.equals(this.account, other.account)) {
            return false;
         } else if (!Objects.equals(this.tenantProfile, other.tenantProfile)) {
            return false;
         } else if (!Objects.equals(this.environment, other.environment)) {
            return false;
         } else if (!Objects.equals(this.expiresOnDate, other.expiresOnDate)) {
            return false;
         } else {
            return !Objects.equals(this.scopes, other.scopes) ? false : Objects.equals(this.metadata, other.metadata);
         }
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = result * 59 + (int)(this.expiresOn >>> 32 ^ this.expiresOn);
      result = result * 59 + (int)(this.extExpiresOn >>> 32 ^ this.extExpiresOn);
      result = result * 59 + (this.refreshOn == null ? 43 : this.refreshOn.hashCode());
      result = result * 59 + (this.isPopAuthorization == null ? 43 : this.isPopAuthorization.hashCode());
      result = result * 59 + (this.accessToken == null ? 43 : this.accessToken.hashCode());
      result = result * 59 + (this.refreshToken == null ? 43 : this.refreshToken.hashCode());
      result = result * 59 + (this.familyId == null ? 43 : this.familyId.hashCode());
      result = result * 59 + (this.idToken == null ? 43 : this.idToken.hashCode());
      result = result * 59 + (this.idTokenObject == null ? 43 : this.idTokenObject.hashCode());
      result = result * 59 + (this.accountCacheEntity == null ? 43 : this.accountCacheEntity.hashCode());
      result = result * 59 + (this.account == null ? 43 : this.account.hashCode());
      result = result * 59 + (this.tenantProfile == null ? 43 : this.tenantProfile.hashCode());
      result = result * 59 + (this.environment == null ? 43 : this.environment.hashCode());
      result = result * 59 + (this.expiresOnDate == null ? 43 : this.expiresOnDate.hashCode());
      result = result * 59 + (this.scopes == null ? 43 : this.scopes.hashCode());
      return result * 59 + (this.metadata == null ? 43 : this.metadata.hashCode());
   }

   static class AuthenticationResultBuilder {
      private String accessToken;
      private long expiresOn;
      private long extExpiresOn;
      private String refreshToken;
      private Long refreshOn;
      private String familyId;
      private String idToken;
      private AccountCacheEntity accountCacheEntity;
      private String environment;
      private String scopes;
      private AuthenticationResultMetadata metadata;
      private Boolean isPopAuthorization;

      public AuthenticationResult.AuthenticationResultBuilder accessToken(String accessToken) {
         this.accessToken = accessToken;
         return this;
      }

      public AuthenticationResult.AuthenticationResultBuilder expiresOn(long expiresOn) {
         this.expiresOn = expiresOn;
         return this;
      }

      public AuthenticationResult.AuthenticationResultBuilder extExpiresOn(long extExpiresOn) {
         this.extExpiresOn = extExpiresOn;
         return this;
      }

      public AuthenticationResult.AuthenticationResultBuilder refreshToken(String refreshToken) {
         this.refreshToken = refreshToken;
         return this;
      }

      public AuthenticationResult.AuthenticationResultBuilder refreshOn(Long refreshOn) {
         this.refreshOn = refreshOn;
         return this;
      }

      public AuthenticationResult.AuthenticationResultBuilder familyId(String familyId) {
         this.familyId = familyId;
         return this;
      }

      public AuthenticationResult.AuthenticationResultBuilder idToken(String idToken) {
         this.idToken = idToken;
         return this;
      }

      public AuthenticationResult.AuthenticationResultBuilder accountCacheEntity(AccountCacheEntity accountCacheEntity) {
         this.accountCacheEntity = accountCacheEntity;
         return this;
      }

      public AuthenticationResult.AuthenticationResultBuilder environment(String environment) {
         this.environment = environment;
         return this;
      }

      public AuthenticationResult.AuthenticationResultBuilder scopes(String scopes) {
         this.scopes = scopes;
         return this;
      }

      public AuthenticationResult.AuthenticationResultBuilder metadata(AuthenticationResultMetadata metadata) {
         this.metadata = metadata;
         return this;
      }

      public AuthenticationResult.AuthenticationResultBuilder isPopAuthorization(Boolean isPopAuthorization) {
         this.isPopAuthorization = isPopAuthorization;
         return this;
      }

      public AuthenticationResult build() {
         return new AuthenticationResult(
            this.accessToken,
            this.expiresOn,
            this.extExpiresOn,
            this.refreshToken,
            this.refreshOn,
            this.familyId,
            this.idToken,
            this.accountCacheEntity,
            this.environment,
            this.scopes,
            this.metadata,
            this.isPopAuthorization
         );
      }

      @Override
      public String toString() {
         return "AuthenticationResult.AuthenticationResultBuilder(accessToken="
            + this.accessToken
            + ", expiresOn="
            + this.expiresOn
            + ", extExpiresOn="
            + this.extExpiresOn
            + ", refreshToken="
            + this.refreshToken
            + ", refreshOn="
            + this.refreshOn
            + ", familyId="
            + this.familyId
            + ", idToken="
            + this.idToken
            + ", accountCacheEntity="
            + this.accountCacheEntity
            + ", environment="
            + this.environment
            + ", scopes="
            + this.scopes
            + ", metadata="
            + this.metadata
            + ", isPopAuthorization="
            + this.isPopAuthorization
            + ")";
      }
   }
}
