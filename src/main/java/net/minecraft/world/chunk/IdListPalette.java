package net.minecraft.world.chunk;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.IndexedIterable;

import java.util.function.Predicate;

/**
 * Глобальная палитра — делегирует все операции глобальному реестру идентификаторов.
 * Используется для секций с большим числом уникальных значений (≥9 бит),
 * когда локальная палитра нецелесообразна. Всегда возвращает {@code true} из
 * {@link #hasAny}, так как реестр содержит все возможные значения.
 */
public class IdListPalette<T> implements Palette<T> {

	private final IndexedIterable<T> idList;

	public IdListPalette(IndexedIterable<T> idList) {
		this.idList = idList;
	}

	@Override
	public int index(T object, PaletteResizeListener<T> listener) {
		int id = idList.getRawId(object);
		return id == -1 ? 0 : id;
	}

	@Override
	public boolean hasAny(Predicate<T> predicate) {
		return true;
	}

	@Override
	public T get(int id) {
		T object = idList.get(id);
		if (object == null) {
			throw new EntryMissingException(id);
		}

		return object;
	}

	@Override
	public void readPacket(PacketByteBuf buf, IndexedIterable<T> idList) {
	}

	@Override
	public void writePacket(PacketByteBuf buf, IndexedIterable<T> idList) {
	}

	@Override
	public int getPacketSize(IndexedIterable<T> idList) {
		return 0;
	}

	@Override
	public int getSize() {
		return idList.size();
	}

	@Override
	public Palette<T> copy() {
		return this;
	}
}
