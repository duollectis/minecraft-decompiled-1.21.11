package com.microsoft.aad.msal4j;

class EnvironmentVariables implements IEnvironmentVariables {
   @Override
   public String getEnvironmentVariable(String envVariable) {
      return System.getenv(envVariable);
   }
}
