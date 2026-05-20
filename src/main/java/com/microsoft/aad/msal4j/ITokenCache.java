package com.microsoft.aad.msal4j;

public interface ITokenCache {
   void deserialize(String var1);

   String serialize();
}
