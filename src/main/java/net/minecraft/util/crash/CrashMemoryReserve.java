package net.minecraft.util.crash;

import org.jspecify.annotations.Nullable;

/**
 * Резервирует блок памяти заранее, чтобы при {@link OutOfMemoryError} его можно было
 * освободить и получить достаточно памяти для записи отчёта о сбое.
 */
public class CrashMemoryReserve {

	/** Размер резервного буфера: 10 МБ. */
	private static final int RESERVE_SIZE_BYTES = 10 * 1024 * 1024;

	private static byte @Nullable [] reservedMemory;

	public static void reserveMemory() {
		reservedMemory = new byte[RESERVE_SIZE_BYTES];
	}

	/**
	 * Освобождает зарезервированную память и принудительно запускает GC,
	 * чтобы освободить место для записи отчёта о сбое.
	 */
	public static void releaseMemory() {
		if (reservedMemory == null) {
			return;
		}

		reservedMemory = null;

		try {
			System.gc();
			System.gc();
			System.gc();
		}
		catch (Throwable ignored) {
		}
	}
}
