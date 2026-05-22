package net.minecraft.nbt;

import java.util.Optional;

/**
 * Запечатанный интерфейс для всех числовых NBT-примитивов.
 *
 * <p>Предоставляет явные методы доступа к значению в каждом числовом типе Java,
 * а также переопределяет все {@code asXxx()} методы из {@link NbtElement} для возврата
 * {@link Optional#of} вместо {@link Optional#empty()}, избегая лишних аллокаций через {@code map}.</p>
 */
public sealed interface AbstractNbtNumber extends NbtPrimitive
		permits NbtByte, NbtShort, NbtInt, NbtLong, NbtFloat, NbtDouble {

	byte byteValue();

	short shortValue();

	int intValue();

	long longValue();

	float floatValue();

	double doubleValue();

	Number numberValue();

	@Override
	default Optional<Number> asNumber() {
		return Optional.of(numberValue());
	}

	@Override
	default Optional<Byte> asByte() {
		return Optional.of(byteValue());
	}

	@Override
	default Optional<Short> asShort() {
		return Optional.of(shortValue());
	}

	@Override
	default Optional<Integer> asInt() {
		return Optional.of(intValue());
	}

	@Override
	default Optional<Long> asLong() {
		return Optional.of(longValue());
	}

	@Override
	default Optional<Float> asFloat() {
		return Optional.of(floatValue());
	}

	@Override
	default Optional<Double> asDouble() {
		return Optional.of(doubleValue());
	}

	@Override
	default Optional<Boolean> asBoolean() {
		return Optional.of(byteValue() != 0);
	}
}
