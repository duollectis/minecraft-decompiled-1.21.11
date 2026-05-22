package net.minecraft.resource;

import java.util.function.Consumer;

/**
 * Провайдер ресурс-паков: сканирует доступные паки и регистрирует их через {@code profileAdder}.
 */
@FunctionalInterface
public interface ResourcePackProvider {

	void register(Consumer<ResourcePackProfile> profileAdder);
}
