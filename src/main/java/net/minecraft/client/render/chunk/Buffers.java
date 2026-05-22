package net.minecraft.client.render.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

/**
 * Хранит GPU-буферы одной отрисованной секции чанка: вершинный буфер и опциональный
 * индексный буфер (используется для прозрачных слоёв с сортировкой вершин).
 * Реализует {@link AutoCloseable} — оба буфера освобождаются при закрытии.
 */
@Environment(EnvType.CLIENT)
public final class Buffers implements AutoCloseable {

	private GpuBuffer vertexBuffer;
	private @Nullable GpuBuffer indexBuffer;
	private int indexCount;
	private VertexFormat.IndexType indexType;

	public Buffers(
			GpuBuffer vertexBuffer,
			@Nullable GpuBuffer indexBuffer,
			int indexCount,
			VertexFormat.IndexType indexType
	) {
		this.vertexBuffer = vertexBuffer;
		this.indexBuffer = indexBuffer;
		this.indexCount = indexCount;
		this.indexType = indexType;
	}

	public GpuBuffer getVertexBuffer() {
		return vertexBuffer;
	}

	public @Nullable GpuBuffer getIndexBuffer() {
		return indexBuffer;
	}

	public void setIndexBuffer(@Nullable GpuBuffer indexBuffer) {
		this.indexBuffer = indexBuffer;
	}

	public int getIndexCount() {
		return indexCount;
	}

	public VertexFormat.IndexType getIndexType() {
		return indexType;
	}

	public void setIndexType(VertexFormat.IndexType indexType) {
		this.indexType = indexType;
	}

	public void setIndexCount(int indexCount) {
		this.indexCount = indexCount;
	}

	public void setVertexBuffer(GpuBuffer vertexBuffer) {
		this.vertexBuffer = vertexBuffer;
	}

	@Override
	public void close() {
		vertexBuffer.close();

		if (indexBuffer != null) {
			indexBuffer.close();
		}
	}
}
