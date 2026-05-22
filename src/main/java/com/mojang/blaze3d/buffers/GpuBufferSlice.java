package com.mojang.blaze3d.buffers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Неизменяемый срез {@link GpuBuffer}, описывающий диапазон байт внутри родительского буфера.
 * Используется для передачи подмножества данных буфера в операции рендеринга и копирования
 * без создания нового GPU-объекта.
 *
 * @param buffer родительский GPU-буфер
 * @param offset смещение от начала буфера в байтах
 * @param length длина среза в байтах
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public record GpuBufferSlice(GpuBuffer buffer, long offset, long length) {

	/**
	 * Создаёт дочерний срез относительно текущего среза.
	 * Итоговое смещение вычисляется как {@code this.offset + offset}.
	 *
	 * @param offset смещение от начала текущего среза в байтах (>= 0)
	 * @param length длина нового среза в байтах (>= 0)
	 * @return новый {@link GpuBufferSlice} внутри текущего диапазона
	 * @throws IllegalArgumentException если новый срез выходит за пределы текущего
	 */
	public GpuBufferSlice slice(long offset, long length) {
		if (offset < 0L || length < 0L || offset + length > this.length) {
			throw new IllegalArgumentException(
				"Offset of " + offset + " and length " + length
					+ " would put new slice outside existing slice's range (of "
					+ this.offset + "," + this.length + ")"
			);
		}

		return new GpuBufferSlice(buffer, this.offset + offset, length);
	}
}
