package net.minecraft.util.crash;

import org.jspecify.annotations.Nullable;

/**
 * {@code CrashMemoryReserve}.
 */
public class CrashMemoryReserve {

	private static byte @Nullable [] reservedMemory;

	/**
	 * Reserve memory.
	 */
	public static void reserveMemory() {
		reservedMemory = new byte[10485760];
	}

	/**
	 * Release memory.
	 */
	public static void releaseMemory() {
		if (reservedMemory != null) {
			reservedMemory = null;

			try {
				System.gc();
				System.gc();
				System.gc();
			}
			catch (Throwable var1) {
			}
		}
	}
}
