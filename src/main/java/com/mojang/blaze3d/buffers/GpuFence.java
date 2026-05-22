package com.mojang.blaze3d.buffers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * GPU-барьер синхронизации (fence), позволяющий CPU ожидать завершения
 * ранее отправленных GPU-команд. После использования обязательно закрыть.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface GpuFence extends AutoCloseable {

	@Override
	void close();

	/**
	 * Блокирует текущий поток до завершения GPU-операций, связанных с этим барьером,
	 * либо до истечения таймаута.
	 *
	 * @param timeoutNanos максимальное время ожидания в наносекундах; 0 — немедленная проверка без блокировки
	 * @return {@code true}, если GPU завершил работу до истечения таймаута
	 */
	boolean awaitCompletion(long timeoutNanos);
}
