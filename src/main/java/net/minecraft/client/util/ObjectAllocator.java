package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Стратегия выделения и освобождения объектов через {@link ClosableFactory}.
 * {@link #TRIVIAL} — простая реализация без пула: создаёт и сразу закрывает объекты.
 */
@Environment(EnvType.CLIENT)
public interface ObjectAllocator {

	ObjectAllocator TRIVIAL = new ObjectAllocator() {
		@Override
		public <T> T acquire(ClosableFactory<T> factory) {
			T object = factory.create();
			factory.prepare(object);
			return object;
		}

		@Override
		public <T> void release(ClosableFactory<T> factory, T value) {
			factory.close(value);
		}
	};

	<T> T acquire(ClosableFactory<T> factory);

	<T> void release(ClosableFactory<T> factory, T value);
}
