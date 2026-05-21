package net.minecraft.resource;

import java.util.function.Consumer;

@FunctionalInterface
/**
 * {@code ResourcePackProvider}.
 */
public interface ResourcePackProvider {

	void register(Consumer<ResourcePackProfile> profileAdder);
}
