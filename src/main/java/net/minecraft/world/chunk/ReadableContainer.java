package net.minecraft.world.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.DataResult;
import net.minecraft.network.PacketByteBuf;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.LongStream;

/**
 * {@code ReadableContainer}.
 */
public interface ReadableContainer<T> {

	T get(int x, int y, int z);

	void forEachValue(Consumer<T> action);

	void writePacket(PacketByteBuf buf);

	int getPacketSize();

	@VisibleForTesting
	int getElementBits();

	boolean hasAny(Predicate<T> predicate);

	void count(PalettedContainer.Counter<T> counter);

	PalettedContainer<T> copy();

	PalettedContainer<T> slice();

	ReadableContainer.Serialized<T> serialize(PaletteProvider<T> provider);

	/**
	 * {@code Reader}.
	 */
	public interface Reader<T, C extends ReadableContainer<T>> {

		DataResult<C> read(PaletteProvider<T> provider, ReadableContainer.Serialized<T> serialized);
	}

	/**
	 * {@code Serialized}.
	 */
	public record Serialized<T>(List<T> paletteEntries, Optional<LongStream> storage, int bitsPerEntry) {

		public static final int MISSING_BITS_PER_ENTRY = -1;

		public Serialized(List<T> paletteEntries, Optional<LongStream> storage) {
			this(paletteEntries, storage, -1);
		}
	}
}
