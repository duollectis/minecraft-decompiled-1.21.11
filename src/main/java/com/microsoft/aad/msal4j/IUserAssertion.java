package com.microsoft.aad.msal4j;

public interface IUserAssertion {
   String getAssertion();

   String getAssertionHash();
}
