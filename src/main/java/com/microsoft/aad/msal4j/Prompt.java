package com.microsoft.aad.msal4j;

public enum Prompt {
   LOGIN("login"),
   SELECT_ACCOUNT("select_account"),
   CONSENT("consent"),
   ADMIN_CONSENT("admin_consent"),
   NONE("none");

   private String prompt;

   private Prompt(String prompt) {
      this.prompt = prompt;
   }

   @Override
   public String toString() {
      return this.prompt;
   }
}
