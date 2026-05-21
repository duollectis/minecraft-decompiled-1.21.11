package net.minecraft.nbt;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@code AbstractNbtList}.
 */
public sealed interface AbstractNbtList extends Iterable<NbtElement>, NbtElement permits NbtList, NbtByteArray, NbtIntArray, NbtLongArray {

	void clear();

	boolean setElement(int index, NbtElement element);

	boolean addElement(int index, NbtElement element);

	NbtElement remove(int i);

	NbtElement get(int i);

	int size();

	default boolean isEmpty() {
		return this.size() == 0;
	}

	@Override
	default Iterator<NbtElement> iterator() {
		return new Iterator<NbtElement>() {
			private int current;

			@Override
			public boolean hasNext() {
				return this.current < AbstractNbtList.this.size();
			}

			public NbtElement next() {
				if (!this.hasNext()) {
					throw new NoSuchElementException();
				}
				else {
					return AbstractNbtList.this.get(this.current++);
				}
			}
		};
	}

	default Stream<NbtElement> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}
}
