package com.mojang.blaze3d.vertex;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Элемент вершинного формата: описывает один атрибут вершины (позиция, цвет, UV и т.д.).
 *
 * <p>Каждый элемент регистрируется глобально через {@link #register} и получает уникальный
 * числовой идентификатор {@code id} в диапазоне [0, {@link #MAX_COUNT}).
 * Идентификатор используется как индекс в битовой маске {@link #mask()} для быстрой
 * проверки наличия атрибута в формате.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public record VertexFormatElement(
		int id,
		int index,
		VertexFormatElement.Type type,
		VertexFormatElement.Usage usage,
		int count
) {

	public static final int MAX_COUNT = 32;
	private static final @Nullable VertexFormatElement[] BY_ID = new VertexFormatElement[MAX_COUNT];
	private static final List<VertexFormatElement> ELEMENTS = new ArrayList<>(MAX_COUNT);
	public static final VertexFormatElement POSITION =
		register(0, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3);
	public static final VertexFormatElement COLOR =
		register(1, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
	public static final VertexFormatElement UV0 =
		register(2, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2);
	public static final VertexFormatElement UV = UV0;
	public static final VertexFormatElement UV1 =
		register(3, 1, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
	public static final VertexFormatElement UV2 =
		register(4, 2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
	public static final VertexFormatElement NORMAL =
		register(5, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 3);
	public static final VertexFormatElement LINE_WIDTH =
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

		if (!supportsUsage(index, usage)) {
			throw new IllegalStateException(
				"Multiple vertex elements of the same type other than UVs are not supported"
			);
		}

		this.id = id;
		this.index = index;
		this.type = type;
		this.usage = usage;
		this.count = count;
	}

	/**
	 * Регистрирует новый элемент формата вершин в глобальном реестре.
	 *
	 * @throws IllegalArgumentException если элемент с данным {@code id} уже зарегистрирован
	 */
	public static VertexFormatElement register(
			int id,
			int index,
			VertexFormatElement.Type type,
			VertexFormatElement.Usage usage,
			int count
	) {
		VertexFormatElement element = new VertexFormatElement(id, index, type, usage, count);
		if (BY_ID[id] != null) {
			throw new IllegalArgumentException("Duplicate element registration for: " + id);
		}

		BY_ID[id] = element;
		ELEMENTS.add(element);
		return element;
	}

	private boolean supportsUsage(int uvIndex, VertexFormatElement.Usage elementUsage) {
		return uvIndex == 0 || elementUsage == VertexFormatElement.Usage.UV;
	}

	@Override
	public String toString() {
		return count + "," + usage + "," + type + " (" + id + ")";
	}

	/** Возвращает битовую маску этого элемента: {@code 1 << id}. */
	public int mask() {
		return 1 << id;
	}

	/** Возвращает размер одного элемента в байтах: {@code type.size() * count}. */
	public int byteSize() {
		return type.size() * count;
	}

	public static @Nullable VertexFormatElement byId(int id) {
		return BY_ID[id];
	}

	public static Stream<VertexFormatElement> elementsFromMask(int mask) {
		return ELEMENTS.stream().filter(element -> (mask & element.mask()) != 0);
	}

	/** Тип данных одного компонента атрибута вершины. */
	@Environment(EnvType.CLIENT)
	@DeobfuscateClass
	public enum Type {
		FLOAT(4, "Float"),
		UBYTE(1, "Unsigned Byte"),
		BYTE(1, "Byte"),
		USHORT(2, "Unsigned Short"),
		SHORT(2, "Short"),
		UINT(4, "Unsigned Int"),
		INT(4, "Int");

		private final int size;
		private final String name;

		Type(int size, String name) {
			this.size = size;
			this.name = name;
		}

		public int size() {
			return size;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/** Семантическое назначение атрибута вершины в шейдере. */
	@Environment(EnvType.CLIENT)
	@DeobfuscateClass
	public enum Usage {
		POSITION("Position"),
		NORMAL("Normal"),
		COLOR("Vertex Color"),
		UV("UV"),
		GENERIC("Generic");

		private final String name;

		Usage(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
