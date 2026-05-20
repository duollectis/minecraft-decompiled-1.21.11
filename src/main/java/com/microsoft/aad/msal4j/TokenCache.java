package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import com.azure.json.ReadValueCallback;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TokenCache implements ITokenCache {
   protected static final int MIN_ACCESS_TOKEN_EXPIRE_IN_SEC = 300;
   private ReadWriteLock lock = new ReentrantReadWriteLock();
   Map<String, AccessTokenCacheEntity> accessTokens = new LinkedHashMap<>();
   Map<String, RefreshTokenCacheEntity> refreshTokens = new LinkedHashMap<>();
   Map<String, IdTokenCacheEntity> idTokens = new LinkedHashMap<>();
   Map<String, AccountCacheEntity> accounts = new LinkedHashMap<>();
   Map<String, AppMetadataCacheEntity> appMetadata = new LinkedHashMap<>();
   ITokenCacheAccessAspect tokenCacheAccessAspect;
   private String serializedCachedSnapshot;

   public TokenCache(ITokenCacheAccessAspect tokenCacheAccessAspect) {
      this();
      this.tokenCacheAccessAspect = tokenCacheAccessAspect;
   }

   public TokenCache() {
   }

   @Override
   public void deserialize(String data) {
      if (!StringHelper.isBlank(data)) {
         this.serializedCachedSnapshot = data;

         try {
            JsonReader jsonReader = JsonProviders.createReader(data);
            this.deserializeFromJson(jsonReader);
         } catch (IOException var3) {
            throw new MsalClientException(var3);
         }
      }
   }

   private void deserializeFromJson(JsonReader jsonReader) throws IOException {
      this.lock.writeLock().lock();

      try {
         jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
               String fieldName = reader.getFieldName();
               reader.nextToken();
               switch (fieldName) {
                  case "AccessToken":
                     this.deserializeCollection(reader, this.accessTokens, AccessTokenCacheEntity::fromJson);
                     break;
                  case "RefreshToken":
                     this.deserializeCollection(reader, this.refreshTokens, RefreshTokenCacheEntity::fromJson);
                     break;
                  case "IdToken":
                     this.deserializeCollection(reader, this.idTokens, IdTokenCacheEntity::fromJson);
                     break;
                  case "Account":
                     this.deserializeCollection(reader, this.accounts, AccountCacheEntity::fromJson);
                     break;
                  case "AppMetadata":
                     this.deserializeCollection(reader, this.appMetadata, AppMetadataCacheEntity::fromJson);
                     break;
                  default:
                     reader.skipChildren();
               }
            }

            return null;
         });
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   private <T> void deserializeCollection(JsonReader reader, Map<String, T> targetCollection, ReadValueCallback<JsonReader, T> deserializer) throws IOException {
      reader.readObject(entityReader -> {
         while (entityReader.nextToken() != JsonToken.END_OBJECT) {
            String key = entityReader.getFieldName();
            entityReader.nextToken();
            T entity = (T)deserializer.read(entityReader);
            targetCollection.put(key, entity);
         }

         return null;
      });
   }

   @Override
   public String serialize() {
      this.lock.readLock().lock();

      try {
         if (!StringHelper.isBlank(this.serializedCachedSnapshot)) {
            String updatedCache = this.mergeWithExistingCache();
            if (updatedCache != null) {
               return updatedCache;
            }
         }

         return this.serializeToJson();
      } finally {
         this.lock.readLock().unlock();
      }
   }

   private String serializeToJson() {
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
         JsonWriter jsonWriter = JsonProviders.createWriter(outputStream);
         Throwable var4 = null;

         String var5;
         try {
            jsonWriter.writeStartObject();
            this.writeCollection(jsonWriter, "AccessToken", this.accessTokens);
            this.writeCollection(jsonWriter, "RefreshToken", this.refreshTokens);
            this.writeCollection(jsonWriter, "IdToken", this.idTokens);
            this.writeCollection(jsonWriter, "Account", this.accounts);
            this.writeCollection(jsonWriter, "AppMetadata", this.appMetadata);
            jsonWriter.writeEndObject();
            jsonWriter.flush();
            var5 = outputStream.toString(StandardCharsets.UTF_8.name());
         } catch (Throwable var30) {
            var4 = var30;
            throw var30;
         } finally {
            if (jsonWriter != null) {
               if (var4 != null) {
                  try {
                     jsonWriter.close();
                  } catch (Throwable var29) {
                     var4.addSuppressed(var29);
                  }
               } else {
                  jsonWriter.close();
               }
            }
         }

         return var5;
      } catch (IOException var34) {
         throw new MsalClientException(var34);
      }
   }

   private <T> void writeCollection(JsonWriter jsonWriter, String collectionName, Map<String, T> collection) throws IOException {
      jsonWriter.writeFieldName(collectionName);
      jsonWriter.writeStartObject();

      for (Entry<String, T> entry : collection.entrySet()) {
         jsonWriter.writeFieldName(entry.getKey());
         if (entry.getValue() instanceof JsonSerializable) {
            ((JsonSerializable)entry.getValue()).toJson(jsonWriter);
         }
      }

      jsonWriter.writeEndObject();
   }

   private String mergeWithExistingCache() {
      try {
         TokenCache updatedCache = new TokenCache();
         updatedCache.deserialize(this.serializedCachedSnapshot);
         this.mergeCache(updatedCache);
         return updatedCache.serializeToJson();
      } catch (Exception var2) {
         return null;
      }
   }

   private void mergeCache(TokenCache targetCache) {
      targetCache.accessTokens.putAll(this.accessTokens);
      targetCache.refreshTokens.putAll(this.refreshTokens);
      targetCache.idTokens.putAll(this.idTokens);
      targetCache.accounts.putAll(this.accounts);
      targetCache.appMetadata.putAll(this.appMetadata);
      targetCache.accessTokens.keySet().retainAll(this.accessTokens.keySet());
      targetCache.refreshTokens.keySet().retainAll(this.refreshTokens.keySet());
      targetCache.idTokens.keySet().retainAll(this.idTokens.keySet());
      targetCache.accounts.keySet().retainAll(this.accounts.keySet());
      targetCache.appMetadata.keySet().retainAll(this.appMetadata.keySet());
   }

   void saveTokens(TokenRequestExecutor tokenRequestExecutor, AuthenticationResult authenticationResult, String environment) {
      try (TokenCache.CacheAspect cacheAspect = new TokenCache.CacheAspect(
            TokenCacheAccessContext.builder()
               .clientId(tokenRequestExecutor.getMsalRequest().application().clientId())
               .tokenCache(this)
               .hasCacheChanged(true)
               .build()
         )) {
         try {
            this.lock.writeLock().lock();
            if (!StringHelper.isBlank(authenticationResult.accessToken())) {
               AccessTokenCacheEntity atEntity = createAccessTokenCacheEntity(tokenRequestExecutor, authenticationResult, environment);
               this.accessTokens.put(atEntity.getKey(), atEntity);
            }

            if (!StringHelper.isBlank(authenticationResult.familyId())) {
               AppMetadataCacheEntity appMetadataCacheEntity = createAppMetadataCacheEntity(tokenRequestExecutor, authenticationResult, environment);
               this.appMetadata.put(appMetadataCacheEntity.getKey(), appMetadataCacheEntity);
            }

            if (!StringHelper.isBlank(authenticationResult.refreshToken())) {
               RefreshTokenCacheEntity rtEntity = createRefreshTokenCacheEntity(tokenRequestExecutor, authenticationResult, environment);
               rtEntity.family_id(authenticationResult.familyId());
               this.refreshTokens.put(rtEntity.getKey(), rtEntity);
            }

            if (!StringHelper.isBlank(authenticationResult.idToken())) {
               IdTokenCacheEntity idTokenEntity = createIdTokenCacheEntity(tokenRequestExecutor, authenticationResult, environment);
               this.idTokens.put(idTokenEntity.getKey(), idTokenEntity);
               AccountCacheEntity accountCacheEntity = authenticationResult.accountCacheEntity();
               if (accountCacheEntity != null) {
                  accountCacheEntity.environment(environment);
                  this.accounts.put(accountCacheEntity.getKey(), accountCacheEntity);
               }
            }
         } finally {
            this.lock.writeLock().unlock();
         }
      }
   }

   private static RefreshTokenCacheEntity createRefreshTokenCacheEntity(
      TokenRequestExecutor tokenRequestExecutor, AuthenticationResult authenticationResult, String environmentAlias
   ) {
      RefreshTokenCacheEntity rt = new RefreshTokenCacheEntity();
      rt.credentialType(CredentialTypeEnum.REFRESH_TOKEN.value());
      if (authenticationResult.account() != null) {
         rt.homeAccountId(authenticationResult.account().homeAccountId());
      }

      rt.environment(environmentAlias);
      rt.clientId(tokenRequestExecutor.getMsalRequest().application().clientId());
      rt.secret(authenticationResult.refreshToken());
      if (tokenRequestExecutor.getMsalRequest() instanceof OnBehalfOfRequest) {
         OnBehalfOfRequest onBehalfOfRequest = (OnBehalfOfRequest)tokenRequestExecutor.getMsalRequest();
         rt.userAssertionHash(onBehalfOfRequest.parameters.userAssertion().getAssertionHash());
      }

      return rt;
   }

   private static AccessTokenCacheEntity createAccessTokenCacheEntity(
      TokenRequestExecutor tokenRequestExecutor, AuthenticationResult authenticationResult, String environmentAlias
   ) {
      AccessTokenCacheEntity at = new AccessTokenCacheEntity();
      at.credentialType(CredentialTypeEnum.ACCESS_TOKEN.value());
      if (authenticationResult.account() != null) {
         at.homeAccountId(authenticationResult.account().homeAccountId());
      }

      at.environment(environmentAlias);
      at.clientId(tokenRequestExecutor.getMsalRequest().application().clientId());
      at.secret(authenticationResult.accessToken());
      at.realm(tokenRequestExecutor.tenant);
      String scopes = !StringHelper.isBlank(authenticationResult.scopes())
         ? authenticationResult.scopes()
         : String.join(" ", tokenRequestExecutor.getMsalRequest().msalAuthorizationGrant().getScopes());
      at.target(scopes);
      if (tokenRequestExecutor.getMsalRequest() instanceof OnBehalfOfRequest) {
         OnBehalfOfRequest onBehalfOfRequest = (OnBehalfOfRequest)tokenRequestExecutor.getMsalRequest();
         at.userAssertionHash(onBehalfOfRequest.parameters.userAssertion().getAssertionHash());
      }

      long currTimestampSec = System.currentTimeMillis() / 1000L;
      at.cachedAt(Long.toString(currTimestampSec));
      at.expiresOn(Long.toString(authenticationResult.expiresOn()));
      if (authenticationResult.refreshOn() > 0L) {
         at.refreshOn(Long.toString(authenticationResult.refreshOn()));
      }

      if (authenticationResult.extExpiresOn() > 0L) {
         at.extExpiresOn(Long.toString(authenticationResult.extExpiresOn()));
      }

      return at;
   }

   private static IdTokenCacheEntity createIdTokenCacheEntity(
      TokenRequestExecutor tokenRequestExecutor, AuthenticationResult authenticationResult, String environmentAlias
   ) {
      IdTokenCacheEntity idToken = new IdTokenCacheEntity();
      idToken.credentialType(CredentialTypeEnum.ID_TOKEN.value());
      if (authenticationResult.account() != null) {
         idToken.homeAccountId(authenticationResult.account().homeAccountId());
      }

      idToken.environment(environmentAlias);
      idToken.clientId(tokenRequestExecutor.getMsalRequest().application().clientId());
      idToken.secret(authenticationResult.idToken());
      idToken.realm(tokenRequestExecutor.tenant);
      if (tokenRequestExecutor.getMsalRequest() instanceof OnBehalfOfRequest) {
         OnBehalfOfRequest onBehalfOfRequest = (OnBehalfOfRequest)tokenRequestExecutor.getMsalRequest();
         idToken.userAssertionHash(onBehalfOfRequest.parameters.userAssertion().getAssertionHash());
      }

      return idToken;
   }

   private static AppMetadataCacheEntity createAppMetadataCacheEntity(
      TokenRequestExecutor tokenRequestExecutor, AuthenticationResult authenticationResult, String environmentAlias
   ) {
      AppMetadataCacheEntity appMetadataCacheEntity = new AppMetadataCacheEntity();
      appMetadataCacheEntity.clientId(tokenRequestExecutor.getMsalRequest().application().clientId());
      appMetadataCacheEntity.environment(environmentAlias);
      appMetadataCacheEntity.familyId(authenticationResult.familyId());
      return appMetadataCacheEntity;
   }

   Set<IAccount> getAccounts(String clientId) {
      HashSet var26;
      try (TokenCache.CacheAspect cacheAspect = new TokenCache.CacheAspect(TokenCacheAccessContext.builder().clientId(clientId).tokenCache(this).build())) {
         try {
            this.lock.readLock().lock();
            Map<String, IAccount> rootAccounts = new HashMap<>();

            for (AccountCacheEntity accCached : this.accounts.values()) {
               IdTokenCacheEntity idToken = this.idTokens
                  .get(this.getIdTokenKey(accCached.homeAccountId(), accCached.environment(), clientId, accCached.realm()));
               ITenantProfile profile = null;
               if (idToken != null) {
                  Map<String, ?> idTokenClaims = JsonHelper.parseJsonToMap(JsonHelper.getTokenPayloadClaims(idToken.secret));
                  profile = new TenantProfile(idTokenClaims, accCached.environment());
               }

               if (rootAccounts.get(accCached.homeAccountId()) == null) {
                  IAccount acc = accCached.toAccount();
                  ((Account)acc).tenantProfiles = new HashMap<>();
                  rootAccounts.put(accCached.homeAccountId(), acc);
               }

               if (profile != null) {
                  ((Account)rootAccounts.get(accCached.homeAccountId())).tenantProfiles.put(accCached.realm(), profile);
               }

               if (accCached.localAccountId() != null && accCached.homeAccountId().contains(accCached.localAccountId())) {
                  ((Account)rootAccounts.get(accCached.homeAccountId())).username(accCached.username());
               }
            }

            var26 = new HashSet<>(rootAccounts.values());
         } finally {
            this.lock.readLock().unlock();
         }
      }

      return var26;
   }

   private String getIdTokenKey(String homeAccountId, String environment, String clientId, String realm) {
      return String.join("-", Arrays.asList(homeAccountId, environment, "idtoken", clientId, realm, "")).toLowerCase();
   }

   private String getApplicationFamilyId(String clientId, Set<String> environmentAliases) {
      for (AppMetadataCacheEntity data : this.appMetadata.values()) {
         if (data.clientId().equals(clientId) && environmentAliases.contains(data.environment()) && !StringHelper.isBlank(data.familyId())) {
            return data.familyId();
         }
      }

      return null;
   }

   private Set<String> getFamilyClientIds(String familyId, Set<String> environmentAliases) {
      return this.appMetadata
         .values()
         .stream()
         .filter(appMetadata -> environmentAliases.contains(appMetadata.environment()) && familyId.equals(appMetadata.familyId()))
         .map(AppMetadataCacheEntity::clientId)
         .collect(Collectors.toSet());
   }

   void removeAccount(String clientId, IAccount account) {
      try (TokenCache.CacheAspect cacheAspect = new TokenCache.CacheAspect(
            TokenCacheAccessContext.builder().clientId(clientId).tokenCache(this).hasCacheChanged(true).build()
         )) {
         try {
            this.lock.writeLock().lock();
            this.removeAccount(account);
         } finally {
            this.lock.writeLock().unlock();
         }
      }
   }

   private void removeAccount(IAccount account) {
      Predicate<Entry<String, ? extends Credential>> credentialToRemovePredicate = e -> !StringHelper.isBlank(e.getValue().homeAccountId())
         && !StringHelper.isBlank(e.getValue().environment())
         && e.getValue().homeAccountId().equals(account.homeAccountId());
      this.accessTokens.entrySet().removeIf(credentialToRemovePredicate);
      this.refreshTokens.entrySet().removeIf(credentialToRemovePredicate);
      this.idTokens.entrySet().removeIf(credentialToRemovePredicate);
      this.accounts
         .entrySet()
         .removeIf(
            e -> !StringHelper.isBlank(e.getValue().homeAccountId())
               && !StringHelper.isBlank(e.getValue().environment())
               && e.getValue().homeAccountId().equals(account.homeAccountId())
         );
   }

   private boolean isMatchingScopes(AccessTokenCacheEntity accessTokenCacheEntity, Set<String> scopes) {
      Set<String> accessTokenCacheEntityScopes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      accessTokenCacheEntityScopes.addAll(Arrays.asList(accessTokenCacheEntity.target().split(" ")));
      return accessTokenCacheEntityScopes.containsAll(scopes);
   }

   private boolean userAssertionHashMatches(Credential credential, String userAssertionHash) {
      return userAssertionHash == null ? true : credential.userAssertionHash() != null && credential.userAssertionHash().equalsIgnoreCase(userAssertionHash);
   }

   private boolean userAssertionHashMatches(AccountCacheEntity accountCacheEntity, String userAssertionHash) {
      return userAssertionHash == null
         ? true
         : accountCacheEntity.userAssertionHash() != null && accountCacheEntity.userAssertionHash().equalsIgnoreCase(userAssertionHash);
   }

   private Optional<AccessTokenCacheEntity> getAccessTokenCacheEntity(
      IAccount account, Authority authority, Set<String> scopes, String clientId, Set<String> environmentAliases
   ) {
      return this.accessTokens
         .values()
         .stream()
         .filter(
            accessToken -> accessToken.homeAccountId != null
               && accessToken.homeAccountId.equals(account.homeAccountId())
               && environmentAliases.contains(accessToken.environment)
               && accessToken.realm.equals(authority.tenant())
               && accessToken.clientId.equals(clientId)
               && this.isMatchingScopes(accessToken, scopes)
         )
         .findAny();
   }

   private Optional<AccessTokenCacheEntity> getApplicationAccessTokenCacheEntity(
      Authority authority, Set<String> scopes, String clientId, Set<String> environmentAliases, String userAssertionHash
   ) {
      long currTimeStampSec = new Date().getTime() / 1000L;
      return this.accessTokens
         .values()
         .stream()
         .filter(
            accessToken -> this.userAssertionHashMatches(accessToken, userAssertionHash)
               && environmentAliases.contains(accessToken.environment)
               && Long.parseLong(accessToken.expiresOn()) > currTimeStampSec + 300L
               && accessToken.realm.equals(authority.tenant())
               && accessToken.clientId.equals(clientId)
               && this.isMatchingScopes(accessToken, scopes)
         )
         .findAny();
   }

   private Optional<IdTokenCacheEntity> getIdTokenCacheEntity(IAccount account, Authority authority, String clientId, Set<String> environmentAliases) {
      return this.idTokens
         .values()
         .stream()
         .filter(
            idToken -> idToken.homeAccountId.equals(account.homeAccountId())
               && environmentAliases.contains(idToken.environment)
               && idToken.realm.equals(authority.tenant())
               && idToken.clientId.equals(clientId)
         )
         .findAny();
   }

   private Optional<IdTokenCacheEntity> getIdTokenCacheEntity(Authority authority, String clientId, Set<String> environmentAliases, String userAssertionHash) {
      return this.idTokens
         .values()
         .stream()
         .filter(
            idToken -> this.userAssertionHashMatches(idToken, userAssertionHash)
               && environmentAliases.contains(idToken.environment)
               && idToken.realm.equals(authority.tenant())
               && idToken.clientId.equals(clientId)
         )
         .findAny();
   }

   private Optional<RefreshTokenCacheEntity> getRefreshTokenCacheEntity(String clientId, Set<String> environmentAliases, String userAssertionHash) {
      return this.refreshTokens
         .values()
         .stream()
         .filter(
            refreshToken -> this.userAssertionHashMatches(refreshToken, userAssertionHash)
               && environmentAliases.contains(refreshToken.environment)
               && refreshToken.clientId.equals(clientId)
         )
         .findAny();
   }

   private Optional<RefreshTokenCacheEntity> getRefreshTokenCacheEntity(IAccount account, String clientId, Set<String> environmentAliases) {
      return this.refreshTokens
         .values()
         .stream()
         .filter(
            refreshToken -> refreshToken.homeAccountId != null
               && refreshToken.homeAccountId.equals(account.homeAccountId())
               && environmentAliases.contains(refreshToken.environment)
               && refreshToken.clientId.equals(clientId)
         )
         .findAny();
   }

   private Optional<AccountCacheEntity> getAccountCacheEntity(IAccount account, Set<String> environmentAliases) {
      return this.accounts
         .values()
         .stream()
         .filter(acc -> acc.homeAccountId.equals(account.homeAccountId()) && environmentAliases.contains(acc.environment))
         .findAny();
   }

   private Optional<AccountCacheEntity> getAccountCacheEntity(Set<String> environmentAliases, String userAssertionHash) {
      return this.accounts
         .values()
         .stream()
         .filter(acc -> this.userAssertionHashMatches(acc, userAssertionHash) && environmentAliases.contains(acc.environment))
         .findAny();
   }

   private Optional<RefreshTokenCacheEntity> getAnyFamilyRefreshTokenCacheEntity(IAccount account, Set<String> environmentAliases) {
      return this.refreshTokens
         .values()
         .stream()
         .filter(
            refreshToken -> refreshToken.homeAccountId.equals(account.homeAccountId())
               && environmentAliases.contains(refreshToken.environment)
               && refreshToken.isFamilyRT()
         )
         .findAny();
   }

   AuthenticationResult getCachedAuthenticationResult(IAccount account, Authority authority, Set<String> scopes, String clientId) {
      AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();
      Set<String> environmentAliases = AadInstanceDiscoveryProvider.getAliases(account.environment());

      try (TokenCache.CacheAspect cacheAspect = new TokenCache.CacheAspect(
            TokenCacheAccessContext.builder().clientId(clientId).tokenCache(this).account(account).build()
         )) {
         try {
            this.lock.readLock().lock();
            Optional<AccountCacheEntity> accountCacheEntity = this.getAccountCacheEntity(account, environmentAliases);
            Optional<AccessTokenCacheEntity> atCacheEntity = this.getAccessTokenCacheEntity(account, authority, scopes, clientId, environmentAliases);
            Optional<IdTokenCacheEntity> idTokenCacheEntity = this.getIdTokenCacheEntity(account, authority, clientId, environmentAliases);
            Optional<RefreshTokenCacheEntity> rtCacheEntity;
            if (!StringHelper.isBlank(this.getApplicationFamilyId(clientId, environmentAliases))) {
               rtCacheEntity = this.getAnyFamilyRefreshTokenCacheEntity(account, environmentAliases);
               if (!rtCacheEntity.isPresent()) {
                  rtCacheEntity = this.getRefreshTokenCacheEntity(account, clientId, environmentAliases);
               }
            } else {
               rtCacheEntity = this.getRefreshTokenCacheEntity(account, clientId, environmentAliases);
               if (!rtCacheEntity.isPresent()) {
                  rtCacheEntity = this.getAnyFamilyRefreshTokenCacheEntity(account, environmentAliases);
               }
            }

            if (atCacheEntity.isPresent()) {
               builder.environment(atCacheEntity.get().environment)
                  .accessToken(atCacheEntity.get().secret)
                  .expiresOn(Long.parseLong(atCacheEntity.get().expiresOn()));
               if (atCacheEntity.get().refreshOn() != null) {
                  builder.refreshOn(Long.parseLong(atCacheEntity.get().refreshOn()));
               }
            } else {
               builder.environment(authority.host());
            }

            idTokenCacheEntity.ifPresent(tokenCacheEntity -> builder.idToken(tokenCacheEntity.secret));
            rtCacheEntity.ifPresent(refreshTokenCacheEntity -> builder.refreshToken(refreshTokenCacheEntity.secret));
            accountCacheEntity.ifPresent(builder::accountCacheEntity);
         } finally {
            this.lock.readLock().unlock();
         }
      }

      return builder.build();
   }

   AuthenticationResult getCachedAuthenticationResult(Authority authority, Set<String> scopes, String clientId, IUserAssertion assertion) {
      AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();
      Set<String> environmentAliases = AadInstanceDiscoveryProvider.getAliases(authority.host);
      builder.environment(authority.host());

      AuthenticationResult var30;
      try (TokenCache.CacheAspect cacheAspect = new TokenCache.CacheAspect(TokenCacheAccessContext.builder().clientId(clientId).tokenCache(this).build())) {
         try {
            this.lock.readLock().lock();
            String userAssertionHash = assertion == null ? null : assertion.getAssertionHash();
            Optional<AccountCacheEntity> accountCacheEntity = this.getAccountCacheEntity(environmentAliases, userAssertionHash);
            accountCacheEntity.ifPresent(builder::accountCacheEntity);
            Optional<AccessTokenCacheEntity> atCacheEntity = this.getApplicationAccessTokenCacheEntity(
               authority, scopes, clientId, environmentAliases, userAssertionHash
            );
            if (atCacheEntity.isPresent()) {
               builder.accessToken(atCacheEntity.get().secret).expiresOn(Long.parseLong(atCacheEntity.get().expiresOn()));
               if (atCacheEntity.get().refreshOn() != null) {
                  builder.refreshOn(Long.parseLong(atCacheEntity.get().refreshOn()));
               }
            }

            Optional<IdTokenCacheEntity> idTokenCacheEntity = this.getIdTokenCacheEntity(authority, clientId, environmentAliases, userAssertionHash);
            idTokenCacheEntity.ifPresent(tokenCacheEntity -> builder.idToken(tokenCacheEntity.secret));
            Optional<RefreshTokenCacheEntity> rtCacheEntity = this.getRefreshTokenCacheEntity(clientId, environmentAliases, userAssertionHash);
            rtCacheEntity.ifPresent(refreshTokenCacheEntity -> builder.refreshToken(refreshTokenCacheEntity.secret));
         } finally {
            this.lock.readLock().unlock();
         }

         var30 = builder.build();
      }

      return var30;
   }

   private class CacheAspect implements AutoCloseable {
      ITokenCacheAccessContext context;

      CacheAspect(ITokenCacheAccessContext context) {
         if (TokenCache.this.tokenCacheAccessAspect != null) {
            this.context = context;
            TokenCache.this.tokenCacheAccessAspect.beforeCacheAccess(context);
         }
      }

      @Override
      public void close() {
         if (TokenCache.this.tokenCacheAccessAspect != null) {
            TokenCache.this.tokenCacheAccessAspect.afterCacheAccess(this.context);
         }
      }
   }
}
