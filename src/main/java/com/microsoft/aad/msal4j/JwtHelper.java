package com.microsoft.aad.msal4j;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class JwtHelper {
   static ClientAssertion buildJwt(String clientId, ClientCertificate credential, String jwtAudience, boolean sendX5c, boolean useSha1) throws MsalClientException {
      ParameterValidationUtils.validateNotBlank("clientId", clientId);
      ParameterValidationUtils.validateNotNull("credential", clientId);

      try {
         long time = System.currentTimeMillis();
         Map<String, Object> header = new HashMap<>();
         header.put("alg", "RS256");
         header.put("typ", "JWT");
         if (sendX5c) {
            List<String> certs = new ArrayList<>(credential.getEncodedPublicKeyCertificateChain());
            header.put("x5c", certs);
         }

         String hash256 = credential.publicCertificateHash256();
         if (!useSha1 && hash256 != null) {
            header.put("x5t#S256", hash256);
         } else {
            header.put("x5t", credential.publicCertificateHash());
         }

         Map<String, Object> payload = new HashMap<>();
         payload.put("aud", jwtAudience);
         payload.put("iss", clientId);
         payload.put("jti", UUID.randomUUID().toString());
         payload.put("nbf", time / 1000L);
         payload.put("exp", time / 1000L + 600L);
         payload.put("sub", clientId);
         String jsonHeader = JsonHelper.writeJsonMap(header);
         String jsonPayload = JsonHelper.writeJsonMap(payload);
         String encodedHeader = base64UrlEncode(jsonHeader.getBytes(StandardCharsets.UTF_8));
         String encodedPayload = base64UrlEncode(jsonPayload.getBytes(StandardCharsets.UTF_8));
         String dataToSign = encodedHeader + "." + encodedPayload;
         Signature sig = Signature.getInstance("SHA256withRSA");
         sig.initSign(credential.privateKey());
         sig.update(dataToSign.getBytes(StandardCharsets.UTF_8));
         byte[] signatureBytes = sig.sign();
         String encodedSignature = base64UrlEncode(signatureBytes);
         String jwt = dataToSign + "." + encodedSignature;
         return new ClientAssertion(jwt);
      } catch (Exception var19) {
         throw new MsalClientException(var19);
      }
   }

   private static String base64UrlEncode(byte[] data) {
      return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
   }
}
