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

@Environment(EnvType.CLIENT)
/**
 * {@code GlGpuBuffer}.
 */
public class GlGpuBuffer extends GpuBuffer {

	protected static final MemoryPool POOL = TracyClient.createMemoryPool("GPU Buffers");
	protected boolean closed;
	protected final @Nullable Supplier<String> debugLabelSupplier;
	private final BufferManager bufferManager;
	protected final int id;
	protected @Nullable ByteBuffer backingBuffer;

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
		int i = (int) Math.min(size, 2147483647L);
		POOL.malloc(id, i);
	}

	@Override
	public boolean isClosed() {
		return this.closed;
	}

	@Override
	public void close() {
		if (!this.closed) {
			this.closed = true;
			if (this.backingBuffer != null) {
				this.bufferManager.unmapBuffer(this.id, this.usage());
				this.backingBuffer = null;
			}

			GlStateManager._glDeleteBuffers(this.id);
			POOL.free(this.id);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Mapped}.
	 */
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
			return this.data;
		}

		@Override
		public void close() {
			if (!this.closed) {
				this.closed = true;
				this.closer.run();
			}
		}
	}
}
