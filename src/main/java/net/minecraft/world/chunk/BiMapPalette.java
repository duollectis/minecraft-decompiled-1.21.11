package net.minecraft.world.chunk;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.collection.Int2ObjectBiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Палитра на основе двунаправленного отображения {@link Int2ObjectBiMap}.
 * Используется для средних секций (5–8 бит, до 256 уникальных значений).
 * Обеспечивает O(1) поиск как по объекту, так и по идентификатору.
 */
public class BiMapPalette<T> implements Palette<T> {

	private final Int2ObjectBiMap<T> map;
	private final int indexBits;

	public BiMapPalette(int indexBits, List<T> values) {
		this(indexBits);
		values.forEach(map::add);
	}

	public BiMapPalette(int indexBits) {
		this(indexBits, Int2ObjectBiMap.create(1 << indexBits));
	}

	private BiMapPalette(int indexBits, Int2ObjectBiMap<T> map) {
		this.indexBits = indexBits;
		this.map = map;
	}

	public static <A> Palette<A> create(int bits, List<A> values) {
		return new BiMapPalette<>(bits, values);
	}

	@Override
	public int index(T object, PaletteResizeListener<T> listener) {
		int id = map.getRawId(object);
		if (id == -1) {
			id = map.add(object);
			if (id >= 1 << indexBits) {
				id = listener.onResize(indexBits + 1, object);
			}
		}

		return id;
	}

	@Override
	public boolean hasAny(Predicate<T> predicate) {
		for (int i = 0; i < getSize(); i++) {
			if (predicate.test(map.get(i))) {
				return true;
			}
		}

		return false;
	}

	@Override
	public T get(int id) {
		T object = map.get(id);
		if (object == null) {
			throw new EntryMissingException(id);
		}

		return object;
	}

	@Override
	public void readPacket(PacketByteBuf buf, IndexedIterable<T> idList) {
		map.clear();
		int count = buf.readVarInt();

		for (int i = 0; i < count; i++) {
			map.add(idList.getOrThrow(buf.readVarInt()));
		}
	}

	@Override
	public void writePacket(PacketByteBuf buf, IndexedIterable<T> idList) {
		int count = getSize();
		buf.writeVarInt(count);

		for (int i = 0; i < count; i++) {
			buf.writeVarInt(idList.getRawId(map.get(i)));
		}
	}

	@Override
	public int getPacketSize(IndexedIterable<T> idList) {
		int totalSize = VarInts.getSizeInBytes(getSize());

		for (int i = 0; i < getSize(); i++) {
			totalSize += VarInts.getSizeInBytes(idList.getRawId(map.get(i)));
		}

		return totalSize;
	}

	public List<T> getElements() {
		List<T> elements = new ArrayList<>();
		map.iterator().forEachRemaining(elements::add);
		return elements;
	}

	@Override
	public int getSize() {
		return map.size();
	}

	@Override
	public Palette<T> copy() {
		return new BiMapPalette<>(indexBits, map.copy());
	}
}
