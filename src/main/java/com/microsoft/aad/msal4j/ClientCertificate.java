package com.microsoft.aad.msal4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

final class ClientCertificate implements IClientCertificate {
   public static final String DEFAULT_PKCS12_PASSWORD = "";
   private final PrivateKey privateKey;
   private final List<X509Certificate> publicKeyCertificateChain;

   ClientCertificate(PrivateKey privateKey, List<X509Certificate> publicKeyCertificateChain) {
      if (privateKey == null) {
         throw new NullPointerException("PrivateKey is null or empty");
      } else {
         this.privateKey = privateKey;
         this.publicKeyCertificateChain = publicKeyCertificateChain;
      }
   }

   @Override
   public String publicCertificateHash256() throws CertificateEncodingException, NoSuchAlgorithmException {
      return Base64.getEncoder().encodeToString(getHashSha256(this.publicKeyCertificateChain.get(0).getEncoded()));
   }

   @Override
   public String publicCertificateHash() throws CertificateEncodingException, NoSuchAlgorithmException {
      return Base64.getEncoder().encodeToString(getHashSha1(this.publicKeyCertificateChain.get(0).getEncoded()));
   }

   @Override
   public List<String> getEncodedPublicKeyCertificateChain() throws CertificateEncodingException {
      List<String> result = new ArrayList<>();

      for (X509Certificate cert : this.publicKeyCertificateChain) {
         result.add(Base64.getEncoder().encodeToString(cert.getEncoded()));
      }

      return result;
   }

   public String getAssertion(Authority authority, String clientId, boolean sendX5c) {
      if (authority == null) {
         throw new NullPointerException("Authority cannot be null");
      } else {
         boolean useSha1 = Authority.detectAuthorityType(authority.canonicalAuthorityUrl()) == AuthorityType.ADFS;
         return JwtHelper.buildJwt(clientId, this, authority.selfSignedJwtAudience(), sendX5c, useSha1).assertion();
      }
   }

   static ClientCertificate create(InputStream pkcs12Certificate, String password) throws KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
      if (password == null) {
         password = "";
      }

      KeyStore keystore = KeyStore.getInstance("PKCS12");
      keystore.load(pkcs12Certificate, password.toCharArray());
      String alias = getPrivateKeyAlias(keystore);
      ArrayList<X509Certificate> publicKeyCertificateChain = new ArrayList<>();
      PrivateKey privateKey = (PrivateKey)keystore.getKey(alias, password.toCharArray());
      X509Certificate publicKeyCertificate = (X509Certificate)keystore.getCertificate(alias);
      Certificate[] chain = keystore.getCertificateChain(alias);
      if (chain != null && chain.length > 0) {
         for (Certificate c : chain) {
            publicKeyCertificateChain.add((X509Certificate)c);
         }
      } else {
         publicKeyCertificateChain.add(publicKeyCertificate);
      }

      return new ClientCertificate(privateKey, publicKeyCertificateChain);
   }

   static String getPrivateKeyAlias(KeyStore keystore) throws KeyStoreException {
      String alias = null;
      Enumeration<String> aliases = keystore.aliases();

      while (aliases.hasMoreElements()) {
         String currentAlias = aliases.nextElement();
         if (keystore.entryInstanceOf(currentAlias, PrivateKeyEntry.class)) {
            if (alias != null) {
               throw new IllegalArgumentException("more than one certificate alias found in input stream");
            }

            alias = currentAlias;
         }
      }

      if (alias == null) {
         throw new IllegalArgumentException("certificate not loaded from input stream");
      } else {
         return alias;
      }
   }

   static ClientCertificate create(PrivateKey key, X509Certificate publicKeyCertificate) {
      return new ClientCertificate(key, Arrays.asList(publicKeyCertificate));
   }

   private static byte[] getHashSha1(byte[] inputBytes) throws NoSuchAlgorithmException {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.update(inputBytes);
      return md.digest();
   }

   private static byte[] getHashSha256(byte[] inputBytes) throws NoSuchAlgorithmException {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(inputBytes);
      return md.digest();
   }

   @Override
   public PrivateKey privateKey() {
      return this.privateKey;
   }
}
