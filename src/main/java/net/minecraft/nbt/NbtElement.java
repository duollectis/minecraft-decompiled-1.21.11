package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

/**
 * Базовый запечатанный интерфейс для всех NBT-элементов.
 *
 * <p>Иерархия типов строго ограничена через {@code sealed}: составные ({@link NbtCompound}),
 * списочные ({@link AbstractNbtList}), примитивные ({@link NbtPrimitive}) и маркер конца ({@link NbtEnd}).
 * Каждый тип идентифицируется числовым идентификатором, соответствующим спецификации NBT-формата.</p>
 */
public sealed interface NbtElement permits NbtCompound, AbstractNbtList, NbtPrimitive, NbtEnd {

	/** Базовый размер строкового тега в байтах (заголовок объекта). */
	int STRING_SIZE_BYTES = 8;

	/** Базовый размер списочного тега в байтах. */
	int LIST_SIZE_BYTES = 12;

	/** Базовый размер числового тега в байтах. */
	int NUMBER_SIZE_BYTES = 4;

	/** Базовый размер составного тега в байтах. */
	int COMPOUND_SIZE_BYTES = 28;

	/** Идентификатор типа {@link NbtEnd}. */
	byte END_TYPE = 0;

	/** Идентификатор типа {@link NbtByte}. */
	byte BYTE_TYPE = 1;

	/** Идентификатор типа {@link NbtShort}. */
	byte SHORT_TYPE = 2;

	/** Идентификатор типа {@link NbtInt}. */
	byte INT_TYPE = 3;

	/** Идентификатор типа {@link NbtLong}. */
	byte LONG_TYPE = 4;

	/** Идентификатор типа {@link NbtFloat}. */
	byte FLOAT_TYPE = 5;

	/** Идентификатор типа {@link NbtDouble}. */
	byte DOUBLE_TYPE = 6;

	/** Идентификатор типа {@link NbtByteArray}. */
	byte BYTE_ARRAY_TYPE = 7;

	/** Идентификатор типа {@link NbtString}. */
	byte STRING_TYPE = 8;

	/** Идентификатор типа {@link NbtList}. */
	byte LIST_TYPE = 9;

	/** Идентификатор типа {@link NbtCompound}. */
	byte COMPOUND_TYPE = 10;

	/** Идентификатор типа {@link NbtIntArray}. */
	byte INT_ARRAY_TYPE = 11;

	/** Идентификатор типа {@link NbtLongArray}. */
	byte LONG_ARRAY_TYPE = 12;

	/** Максимальная глубина вложенности NBT-структур при чтении. */
	int MAX_DEPTH = 512;

	/**
	 * Сериализует элемент в бинарный поток.
	 *
	 * @param output поток вывода
	 * @throws IOException при ошибке записи
	 */
	void write(DataOutput output) throws IOException;

	@Override
	String toString();

	/** @return числовой идентификатор типа согласно спецификации NBT */
	byte getType();

	/** @return объект типа, содержащий логику чтения и пропуска */
	NbtType<?> getNbtType();

	/** @return глубокая копия элемента */
	NbtElement copy();

	/** @return приблизительный размер элемента в байтах для отслеживания лимитов */
	int getSizeInBytes();

	/**
	 * Принимает визитор паттерна Visitor.
	 *
	 * @param visitor визитор
	 */
	void accept(NbtElementVisitor visitor);

	/**
	 * Выполняет обход элемента сканером без проверки стартового условия.
	 * Вызывается только после того, как {@link NbtScanner#start} вернул {@link NbtScanner.Result#CONTINUE}.
	 *
	 * @param visitor сканер
	 * @return результат обхода
	 */
	NbtScanner.Result doAccept(NbtScanner visitor);

	/**
	 * Запускает обход элемента сканером с проверкой стартового условия.
	 *
	 * @param visitor сканер
	 */
	default void accept(NbtScanner visitor) {
		NbtScanner.Result result = visitor.start(getNbtType());

		if (result == NbtScanner.Result.CONTINUE) {
			doAccept(visitor);
		}
	}

	/** @return строковое значение, если элемент является {@link NbtString} */
	default Optional<String> asString() {
		return Optional.empty();
	}

	/** @return числовое значение, если элемент является числовым типом */
	default Optional<Number> asNumber() {
		return Optional.empty();
	}

	default Optional<Byte> asByte() {
		return asNumber().map(Number::byteValue);
	}

	default Optional<Short> asShort() {
		return asNumber().map(Number::shortValue);
	}

	default Optional<Integer> asInt() {
		return asNumber().map(Number::intValue);
	}

	default Optional<Long> asLong() {
		return asNumber().map(Number::longValue);
	}

	default Optional<Float> asFloat() {
		return asNumber().map(Number::floatValue);
	}

	default Optional<Double> asDouble() {
		return asNumber().map(Number::doubleValue);
	}

	/** @return булево значение: {@code true} если байтовое значение не равно нулю */
	default Optional<Boolean> asBoolean() {
		return asByte().map(b -> b != 0);
	}

	default Optional<byte[]> asByteArray() {
		return Optional.empty();
	}

	default Optional<int[]> asIntArray() {
		return Optional.empty();
	}

	default Optional<long[]> asLongArray() {
		return Optional.empty();
	}

	default Optional<NbtCompound> asCompound() {
		return Optional.empty();
	}

	default Optional<NbtList> asNbtList() {
		return Optional.empty();
	}
}
