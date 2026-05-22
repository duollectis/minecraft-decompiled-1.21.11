package com.mojang.blaze3d.buffers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;

/**
 * Абстрактный GPU-буфер, представляющий выделенную область памяти на видеокарте.
 * Поддерживает различные режимы использования через битовые флаги {@link Usage}.
 * Реализует {@link AutoCloseable} — буфер обязательно должен быть закрыт после использования.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public abstract class GpuBuffer implements AutoCloseable {

	/** Флаг: буфер можно читать через маппинг в CPU-память. */
	public static final int USAGE_MAP_READ = 1;
	/** Флаг: буфер можно записывать через маппинг из CPU-памяти. */
	public static final int USAGE_MAP_WRITE = 2;
	/** Флаг: подсказка драйверу, что данные хранятся преимущественно на стороне CPU. */
	public static final int USAGE_HINT_CLIENT_STORAGE = 4;
	/** Флаг: буфер является целью операций копирования. */
	public static final int USAGE_COPY_DST = 8;
	/** Флаг: буфер является источником операций копирования. */
	public static final int USAGE_COPY_SRC = 16;
	/** Флаг: буфер используется как вершинный буфер. */
	public static final int USAGE_VERTEX = 32;
	/** Флаг: буфер используется как индексный буфер. */
	public static final int USAGE_INDEX = 64;
	/** Флаг: буфер используется как uniform-буфер. */
	public static final int USAGE_UNIFORM = 128;
	/** Флаг: буфер используется как texel-буфер для uniform-переменных. */
	public static final int USAGE_UNIFORM_TEXEL_BUFFER = 256;

	@Usage
	private final int usage;
	private final long size;

	public GpuBuffer(@Usage int usage, long size) {
		this.size = size;
		this.usage = usage;
	}

	public long size() {
		return size;
	}

	@Usage
	public int usage() {
		return usage;
	}

	public abstract boolean isClosed();

	@Override
	public abstract void close();

	/**
	 * Создаёт срез буфера с заданным смещением и длиной.
	 * Проверяет, что срез не выходит за границы буфера.
	 *
	 * @param offset смещение от начала буфера в байтах (>= 0)
	 * @param length длина среза в байтах (>= 0)
	 * @return новый {@link GpuBufferSlice}, ссылающийся на указанный диапазон
	 * @throws IllegalArgumentException если срез выходит за пределы буфера
	 */
	public GpuBufferSlice slice(long offset, long length) {
		if (offset < 0L || length < 0L || offset + length > size) {
			throw new IllegalArgumentException(
				"Offset of " + offset + " and length " + length
					+ " would put new slice outside buffer's range (of 0," + size + ")"
			);
		}

		return new GpuBufferSlice(this, offset, length);
	}

	/** Создаёт срез, охватывающий весь буфер целиком. */
	public GpuBufferSlice slice() {
		return new GpuBufferSlice(this, 0L, size);
	}

	/**
	 * Представление буфера, отображённого в CPU-память.
	 * Позволяет читать и/или записывать данные через {@link ByteBuffer}.
	 * Обязательно закрывать после использования для снятия маппинга.
	 */
	@Environment(EnvType.CLIENT)
	@DeobfuscateClass
	public interface MappedView extends AutoCloseable {

		ByteBuffer data();

		@Override
		void close();
	}

	/**
	 * Аннотация-маркер для параметров, полей и возвращаемых значений,
	 * обозначающая битовую маску флагов использования GPU-буфера.
	 */
	@Retention(RetentionPolicy.CLASS)
	@Target({
		ElementType.FIELD,
		ElementType.PARAMETER,
		ElementType.LOCAL_VARIABLE,
		ElementType.METHOD,
		ElementType.TYPE_USE
	})
	@Environment(EnvType.CLIENT)
	public @interface Usage {
	}
}
