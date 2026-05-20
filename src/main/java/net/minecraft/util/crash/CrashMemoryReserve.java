package net.minecraft.util.crash;

import org.jspecify.annotations.Nullable;

public class CrashMemoryReserve {
   private static byte @Nullable [] reservedMemory;

   public static void reserveMemory() {
      reservedMemory = new byte[10485760];
   }

   public static void releaseMemory() {
      if (reservedMemory != null) {
         reservedMemory = null;

         try {
            System.gc();
            System.gc();
            System.gc();
         } catch (Throwable var1) {
         }
      }
   }
}
