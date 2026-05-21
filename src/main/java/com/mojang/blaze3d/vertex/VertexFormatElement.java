package com.mojang.blaze3d.vertex;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
/**
 * {@code VertexFormatElement}.
 */
public record VertexFormatElement(
		int id,
		int index,
		VertexFormatElement.Type type,
		VertexFormatElement.Usage usage,
		int count
) {

	public static final int MAX_COUNT = 32;
	private static final @Nullable VertexFormatElement[] BY_ID = new VertexFormatElement[32];
	private static final List<VertexFormatElement> ELEMENTS = new ArrayList<>(32);
	public static final VertexFormatElement
			POSITION =
			register(0, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3);
	public static final VertexFormatElement
			COLOR =
			register(1, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
	public static final VertexFormatElement
			UV0 =
			register(2, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2);
	public static final VertexFormatElement UV = UV0;
	public static final VertexFormatElement
			UV1 =
			register(3, 1, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
	public static final VertexFormatElement
			UV2 =
			register(4, 2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
	public static final VertexFormatElement
			NORMAL =
			register(5, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 3);
	public static final VertexFormatElement
			LINE_WIDTH =
			register(6, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1);

	public VertexFormatElement(
			int id,
			int index,
			VertexFormatElement.Type type,
			VertexFormatElement.Usage usage,
			int count
	) {
		if (id < 0 || id >= BY_ID.length) {
			throw new IllegalArgumentException("Element ID must be in range [0; " + BY_ID.length + ")");
		}
		else if (!this.supportsUsage(index, usage)) {
			throw new IllegalStateException("Multiple vertex elements of the same type other than UVs are not supported");
		}
		else {
			this.id = id;
			this.index = index;
			this.type = type;
			this.usage = usage;
			this.count = count;
		}
	}

	public static VertexFormatElement register(
			int id,
			int index,
			VertexFormatElement.Type type,
			VertexFormatElement.Usage usage,
			int count
	) {
		VertexFormatElement vertexFormatElement = new VertexFormatElement(id, index, type, usage, count);
		if (BY_ID[id] != null) {
			throw new IllegalArgumentException("Duplicate element registration for: " + id);
		}
		else {
			BY_ID[id] = vertexFormatElement;
			ELEMENTS.add(vertexFormatElement);
			return vertexFormatElement;
		}
	}

	private boolean supportsUsage(int uvIndex, VertexFormatElement.Usage usage) {
		return uvIndex == 0 || usage == VertexFormatElement.Usage.UV;
	}

	@Override
	public String toString() {
		return this.count + "," + this.usage + "," + this.type + " (" + this.id + ")";
	}

	public int mask() {
		return 1 << this.id;
	}

	public int byteSize() {
		return this.type.size() * this.count;
	}

	public static @Nullable VertexFormatElement byId(int id) {
		return BY_ID[id];
	}

	public static Stream<VertexFormatElement> elementsFromMask(int mask) {
		return ELEMENTS.stream().filter(element -> (mask & element.mask()) != 0);
	}

	@Environment(EnvType.CLIENT)
	@DeobfuscateClass
	/**
	 * {@code Type}.
	 */
	public static enum Type {
		FLOAT(4, "Float"),
		UBYTE(1, "Unsigned Byte"),
		BYTE(1, "Byte"),
		USHORT(2, "Unsigned Short"),
		SHORT(2, "Short"),
		UINT(4, "Unsigned Int"),
		INT(4, "Int");

		private final int size;
		private final String name;

		private Type(final int size, final String name) {
			this.size = size;
			this.name = name;
		}

		public int size() {
			return this.size;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	@Environment(EnvType.CLIENT)
	@DeobfuscateClass
	/**
	 * {@code Usage}.
	 */
	public static enum Usage {
		POSITION("Position"),
		NORMAL("Normal"),
		COLOR("Vertex Color"),
		UV("UV"),
		GENERIC("Generic");

		private final String name;

		private Usage(final String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}
