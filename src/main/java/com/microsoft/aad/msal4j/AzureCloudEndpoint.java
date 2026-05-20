package com.microsoft.aad.msal4j;

public enum AzureCloudEndpoint {
   AzurePublic("https://login.microsoftonline.com/"),
   AzureChina("https://login.chinacloudapi.cn/"),
   AzureGermany("https://login.microsoftonline.de/"),
   AzureUsGovernment("https://login.microsoftonline.us/");

   public final String endpoint;

   private AzureCloudEndpoint(String endpoint) {
      this.endpoint = endpoint;
   }
}
