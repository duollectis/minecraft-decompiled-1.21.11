package net.minecraft.world.chunk;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.util.collection.IndexedIterable;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.function.Predicate;

/**
 * {@code ArrayPalette}.
 */
public class ArrayPalette<T> implements Palette<T> {

	private final T[] array;
	private final int indexBits;
	private int size;

	private ArrayPalette(int indexBits, List<T> values) {
		this.array = (T[]) (new Object[1 << indexBits]);
		this.indexBits = indexBits;
		Validate.isTrue(
				values.size() <= this.array.length,
				"Can't initialize LinearPalette of size %d with %d entries",
				new Object[]{this.array.length, values.size()}
		);

		for (int i = 0; i < values.size(); i++) {
			this.array[i] = values.get(i);
		}

		this.size = values.size();
	}

	private ArrayPalette(T[] array, int indexBits, int size) {
		this.array = array;
		this.indexBits = indexBits;
		this.size = size;
	}

	/**
	 * Create.
	 *
	 * @param bits bits
	 * @param values values
	 *
	 * @return Palette — результат операции
	 */
	public static <A> Palette<A> create(int bits, List<A> values) {
		return new ArrayPalette<>(bits, values);
	}

	@Override
	public int index(T object, PaletteResizeListener<T> listener) {
		for (int i = 0; i < this.size; i++) {
			if (this.array[i] == object) {
				return i;
			}
		}

		int ix = this.size;
		if (ix < this.array.length) {
			this.array[ix] = object;
			this.size++;
			return ix;
		}
		else {
			return listener.onResize(this.indexBits + 1, object);
		}
	}

	@Override
	public boolean hasAny(Predicate<T> predicate) {
		for (int i = 0; i < this.size; i++) {
			if (predicate.test(this.array[i])) {
				return true;
			}
		}

		return false;
	}

	@Override
	public T get(int id) {
		if (id >= 0 && id < this.size) {
			return this.array[id];
		}
		else {
			throw new EntryMissingException(id);
		}
	}

	@Override
	public void readPacket(PacketByteBuf buf, IndexedIterable<T> idList) {
		this.size = buf.readVarInt();

		for (int i = 0; i < this.size; i++) {
			this.array[i] = idList.getOrThrow(buf.readVarInt());
		}
	}

	@Override
	public void writePacket(PacketByteBuf buf, IndexedIterable<T> idList) {
		buf.writeVarInt(this.size);

		for (int i = 0; i < this.size; i++) {
			buf.writeVarInt(idList.getRawId(this.array[i]));
		}
	}

	@Override
	public int getPacketSize(IndexedIterable<T> idList) {
		int i = VarInts.getSizeInBytes(this.getSize());

		for (int j = 0; j < this.getSize(); j++) {
			i += VarInts.getSizeInBytes(idList.getRawId(this.array[j]));
		}

		return i;
	}

	@Override
	public int getSize() {
		return this.size;
	}

	@Override
	public Palette<T> copy() {
		return new ArrayPalette<>((T[]) ((Object[]) this.array.clone()), this.indexBits, this.size);
	}
}
