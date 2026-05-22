package net.minecraft.world.chunk;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.IndexedIterable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Палитра — отображение между компактными целочисленными идентификаторами
 * и реальными объектами (блок-стейтами, биомами и т.д.).
 * Используется внутри {@link PalettedContainer} для сжатого хранения данных секций.
 */
public interface Palette<T> {

	/**
	 * Возвращает числовой идентификатор объекта в палитре.
	 * Если объект отсутствует — добавляет его; при переполнении вызывает {@code listener}.
	 */
	int index(T object, PaletteResizeListener<T> listener);

	boolean hasAny(Predicate<T> predicate);

	T get(int id);

	void readPacket(PacketByteBuf buf, IndexedIterable<T> idList);

	void writePacket(PacketByteBuf buf, IndexedIterable<T> idList);

	int getPacketSize(IndexedIterable<T> idList);

	int getSize();

	Palette<T> copy();

	/** Фабрика для создания конкретных реализаций палитры. */
	interface Factory {

		<A> Palette<A> create(int bits, List<A> values);
	}
}
