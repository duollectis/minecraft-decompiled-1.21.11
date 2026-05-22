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
 * Интерфейс доступа к палитризованному контейнеру данных секции чанка.
 * Предоставляет операции чтения, сериализации и копирования.
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

	/** Читает контейнер из сериализованного представления. */
	interface Reader<T, C extends ReadableContainer<T>> {

		DataResult<C> read(PaletteProvider<T> provider, ReadableContainer.Serialized<T> serialized);
	}

	/**
	 * Сериализованное представление контейнера: список значений палитры,
	 * опциональный поток данных хранилища и число бит на элемент.
	 */
	record Serialized<T>(List<T> paletteEntries, Optional<LongStream> storage, int bitsPerEntry) {

		public static final int MISSING_BITS_PER_ENTRY = -1;

		public Serialized(List<T> paletteEntries, Optional<LongStream> storage) {
			this(paletteEntries, storage, MISSING_BITS_PER_ENTRY);
		}
	}
}
