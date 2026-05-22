package net.minecraft.world.chunk;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.util.collection.IndexedIterable;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.function.Predicate;

/**
 * Линейная палитра на основе массива — используется для небольших секций
 * (до 2^indexBits элементов). Поиск по значению линейный O(n),
 * что приемлемо при малом размере (≤16 элементов).
 */
public class ArrayPalette<T> implements Palette<T> {

	private final T[] array;
	private final int indexBits;
	private int size;

	@SuppressWarnings("unchecked")
	private ArrayPalette(int indexBits, List<T> values) {
		this.array = (T[]) new Object[1 << indexBits];
		this.indexBits = indexBits;
		Validate.isTrue(
			values.size() <= array.length,
			"Can't initialize LinearPalette of size %d with %d entries",
			array.length, values.size()
		);

		for (int i = 0; i < values.size(); i++) {
			array[i] = values.get(i);
		}

		size = values.size();
	}

	@SuppressWarnings("unchecked")
	private ArrayPalette(T[] array, int indexBits, int size) {
		this.array = array;
		this.indexBits = indexBits;
		this.size = size;
	}

	public static <A> Palette<A> create(int bits, List<A> values) {
		return new ArrayPalette<>(bits, values);
	}

	@Override
	public int index(T object, PaletteResizeListener<T> listener) {
		for (int i = 0; i < size; i++) {
			if (array[i] == object) {
				return i;
			}
		}

		int nextIndex = size;
		if (nextIndex < array.length) {
			array[nextIndex] = object;
			size++;
			return nextIndex;
		}

		return listener.onResize(indexBits + 1, object);
	}

	@Override
	public boolean hasAny(Predicate<T> predicate) {
		for (int i = 0; i < size; i++) {
			if (predicate.test(array[i])) {
				return true;
			}
		}

		return false;
	}

	@Override
	public T get(int id) {
		if (id >= 0 && id < size) {
			return array[id];
		}

		throw new EntryMissingException(id);
	}

	@Override
	public void readPacket(PacketByteBuf buf, IndexedIterable<T> idList) {
		size = buf.readVarInt();

		for (int i = 0; i < size; i++) {
			array[i] = idList.getOrThrow(buf.readVarInt());
		}
	}

	@Override
	public void writePacket(PacketByteBuf buf, IndexedIterable<T> idList) {
		buf.writeVarInt(size);

		for (int i = 0; i < size; i++) {
			buf.writeVarInt(idList.getRawId(array[i]));
		}
	}

	@Override
	public int getPacketSize(IndexedIterable<T> idList) {
		int totalSize = VarInts.getSizeInBytes(getSize());

		for (int i = 0; i < getSize(); i++) {
			totalSize += VarInts.getSizeInBytes(idList.getRawId(array[i]));
		}

		return totalSize;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Palette<T> copy() {
		return new ArrayPalette<>((T[]) array.clone(), indexBits, size);
	}
}
