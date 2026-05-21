package net.minecraft.client.search;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Comparator;
import java.util.Iterator;

@Environment(EnvType.CLIENT)
/**
 * {@code IdentifierSearchableIterator}.
 */
public class IdentifierSearchableIterator<T> extends AbstractIterator<T> {

	private final PeekingIterator<T> namespacesIterator;
	private final PeekingIterator<T> pathsIterator;
	private final Comparator<T> lastIndexComparator;

	public IdentifierSearchableIterator(
			Iterator<T> namespacesIterator,
			Iterator<T> pathsIterator,
			Comparator<T> lastIndexComparator
	) {
		this.namespacesIterator = Iterators.peekingIterator(namespacesIterator);
		this.pathsIterator = Iterators.peekingIterator(pathsIterator);
		this.lastIndexComparator = lastIndexComparator;
	}

	/**
	 * Вычисляет next.
	 *
	 * @return T — результат операции
	 */
	protected T computeNext() {
		while (this.namespacesIterator.hasNext() && this.pathsIterator.hasNext()) {
			int i = this.lastIndexComparator.compare((T) this.namespacesIterator.peek(), (T) this.pathsIterator.peek());
			if (i == 0) {
				this.pathsIterator.next();
				return (T) this.namespacesIterator.next();
			}

			if (i < 0) {
				this.namespacesIterator.next();
			}
			else {
				this.pathsIterator.next();
			}
		}

		return (T) this.endOfData();
	}
}
