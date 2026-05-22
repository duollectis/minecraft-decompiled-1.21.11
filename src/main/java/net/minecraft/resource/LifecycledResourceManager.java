package net.minecraft.resource;

/**
 * {@link ResourceManager}, имеющий жизненный цикл с явным закрытием.
 * При закрытии освобождает все связанные ресурс-паки.
 */
public interface LifecycledResourceManager extends ResourceManager, AutoCloseable {

	@Override
	void close();
}
