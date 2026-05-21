package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

/**
 * {@code NbtElement}.
 */
public sealed interface NbtElement permits NbtCompound, AbstractNbtList, NbtPrimitive, NbtEnd {

	int STRING_SIZE_BYTES = 8;

	int LIST_SIZE_BYTES = 12;

	int NUMBER_SIZE_BYTES = 4;

	int COMPOUND_SIZE_BYTES = 28;

	byte END_TYPE = 0;

	byte BYTE_TYPE = 1;

	byte SHORT_TYPE = 2;

	byte INT_TYPE = 3;

	byte LONG_TYPE = 4;

	byte FLOAT_TYPE = 5;

	byte DOUBLE_TYPE = 6;

	byte BYTE_ARRAY_TYPE = 7;

	byte STRING_TYPE = 8;

	byte LIST_TYPE = 9;

	byte COMPOUND_TYPE = 10;

	byte INT_ARRAY_TYPE = 11;

	byte LONG_ARRAY_TYPE = 12;

	int MAX_DEPTH = 512;

	void write(DataOutput output) throws IOException;

	@Override
	String toString();

	byte getType();

	NbtType<?> getNbtType();

	NbtElement copy();

	int getSizeInBytes();

	void accept(NbtElementVisitor visitor);

	NbtScanner.Result doAccept(NbtScanner visitor);

	default void accept(NbtScanner visitor) {
		NbtScanner.Result result = visitor.start(this.getNbtType());
		if (result == NbtScanner.Result.CONTINUE) {
			this.doAccept(visitor);
		}
	}

	default Optional<String> asString() {
		return Optional.empty();
	}

	default Optional<Number> asNumber() {
		return Optional.empty();
	}

	default Optional<Byte> asByte() {
		return this.asNumber().map(Number::byteValue);
	}

	default Optional<Short> asShort() {
		return this.asNumber().map(Number::shortValue);
	}

	default Optional<Integer> asInt() {
		return this.asNumber().map(Number::intValue);
	}

	default Optional<Long> asLong() {
		return this.asNumber().map(Number::longValue);
	}

	default Optional<Float> asFloat() {
		return this.asNumber().map(Number::floatValue);
	}

	default Optional<Double> asDouble() {
		return this.asNumber().map(Number::doubleValue);
	}

	default Optional<Boolean> asBoolean() {
		return this.asByte().map(b -> b != 0);
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
