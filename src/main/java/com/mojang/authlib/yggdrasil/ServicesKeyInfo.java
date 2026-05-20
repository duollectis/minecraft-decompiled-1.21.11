package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.properties.Property;
import java.security.Signature;

public interface ServicesKeyInfo {
   int keyBitCount();

   default int signatureBitCount() {
      return this.keyBitCount();
   }

   Signature signature();

   boolean validateProperty(Property var1);
}
