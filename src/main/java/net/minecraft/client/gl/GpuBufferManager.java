package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Абстрактный менеджер GPU-буферов. Выбирает реализацию в зависимости от поддержки
 * расширения {@code GL_ARB_buffer_storage}: ARB-вариант использует персистентное
 * отображение памяти, Direct-вариант — стандартный {@code glMapBufferRange}.
 */
@Environment(EnvType.CLIENT)
public abstract class GpuBufferManager {

	// Флаги доступа для персистентного маппинга (GL_ARB_buffer_storage)
	private static final int GL_MAP_READ_BIT = 1;
	private static final int GL_MAP_WRITE_BIT = 2;
	private static final int GL_MAP_FLUSH_EXPLICIT_BIT = 16;
	private static final int GL_MAP_PERSISTENT_BIT = 64;
	// MAP_WRITE | MAP_FLUSH_EXPLICIT
	private static final int ARB_WRITE_FLAGS = GL_MAP_WRITE_BIT | GL_MAP_FLUSH_EXPLICIT_BIT;

	/**
	 * Создаёт оптимальную реализацию менеджера буферов.
	 * Предпочитает ARB-вариант с персистентным маппингом при наличии расширения.
	 */
	public static GpuBufferManager create(GLCapabilities capabilities, Set<String> usedCapabilities) {
		if (capabilities.GL_ARB_buffer_storage && GlBackend.allowGlBufferStorage) {
			usedCapabilities.add("GL_ARB_buffer_storage");
			return new ARBGpuBufferManager();
		}

		return new DirectGpuBufferManager();
	}

	public abstract GlGpuBuffer createBuffer(
		BufferManager bufferManager,
		@Nullable Supplier<String> debugLabelSupplier,
		@GpuBuffer.Usage int usage,
		long size
	);

	public abstract GlGpuBuffer createBuffer(
		BufferManager bufferManager,
		@Nullable Supplier<String> debugLabelSupplier,
		@GpuBuffer.Usage int usage,
		ByteBuffer data
	);

	public abstract GlGpuBuffer.Mapped mapBufferRange(
		BufferManager bufferManager,
		GlGpuBuffer buffer,
		long offset,
		long length,
		int flags
	);

	/**
	 * Реализация на базе {@code GL_ARB_buffer_storage} с персистентным маппингом.
	 * Буфер отображается в память один раз при создании и остаётся доступным всё время жизни.
	 */
	@Environment(EnvType.CLIENT)
	static class ARBGpuBufferManager extends GpuBufferManager {

		@Override
		public GlGpuBuffer createBuffer(
			BufferManager bufferManager,
			@Nullable Supplier<String> debugLabelSupplier,
			@GpuBuffer.Usage int usage,
			long size
		) {
			int glId = bufferManager.createBuffer();
			bufferManager.setBufferStorage(glId, size, usage);
			ByteBuffer mapped = mapPersistent(bufferManager, usage, glId, size);

			return new GlGpuBuffer(debugLabelSupplier, bufferManager, usage, size, glId, mapped);
		}

		@Override
		public GlGpuBuffer createBuffer(
			BufferManager bufferManager,
			@Nullable Supplier<String> debugLabelSupplier,
			@GpuBuffer.Usage int usage,
			ByteBuffer data
		) {
			int glId = bufferManager.createBuffer();
			int dataSize = data.remaining();
			bufferManager.setBufferStorage(glId, data, usage);
			ByteBuffer mapped = mapPersistent(bufferManager, usage, glId, dataSize);

			return new GlGpuBuffer(debugLabelSupplier, bufferManager, usage, dataSize, glId, mapped);
		}

		/**
		 * Выполняет персистентное отображение буфера в память, если флаги доступа это требуют.
		 * Возвращает {@code null} для буферов без флагов чтения/записи.
		 */
		private @Nullable ByteBuffer mapPersistent(
			BufferManager bufferManager,
			@GpuBuffer.Usage int usage,
			int buffer,
			long length
		) {
			int accessFlags = 0;

			if ((usage & GL_MAP_READ_BIT) != 0) {
				accessFlags |= GL_MAP_READ_BIT;
			}

			if ((usage & GL_MAP_WRITE_BIT) != 0) {
				accessFlags |= ARB_WRITE_FLAGS;
			}

			if (accessFlags == 0) {
				return null;
			}

			GlStateManager.clearGlErrors();
			ByteBuffer mapped = bufferManager.mapBufferRange(buffer, 0L, length, accessFlags | GL_MAP_PERSISTENT_BIT, usage);

			if (mapped == null) {
				throw new IllegalStateException(
					"Can't persistently map buffer, opengl error " + GlStateManager._getError()
				);
			}

			return mapped;
		}

		@Override
		public GlGpuBuffer.Mapped mapBufferRange(
			BufferManager bufferManager,
			GlGpuBuffer buffer,
			long offset,
			long length,
			int flags
		) {
			if (buffer.backingBuffer == null) {
				throw new IllegalStateException("Somehow trying to map an unmappable buffer");
			}

			if (offset > Integer.MAX_VALUE || length > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Mapping buffers larger than 2GB is not supported");
			}

			if (offset < 0L || length < 0L) {
				throw new IllegalArgumentException("Offset or length must be positive integer values");
			}

			return new GlGpuBuffer.Mapped(
				() -> {
					if ((flags & GL_MAP_WRITE_BIT) != 0) {
						bufferManager.flushMappedBufferRange(buffer.id, offset, length, buffer.usage());
					}
				},
				buffer,
				MemoryUtil.memSlice(buffer.backingBuffer, (int) offset, (int) length)
			);
		}
	}

	/**
	 * Стандартная реализация через {@code glMapBufferRange} без персистентного маппинга.
	 * Используется как запасной вариант при отсутствии {@code GL_ARB_buffer_storage}.
	 */
	@Environment(EnvType.CLIENT)
	static class DirectGpuBufferManager extends GpuBufferManager {

		@Override
		public GlGpuBuffer createBuffer(
			BufferManager bufferManager,
			@Nullable Supplier<String> debugLabelSupplier,
			@GpuBuffer.Usage int usage,
			long size
		) {
			int glId = bufferManager.createBuffer();
			bufferManager.setBufferData(glId, size, usage);

			return new GlGpuBuffer(debugLabelSupplier, bufferManager, usage, size, glId, null);
		}

		@Override
		public GlGpuBuffer createBuffer(
			BufferManager bufferManager,
			@Nullable Supplier<String> debugLabelSupplier,
			@GpuBuffer.Usage int usage,
			ByteBuffer data
		) {
			int glId = bufferManager.createBuffer();
			int dataSize = data.remaining();
			bufferManager.setBufferData(glId, data, usage);

			return new GlGpuBuffer(debugLabelSupplier, bufferManager, usage, dataSize, glId, null);
		}

		@Override
		public GlGpuBuffer.Mapped mapBufferRange(
			BufferManager bufferManager,
			GlGpuBuffer buffer,
			long offset,
			long length,
			int flags
		) {
			GlStateManager.clearGlErrors();
			ByteBuffer mapped = bufferManager.mapBufferRange(buffer.id, offset, length, flags, buffer.usage());

			if (mapped == null) {
				throw new IllegalStateException("Can't map buffer, opengl error " + GlStateManager._getError());
			}

			return new GlGpuBuffer.Mapped(
				() -> bufferManager.unmapBuffer(buffer.id, buffer.usage()),
				buffer,
				mapped
			);
		}
	}
}
