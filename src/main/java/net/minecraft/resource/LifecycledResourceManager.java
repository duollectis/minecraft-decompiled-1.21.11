package net.minecraft.resource;

/**
 * {@code LifecycledResourceManager}.
 */
public interface LifecycledResourceManager extends ResourceManager, AutoCloseable {

	@Override
	void close();
}
