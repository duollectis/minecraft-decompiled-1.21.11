package com.microsoft.aad.msal4j;

import java.util.Objects;
import java.util.concurrent.Callable;

final class ClientAssertion implements IClientAssertion {
   static final String ASSERTION_TYPE_JWT_BEARER = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
   private final String assertion;
   private final Callable<String> assertionProvider;

   ClientAssertion(String assertion) {
      if (StringHelper.isBlank(assertion)) {
         throw new NullPointerException("assertion");
      } else {
         this.assertion = assertion;
         this.assertionProvider = null;
      }
   }

   ClientAssertion(Callable<String> assertionProvider) {
      if (assertionProvider == null) {
         throw new NullPointerException("assertionProvider");
      } else {
         this.assertion = null;
         this.assertionProvider = assertionProvider;
      }
   }

   @Override
   public String assertion() {
      if (this.assertionProvider != null) {
         try {
            String generatedAssertion = this.assertionProvider.call();
            if (StringHelper.isBlank(generatedAssertion)) {
               throw new MsalClientException("Assertion provider returned null or empty assertion", "invalid_jwt");
            } else {
               return generatedAssertion;
            }
         } catch (MsalClientException var2) {
            throw var2;
         } catch (Exception var3) {
            throw new MsalClientException(var3);
         }
      } else {
         return this.assertion;
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof ClientAssertion)) {
         return false;
      } else {
         ClientAssertion other = (ClientAssertion)o;
         return this.assertionProvider != null && other.assertionProvider != null
            ? this.assertionProvider == other.assertionProvider
            : Objects.equals(this.assertion(), other.assertion());
      }
   }

   @Override
   public int hashCode() {
      if (this.assertionProvider != null) {
         return System.identityHashCode(this.assertionProvider);
      } else {
         int result = 1;
         return result * 59 + (this.assertion == null ? 43 : this.assertion.hashCode());
      }
   }
}
