package net.minecraft.client.util;

import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryUtil.MemoryAllocator;
import org.slf4j.Logger;

import java.nio.ByteBuffer;

/**
 * Низкоуровневый аллокатор нативной памяти для вершинных буферов.
 * Управляет единым непрерывным блоком памяти с поддержкой роста и подсчёта ссылок.
 * Реализует паттерн «кольцевого» сдвига: при освобождении последнего буфера
 * данные сдвигаются в начало, что позволяет переиспользовать память без realloc.
 */
@Environment(EnvType.CLIENT)
public class BufferAllocator implements AutoCloseable {

	private static final MemoryPool MEMORY_POOL = TracyClient.createMemoryPool("ByteBufferBuilder");
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);
	private static final long MAX_UNSIGNED_INT_SIZE = 4294967295L;
	private static final int MIN_GROWTH_BYTES = 2097152;
	private static final int MAX_JAVA_BUFFER_SIZE = 2147483647;
	private static final int CLOSED_SENTINEL = -1;

	long pointer;
	private long size;
	private final long maxSize;
	private long offset;
	private long lastOffset;
	private int refCount;
	private int clearCount;

	public BufferAllocator(int initialSize, long maxSize) {
		this.size = initialSize;
		this.maxSize = maxSize;
		pointer = ALLOCATOR.malloc(initialSize);
		MEMORY_POOL.malloc(pointer, initialSize);
		if (pointer == 0L) {
			throw new OutOfMemoryError("Failed to allocate " + initialSize + " bytes");
		}
	}

	public BufferAllocator(int initialSize) {
		this(initialSize, MAX_UNSIGNED_INT_SIZE);
	}

	public static BufferAllocator fixedSized(int size) {
		return new BufferAllocator(size, size);
	}

	/**
	 * Выделяет {@code size} байт в буфере и возвращает нативный указатель на начало выделенного блока.
	 * При необходимости автоматически расширяет буфер.
	 *
	 * @param size количество байт для выделения
	 * @return нативный указатель на начало выделенного блока
	 */
	public long allocate(int size) {
		long startOffset = offset;
		long newOffset = Math.addExact(startOffset, (long) size);
		growIfNecessary(newOffset);
		offset = newOffset;
		return Math.addExact(pointer, startOffset);
	}

	private void growIfNecessary(long requiredSize) {
		if (requiredSize <= this.size) {
			return;
		}

		if (requiredSize > maxSize) {
			throw new IllegalArgumentException(
				"Maximum capacity of ByteBufferBuilder (" + maxSize + ") exceeded, required " + requiredSize
			);
		}

		long growth = Math.min(this.size, (long) MIN_GROWTH_BYTES);
		long newSize = MathHelper.clamp(this.size + growth, requiredSize, maxSize);
		grow(newSize);
	}

	private void grow(long newSize) {
		MEMORY_POOL.free(pointer);
		pointer = ALLOCATOR.realloc(pointer, newSize);
		MEMORY_POOL.malloc(pointer, (int) Math.min(newSize, MAX_JAVA_BUFFER_SIZE));
		LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", size, newSize);
		if (pointer == 0L) {
			throw new OutOfMemoryError("Failed to resize buffer from " + size + " bytes to " + newSize + " bytes");
		}

		size = newSize;
	}

	/**
	 * Возвращает {@link CloseableBuffer} для данных, записанных с момента последнего вызова этого метода.
	 * Возвращает {@code null}, если новых данных нет.
	 *
	 * @return буфер с новыми данными или {@code null}
	 * @throws IllegalStateException если буфер уже освобождён или данные превышают 2 ГБ
	 */
	public @Nullable CloseableBuffer getAllocated() {
		ensureNotFreed();
		long dataStart = lastOffset;
		long dataSize = offset - dataStart;
		if (dataSize == 0L) {
			return null;
		}

		if (dataSize > MAX_JAVA_BUFFER_SIZE) {
			throw new IllegalStateException("Cannot build buffer larger than 2147483647 bytes (was " + dataSize + ")");
		}

		lastOffset = offset;
		refCount++;
		return new CloseableBuffer(dataStart, (int) dataSize, clearCount);
	}

	public void clear() {
		if (refCount > 0) {
			LOGGER.warn("Clearing BufferBuilder with unused batches");
		}

		reset();
	}

	public void reset() {
		ensureNotFreed();
		if (refCount > 0) {
			forceClear();
			refCount = 0;
		}
	}

	boolean clearCountEquals(int count) {
		return count == clearCount;
	}

	void clearIfUnreferenced() {
		if (--refCount <= 0) {
			forceClear();
		}
	}

	private void forceClear() {
		long pendingBytes = offset - lastOffset;
		if (pendingBytes > 0L) {
			MemoryUtil.memCopy(pointer + lastOffset, pointer, pendingBytes);
		}

		offset = pendingBytes;
		lastOffset = 0L;
		clearCount++;
	}

	@Override
	public void close() {
		if (pointer == 0L) {
			return;
		}

		MEMORY_POOL.free(pointer);
		ALLOCATOR.free(pointer);
		pointer = 0L;
		clearCount = CLOSED_SENTINEL;
	}

	private void ensureNotFreed() {
		if (pointer == 0L) {
			throw new IllegalStateException("Buffer has been freed");
		}
	}

	@Environment(EnvType.CLIENT)
	public class CloseableBuffer implements AutoCloseable {

		private final long offset;
		private final int size;
		private final int clearCount;
		private boolean closed;

		CloseableBuffer(long offset, int size, int clearCount) {
			this.offset = offset;
			this.size = size;
			this.clearCount = clearCount;
		}

		/**
		 * Возвращает {@link ByteBuffer}, указывающий на данные этого буфера.
		 *
		 * @return нативный {@link ByteBuffer} с данными
		 * @throws IllegalStateException если родительский аллокатор был очищен
		 */
		public ByteBuffer getBuffer() {
			if (!BufferAllocator.this.clearCountEquals(clearCount)) {
				throw new IllegalStateException("Buffer is no longer valid");
			}

			return MemoryUtil.memByteBuffer(BufferAllocator.this.pointer + offset, size);
		}

		@Override
		public void close() {
			if (closed) {
				return;
			}

			closed = true;
			if (BufferAllocator.this.clearCountEquals(clearCount)) {
				BufferAllocator.this.clearIfUnreferenced();
			}
		}
	}
}
