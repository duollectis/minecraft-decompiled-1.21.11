package com.microsoft.aad.msal4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.Callable;

public class ClientCredentialFactory {
   public static IClientSecret createFromSecret(String secret) {
      return new ClientSecret(secret);
   }

   public static IClientCertificate createFromCertificate(InputStream pkcs12Certificate, String password) throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
      return ClientCertificate.create(pkcs12Certificate, password);
   }

   public static IClientCertificate createFromCertificate(PrivateKey key, X509Certificate publicKeyCertificate) {
      ParameterValidationUtils.validateNotNull("publicKeyCertificate", publicKeyCertificate);
      return ClientCertificate.create(key, publicKeyCertificate);
   }

   public static IClientCertificate createFromCertificateChain(PrivateKey key, List<X509Certificate> publicKeyCertificateChain) {
      if (key != null && publicKeyCertificateChain != null && publicKeyCertificateChain.size() != 0) {
         return new ClientCertificate(key, publicKeyCertificateChain);
      } else {
         throw new IllegalArgumentException("null or empty input parameter");
      }
   }

   public static IClientAssertion createFromClientAssertion(String clientAssertion) {
      return new ClientAssertion(clientAssertion);
   }

   public static IClientAssertion createFromCallback(Callable<String> callable) {
      if (callable == null) {
         throw new NullPointerException("callable");
      } else {
         return new ClientAssertion(callable);
      }
   }
}
