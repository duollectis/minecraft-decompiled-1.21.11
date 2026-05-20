package com.microsoft.aad.msal4j;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.util.List;

public interface IClientCertificate extends IClientCredential {
   PrivateKey privateKey();

   default String publicCertificateHash256() throws CertificateEncodingException, NoSuchAlgorithmException {
      return null;
   }

   String publicCertificateHash() throws CertificateEncodingException, NoSuchAlgorithmException;

   List<String> getEncodedPublicKeyCertificateChain() throws CertificateEncodingException;
}
