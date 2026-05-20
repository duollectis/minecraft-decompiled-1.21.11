package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class SystemKeycodes {
   private static final boolean IS_MAC_OS_IMPL = Util.getOperatingSystem() == Util.OperatingSystem.OSX;
   public static final boolean IS_MAC_OS = IS_MAC_OS_IMPL;
   public static final int CTRL_MOD = IS_MAC_OS ? 8 : 2;
   public static final boolean USE_LONG_LEFT_PRESS = IS_MAC_OS_IMPL;
   public static final boolean UPDATE_PRESSED_STATE_ON_MOUSE_GRAB = !IS_MAC_OS_IMPL;
}
