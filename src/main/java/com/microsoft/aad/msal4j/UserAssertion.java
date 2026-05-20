package com.microsoft.aad.msal4j;

public class UserAssertion implements IUserAssertion {
   private final String assertion;
   private final String assertionHash;

   public UserAssertion(String assertion) {
      if (StringHelper.isBlank(assertion)) {
         throw new NullPointerException("assertion");
      } else {
         this.assertion = assertion;
         this.assertionHash = StringHelper.createBase64EncodedSha256Hash(this.assertion);
      }
   }

   @Override
   public String getAssertion() {
      return this.assertion;
   }

   @Override
   public String getAssertionHash() {
      return this.assertionHash;
   }
}
