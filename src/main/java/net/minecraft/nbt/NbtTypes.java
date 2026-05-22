package net.minecraft.nbt;

/**
 * Реестр всех известных типов NBT-тегов, индексированный по числовому идентификатору.
 * <p>
 * Порядок элементов в массиве строго соответствует спецификации формата NBT:
 * индекс 0 — {@code TAG_End}, индекс 1 — {@code TAG_Byte}, ..., индекс 12 — {@code TAG_Long_Array}.
 */
public final class NbtTypes {

	private NbtTypes() {
	}

	private static final NbtType<?>[] VALUES = new NbtType[]{
		NbtEnd.TYPE,
		NbtByte.TYPE,
		NbtShort.TYPE,
		NbtInt.TYPE,
		NbtLong.TYPE,
		NbtFloat.TYPE,
		NbtDouble.TYPE,
		NbtByteArray.TYPE,
		NbtString.TYPE,
		NbtList.TYPE,
		NbtCompound.TYPE,
		NbtIntArray.TYPE,
		NbtLongArray.TYPE
	};

	/**
	 * Возвращает тип NBT-тега по его числовому идентификатору.
	 * Если идентификатор выходит за пределы допустимого диапазона,
	 * возвращает специальный «невалидный» тип, который бросает исключение при чтении.
	 *
	 * @param id числовой идентификатор типа (0–12)
	 * @return соответствующий {@link NbtType}, или «невалидный» тип для неизвестных id
	 */
	public static NbtType<?> byId(int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : NbtType.createInvalid(id);
	}
}
