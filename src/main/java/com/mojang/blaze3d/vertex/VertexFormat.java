package com.mojang.blaze3d.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GpuDeviceInfo;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Описание формата вершинного буфера: набор атрибутов, их типы, смещения и общий размер вершины.
 *
 * <p>Создаётся через {@link Builder}. После построения является иммутабельным.
 * Хранит кэшированные GPU-буферы для немедленной отрисовки ({@code immediateDrawVertexBuffer}
 * и {@code immediateDrawIndexBuffer}), которые переиспользуются между кадрами.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class VertexFormat {

	// USAGE_COPY_DST | USAGE_VERTEX = 8 | 32 = 40
	private static final int IMMEDIATE_VERTEX_BUFFER_USAGE = GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX;
	// USAGE_COPY_DST | USAGE_INDEX = 8 | 64 = 72
	private static final int IMMEDIATE_INDEX_BUFFER_USAGE = GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_INDEX;

	public static final int UNKNOWN_ELEMENT = -1;
	private final List<VertexFormatElement> elements;
	private final List<String> names;
	private final int vertexSize;
	private final int elementsMask;
	private final int[] offsetsByElement = new int[32];
	private @Nullable GpuBuffer immediateDrawVertexBuffer;
	private @Nullable GpuBuffer immediateDrawIndexBuffer;

	VertexFormat(List<VertexFormatElement> elements, List<String> names, IntList offsets, int vertexSize) {
		this.elements = elements;
		this.names = names;
		this.vertexSize = vertexSize;
		elementsMask = elements.stream().mapToInt(VertexFormatElement::mask).reduce(0, (a, b) -> a | b);

		for (int slot = 0; slot < offsetsByElement.length; slot++) {
			VertexFormatElement element = VertexFormatElement.byId(slot);
			int elementIndex = element != null ? elements.indexOf(element) : -1;
			offsetsByElement[slot] = elementIndex != -1 ? offsets.getInt(elementIndex) : -1;
		}
	}

	public static VertexFormat.Builder builder() {
		return new VertexFormat.Builder();
	}

	@Override
	public String toString() {
		return "VertexFormat" + names;
	}

	public int getVertexSize() {
		return vertexSize;
	}

	public List<VertexFormatElement> getElements() {
		return elements;
	}

	public List<String> getElementAttributeNames() {
		return names;
	}

	public int[] getOffsetsByElement() {
		return offsetsByElement;
	}

	public int getOffset(VertexFormatElement element) {
		return offsetsByElement[element.id()];
	}

	public boolean contains(VertexFormatElement element) {
		return (elementsMask & element.mask()) != 0;
	}

	public int getElementsMask() {
		return elementsMask;
	}

	public String getElementName(VertexFormatElement element) {
		int index = elements.indexOf(element);
		if (index == -1) {
			throw new IllegalArgumentException(element + " is not contained in format");
		}

		return names.get(index);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof VertexFormat other
			&& elementsMask == other.elementsMask
			&& vertexSize == other.vertexSize
			&& names.equals(other.names)
			&& Arrays.equals(offsetsByElement, other.offsetsByElement);
	}

	@Override
	public int hashCode() {
		return elementsMask * 31 + Arrays.hashCode(offsetsByElement);
	}

	/**
	 * Загружает данные в GPU-буфер, переиспользуя существующий если возможно.
	 * На устройствах, требующих пересоздания буфера при загрузке, всегда создаёт новый.
	 *
	 * @param gpuBuffer существующий буфер для переиспользования или {@code null}
	 * @param data      данные для загрузки
	 * @param usage     флаги использования буфера
	 * @param labelGetter поставщик отладочного имени буфера
	 * @return актуальный GPU-буфер с загруженными данными
	 */
	private static GpuBuffer uploadToBuffer(
			@Nullable GpuBuffer gpuBuffer,
			ByteBuffer data,
			@GpuBuffer.Usage int usage,
			Supplier<String> labelGetter
	) {
		GpuDevice gpuDevice = RenderSystem.getDevice();

		if (GpuDeviceInfo.get(gpuDevice).requiresRecreateOnUploadToBuffer()) {
			if (gpuBuffer != null) {
				gpuBuffer.close();
			}

			return gpuDevice.createBuffer(labelGetter, usage, data);
		}

		if (gpuBuffer == null) {
			return gpuDevice.createBuffer(labelGetter, usage, data);
		}

		if (gpuBuffer.size() < data.remaining()) {
			gpuBuffer.close();
			return gpuDevice.createBuffer(labelGetter, usage, data);
		}

		CommandEncoder commandEncoder = gpuDevice.createCommandEncoder();
		commandEncoder.writeToBuffer(gpuBuffer.slice(), data);
		return gpuBuffer;
	}

	public GpuBuffer uploadImmediateVertexBuffer(ByteBuffer data) {
		immediateDrawVertexBuffer = uploadToBuffer(
			immediateDrawVertexBuffer,
			data,
			IMMEDIATE_VERTEX_BUFFER_USAGE,
			() -> "Immediate vertex buffer for " + this
		);
		return immediateDrawVertexBuffer;
	}

	public GpuBuffer uploadImmediateIndexBuffer(ByteBuffer data) {
		immediateDrawIndexBuffer = uploadToBuffer(
			immediateDrawIndexBuffer,
			data,
			IMMEDIATE_INDEX_BUFFER_USAGE,
			() -> "Immediate index buffer for " + this
		);
		return immediateDrawIndexBuffer;
	}

	/** Строитель формата вершин. Элементы добавляются в порядке вызовов {@link #add}. */
	@Environment(EnvType.CLIENT)
	@DeobfuscateClass
	public static class Builder {

		private final ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
		private final IntList offsets = new IntArrayList();
		private int offset;

		Builder() {
		}

		public VertexFormat.Builder add(String name, VertexFormatElement element) {
			elements.put(name, element);
			offsets.add(offset);
			offset = offset + element.byteSize();
			return this;
		}

		public VertexFormat.Builder padding(int padding) {
			offset += padding;
			return this;
		}

		public VertexFormat build() {
			ImmutableMap<String, VertexFormatElement> elementMap = elements.buildOrThrow();
			ImmutableList<VertexFormatElement> elementList = elementMap.values().asList();
			ImmutableList<String> nameList = elementMap.keySet().asList();
			return new VertexFormat(elementList, nameList, offsets, offset);
		}
	}

	/** Режим отрисовки примитивов: определяет топологию и количество индексов на вершину. */
	@Environment(EnvType.CLIENT)
	public enum DrawMode {
		LINES(2, 2, false),
		DEBUG_LINES(2, 2, false),
		DEBUG_LINE_STRIP(2, 1, true),
		POINTS(1, 1, false),
		TRIANGLES(3, 3, false),
		TRIANGLE_STRIP(3, 1, true),
		TRIANGLE_FAN(3, 1, true),
		QUADS(4, 4, false);

		public final int firstVertexCount;
		public final int additionalVertexCount;
		public final boolean shareVertices;

		DrawMode(int firstVertexCount, int additionalVertexCount, boolean shareVertices) {
			this.firstVertexCount = firstVertexCount;
			this.additionalVertexCount = additionalVertexCount;
			this.shareVertices = shareVertices;
		}

		public int getIndexCount(int vertexCount) {
			return switch (this) {
				case LINES, QUADS -> vertexCount / 4 * 6;
				case DEBUG_LINES, DEBUG_LINE_STRIP, POINTS, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> vertexCount;
				default -> 0;
			};
		}
	}

	/** Тип индексного буфера: SHORT (2 байта, до 65535 вершин) или INT (4 байта). */
	@Environment(EnvType.CLIENT)
	public enum IndexType {
		SHORT(2),
		INT(4);

		public final int size;

		IndexType(int size) {
			this.size = size;
		}

		/**
		 * Возвращает минимальный тип индекса, достаточный для адресации {@code vertexCount} вершин.
		 * Использует SHORT если количество вершин помещается в 16 бит (< 65536).
		 */
		public static VertexFormat.IndexType smallestFor(int vertexCount) {
			// Маска 0xFFFF0000 проверяет, есть ли биты выше 16-го
			return (vertexCount & -65536) != 0 ? INT : SHORT;
		}
	}
}
