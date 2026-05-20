package com.microsoft.aad.msal4j;

public class OSHelper {
   private static final String OS = System.getProperty("os.name").toLowerCase();
   private static OSHelper.OSType osType;

   public static String getOs() {
      return OS;
   }

   public static boolean isMac() {
      return OSHelper.OSType.MAC.equals(osType);
   }

   public static boolean isWindows() {
      return OSHelper.OSType.WINDOWS.equals(osType);
   }

   public static boolean isLinux() {
      return OSHelper.OSType.LINUX.equals(osType);
   }

   static {
      if (OS.contains("windows")) {
         osType = OSHelper.OSType.WINDOWS;
      } else if (OS.contains("mac")) {
         osType = OSHelper.OSType.MAC;
      } else if (OS.contains("nux") || OS.contains("nix")) {
         osType = OSHelper.OSType.LINUX;
      }
   }

   static enum OSType {
      MAC,
      WINDOWS,
      LINUX;
   }
}
