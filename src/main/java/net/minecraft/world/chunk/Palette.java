package net.minecraft.world.chunk;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.IndexedIterable;

import java.util.List;
import java.util.function.Predicate;

/**
 * {@code Palette}.
 */
public interface Palette<T> {

	int index(T object, PaletteResizeListener<T> listener);

	boolean hasAny(Predicate<T> predicate);

	T get(int id);

	void readPacket(PacketByteBuf buf, IndexedIterable<T> idList);

	void writePacket(PacketByteBuf buf, IndexedIterable<T> idList);

	int getPacketSize(IndexedIterable<T> idList);

	int getSize();

	Palette<T> copy();

	/**
	 * {@code Factory}.
	 */
	public interface Factory {

		<A> Palette<A> create(int bits, List<A> values);
	}
}
