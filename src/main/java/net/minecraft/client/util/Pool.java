package net.minecraft.client.util;

import com.google.common.annotations.VisibleForTesting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

/**
 * Пул объектов с ограниченным временем жизни.
 * Возвращённые объекты хранятся в пуле до истечения {@code lifespan} тиков,
 * после чего автоматически закрываются через {@link ClosableFactory#close}.
 */
@Environment(EnvType.CLIENT)
public class Pool implements ObjectAllocator, AutoCloseable {

	private final int lifespan;
	private final Deque<Entry<?>> entries = new ArrayDeque<>();

	public Pool(int lifespan) {
		this.lifespan = lifespan;
	}

	/**
	 * Уменьшает счётчик жизни каждой записи пула на 1.
	 * Записи с истёкшим сроком жизни закрываются и удаляются.
	 */
	public void decrementLifespan() {
		Iterator<? extends Entry<?>> iterator = entries.iterator();
		while (iterator.hasNext()) {
			Entry<?> entry = iterator.next();
			if (entry.lifespan-- == 0) {
				entry.close();
				iterator.remove();
			}
		}
	}

	@Override
	public <T> T acquire(ClosableFactory<T> factory) {
		T object = acquireUnprepared(factory);
		factory.prepare(object);
		return object;
	}

	@SuppressWarnings("unchecked")
	private <T> T acquireUnprepared(ClosableFactory<T> factory) {
		Iterator<? extends Entry<?>> iterator = entries.iterator();
		while (iterator.hasNext()) {
			Entry<?> entry = iterator.next();
			if (factory.equals(entry.factory)) {
				iterator.remove();
				return (T) entry.object;
			}
		}

		return factory.create();
	}

	@Override
	public <T> void release(ClosableFactory<T> factory, T value) {
		entries.addFirst(new Entry<>(factory, value, lifespan));
	}

	public void clear() {
		entries.forEach(Entry::close);
		entries.clear();
	}

	@Override
	public void close() {
		clear();
	}

	@VisibleForTesting
	protected Collection<Entry<?>> getEntries() {
		return entries;
	}

	@Environment(EnvType.CLIENT)
	@VisibleForTesting
	protected static final class Entry<T> implements AutoCloseable {

		final ClosableFactory<T> factory;
		final T object;
		int lifespan;

		Entry(ClosableFactory<T> factory, T object, int lifespan) {
			this.factory = factory;
			this.object = object;
			this.lifespan = lifespan;
		}

		@Override
		public void close() {
			factory.close(object);
		}
	}
}
