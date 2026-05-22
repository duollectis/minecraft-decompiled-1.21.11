package net.minecraft.client.render;

import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.Vec3fArray;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Хранит результат построения вершинного буфера: сырые вершинные данные и параметры отрисовки.
 * Поддерживает сортировку квадов по глубине для корректного рендеринга прозрачных поверхностей.
 * Реализует {@link AutoCloseable} — освобождает нативную память при закрытии.
 */
@Environment(EnvType.CLIENT)
public class BuiltBuffer implements AutoCloseable {

	private static final int VERTICES_PER_QUAD = 4;

	private final BufferAllocator.CloseableBuffer buffer;
	private BufferAllocator.@Nullable CloseableBuffer sortedBuffer;
	private final BuiltBuffer.DrawParameters drawParameters;

	public BuiltBuffer(BufferAllocator.CloseableBuffer buffer, BuiltBuffer.DrawParameters drawParameters) {
		this.buffer = buffer;
		this.drawParameters = drawParameters;
	}

	/**
	 * Вычисляет центроиды всех квадов в буфере для последующей сортировки по глубине.
	 * Центроид каждого квада определяется как среднее между первой и третьей вершинами (диагональ).
	 *
	 * @param buffer      байтовый буфер с вершинными данными
	 * @param vertexCount общее количество вершин
	 * @param format      формат вершин, определяющий смещение позиции и размер вершины
	 * @return массив центроидов квадов
	 */
	private static Vec3fArray collectCentroids(ByteBuffer buffer, int vertexCount, VertexFormat format) {
		int positionOffset = format.getOffset(VertexFormatElement.POSITION);
		if (positionOffset == -1) {
			throw new IllegalArgumentException("Cannot identify quad centers with no position element");
		}

		FloatBuffer floatBuffer = buffer.asFloatBuffer();
		int floatsPerVertex = format.getVertexSize() / 4;
		int floatsPerQuad = floatsPerVertex * VERTICES_PER_QUAD;
		int quadCount = vertexCount / VERTICES_PER_QUAD;
		Vec3fArray centroids = new Vec3fArray(quadCount);

		for (int quadIndex = 0; quadIndex < quadCount; quadIndex++) {
			int vertex0Offset = quadIndex * floatsPerQuad + positionOffset;
			int vertex2Offset = vertex0Offset + floatsPerVertex * 2;
			float x0 = floatBuffer.get(vertex0Offset);
			float y0 = floatBuffer.get(vertex0Offset + 1);
			float z0 = floatBuffer.get(vertex0Offset + 2);
			float x2 = floatBuffer.get(vertex2Offset);
			float y2 = floatBuffer.get(vertex2Offset + 1);
			float z2 = floatBuffer.get(vertex2Offset + 2);
			centroids.set(quadIndex, (x0 + x2) / 2.0F, (y0 + y2) / 2.0F, (z0 + z2) / 2.0F);
		}

		return centroids;
	}

	public ByteBuffer getBuffer() {
		return buffer.getBuffer();
	}

	public @Nullable ByteBuffer getSortedBuffer() {
		return sortedBuffer != null ? sortedBuffer.getBuffer() : null;
	}

	public BuiltBuffer.DrawParameters getDrawParameters() {
		return drawParameters;
	}

	/**
	 * Сортирует квады по глубине с помощью переданного {@link VertexSorter} и сохраняет
	 * переупорядоченный индексный буфер. Возвращает {@code null}, если режим отрисовки — не QUADS.
	 *
	 * @param allocator аллокатор для нового индексного буфера
	 * @param sorter    стратегия сортировки квадов по глубине
	 * @return состояние сортировки с центроидами, либо {@code null}
	 */
	public BuiltBuffer.@Nullable SortState sortQuads(BufferAllocator allocator, VertexSorter sorter) {
		if (drawParameters.mode() != VertexFormat.DrawMode.QUADS) {
			return null;
		}

		Vec3fArray centroids = collectCentroids(
				buffer.getBuffer(),
				drawParameters.vertexCount(),
				drawParameters.format()
		);
		BuiltBuffer.SortState sortState = new BuiltBuffer.SortState(centroids, drawParameters.indexType());
		sortedBuffer = sortState.sortAndStore(allocator, sorter);
		return sortState;
	}

	@Override
	public void close() {
		buffer.close();
		if (sortedBuffer != null) {
			sortedBuffer.close();
		}
	}

	/**
	 * Параметры отрисовки построенного буфера: формат вершин, количество вершин и индексов,
	 * режим примитивов и тип индексов.
	 */
	@Environment(EnvType.CLIENT)
	public record DrawParameters(
			VertexFormat format,
			int vertexCount,
			int indexCount,
			VertexFormat.DrawMode mode,
			VertexFormat.IndexType indexType
	) {
	}

	/**
	 * Хранит центроиды квадов и тип индексов для повторной сортировки буфера по глубине.
	 * Используется при изменении позиции камеры для обновления порядка прозрачных квадов.
	 */
	@Environment(EnvType.CLIENT)
	public record SortState(Vec3fArray centroids, VertexFormat.IndexType indexType) {

		/**
		 * Сортирует квады по центроидам, записывает индексы треугольников (2 на квад) в новый буфер.
		 * Каждый квад разбивается на два треугольника: (0,1,2) и (2,3,0).
		 *
		 * @param allocator аллокатор нативной памяти
		 * @param sorter    стратегия сортировки
		 * @return буфер с отсортированными индексами, либо {@code null}
		 */
		public BufferAllocator.@Nullable CloseableBuffer sortAndStore(BufferAllocator allocator, VertexSorter sorter) {
			int[] sortedIndices = sorter.sort(centroids);
			long bufferPointer = allocator.allocate(sortedIndices.length * 6 * indexType.size);
			IntConsumer indexWriter = getStorer(bufferPointer, indexType);

			for (int quadIndex : sortedIndices) {
				int base = quadIndex * VERTICES_PER_QUAD;
				indexWriter.accept(base);
				indexWriter.accept(base + 1);
				indexWriter.accept(base + 2);
				indexWriter.accept(base + 2);
				indexWriter.accept(base + 3);
				indexWriter.accept(base);
			}

			return allocator.getAllocated();
		}

		private IntConsumer getStorer(long pointer, VertexFormat.IndexType indexType) {
			MutableLong cursor = new MutableLong(pointer);

			return switch (indexType) {
				case SHORT -> index -> MemoryUtil.memPutShort(cursor.getAndAdd(2L), (short) index);
				case INT -> index -> MemoryUtil.memPutInt(cursor.getAndAdd(4L), index);
			};
		}
	}
}
