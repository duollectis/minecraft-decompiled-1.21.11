package net.minecraft.world.chunk;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.util.collection.IndexedIterable;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Палитра с единственным значением — оптимизация для однородных секций
 * (например, полностью воздушных или заполненных одним блоком).
 * Хранит ровно один элемент под индексом 0.
 */
public class SingularPalette<T> implements Palette<T> {

	private @Nullable T entry;

	public SingularPalette(List<T> idList) {
		if (idList.isEmpty()) {
			return;
		}

		Validate.isTrue(idList.size() <= 1, "Can't initialize SingleValuePalette with %d values.", idList.size());
		entry = idList.getFirst();
	}

	public static <A> Palette<A> create(int bitSize, List<A> idList) {
		return new SingularPalette<>(idList);
	}

	@Override
	public int index(T object, PaletteResizeListener<T> listener) {
		if (entry != null && entry != object) {
			return listener.onResize(1, object);
		}

		entry = object;
		return 0;
	}

	@Override
	public boolean hasAny(Predicate<T> predicate) {
		if (entry == null) {
			throw new IllegalStateException("Use of an uninitialized palette");
		}

		return predicate.test(entry);
	}

	@Override
	public T get(int id) {
		if (entry != null && id == 0) {
			return entry;
		}

		throw new IllegalStateException("Missing Palette entry for id " + id + ".");
	}

	@Override
	public void readPacket(PacketByteBuf buf, IndexedIterable<T> idList) {
		entry = idList.getOrThrow(buf.readVarInt());
	}

	@Override
	public void writePacket(PacketByteBuf buf, IndexedIterable<T> idList) {
		if (entry == null) {
			throw new IllegalStateException("Use of an uninitialized palette");
		}

		buf.writeVarInt(idList.getRawId(entry));
	}

	@Override
	public int getPacketSize(IndexedIterable<T> idList) {
		if (entry == null) {
			throw new IllegalStateException("Use of an uninitialized palette");
		}

		return VarInts.getSizeInBytes(idList.getRawId(entry));
	}

	@Override
	public int getSize() {
		return 1;
	}

	@Override
	public Palette<T> copy() {
		if (entry == null) {
			throw new IllegalStateException("Use of an uninitialized palette");
		}

		return this;
	}
}
