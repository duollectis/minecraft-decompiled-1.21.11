package net.minecraft.nbt.scanner;

import net.minecraft.nbt.NbtType;

/**
 * Интерфейс потокового сканера NBT-данных.
 * <p>
 * Позволяет обходить структуру NBT без полной десериализации в объекты.
 * Каждый метод возвращает {@link Result} или {@link NestedResult}, управляющий дальнейшим обходом:
 * <ul>
 *   <li>{@link Result#CONTINUE} — продолжить обход</li>
 *   <li>{@link Result#BREAK} — завершить текущий вложенный блок</li>
 *   <li>{@link Result#HALT} — немедленно прервать весь обход</li>
 * </ul>
 *
 * @see SimpleNbtScanner
 * @see NbtCollector
 */
public interface NbtScanner {

	NbtScanner.Result visitEnd();

	NbtScanner.Result visitString(String value);

	NbtScanner.Result visitByte(byte value);

	NbtScanner.Result visitShort(short value);

	NbtScanner.Result visitInt(int value);

	NbtScanner.Result visitLong(long value);

	NbtScanner.Result visitFloat(float value);

	NbtScanner.Result visitDouble(double value);

	NbtScanner.Result visitByteArray(byte[] value);

	NbtScanner.Result visitIntArray(int[] value);

	NbtScanner.Result visitLongArray(long[] value);

	NbtScanner.Result visitListMeta(NbtType<?> entryType, int length);

	NbtScanner.NestedResult visitSubNbtType(NbtType<?> type);

	NbtScanner.NestedResult startSubNbt(NbtType<?> type, String key);

	NbtScanner.NestedResult startListItem(NbtType<?> type, int index);

	NbtScanner.Result endNested();

	NbtScanner.Result start(NbtType<?> rootType);

	/**
	 * Результат обхода вложенного элемента (компаунда или списка).
	 * Управляет тем, нужно ли входить внутрь, пропустить или прервать обход.
	 */
	enum NestedResult {
		ENTER,
		SKIP,
		BREAK,
		HALT
	}

	/**
	 * Результат обхода примитивного элемента.
	 * Управляет продолжением, завершением блока или полной остановкой.
	 */
	enum Result {
		CONTINUE,
		BREAK,
		HALT
	}
}
