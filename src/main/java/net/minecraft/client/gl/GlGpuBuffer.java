package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * Реализация {@link GpuBuffer} для OpenGL.
 * Отслеживает выделение памяти через Tracy и поддерживает опциональный backing-буфер
 * для реализаций без поддержки ARB_buffer_storage.
 */
@Environment(EnvType.CLIENT)
public class GlGpuBuffer extends GpuBuffer {

	protected static final MemoryPool POOL = TracyClient.createMemoryPool("GPU Buffers");

	protected boolean closed;
	protected final @Nullable Supplier<String> debugLabelSupplier;
	protected final int id;
	protected @Nullable ByteBuffer backingBuffer;

	private final BufferManager bufferManager;

	protected GlGpuBuffer(
		@Nullable Supplier<String> debugLabelSupplier,
		BufferManager bufferManager,
		@GpuBuffer.Usage int usage,
		long size,
		int id,
		@Nullable ByteBuffer backingBuffer
	) {
		super(usage, size);
		this.debugLabelSupplier = debugLabelSupplier;
		this.bufferManager = bufferManager;
		this.id = id;
		this.backingBuffer = backingBuffer;
		POOL.malloc(id, (int) Math.min(size, Integer.MAX_VALUE));
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}

		closed = true;

		if (backingBuffer != null) {
			bufferManager.unmapBuffer(id, usage());
			backingBuffer = null;
		}

		GlStateManager._glDeleteBuffers(id);
		POOL.free(id);
	}

	/**
	 * Представление замапленного диапазона GPU-буфера.
	 * Гарантирует однократное закрытие через флаг {@code closed}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Mapped implements GpuBuffer.MappedView {

		private final Runnable closer;
		private final GlGpuBuffer backingBuffer;
		private final ByteBuffer data;
		private boolean closed;

		protected Mapped(Runnable closer, GlGpuBuffer backingBuffer, ByteBuffer data) {
			this.closer = closer;
			this.backingBuffer = backingBuffer;
			this.data = data;
		}

		@Override
		public ByteBuffer data() {
			return data;
		}

		@Override
		public void close() {
			if (closed) {
				return;
			}

			closed = true;
			closer.run();
		}
	}
}
