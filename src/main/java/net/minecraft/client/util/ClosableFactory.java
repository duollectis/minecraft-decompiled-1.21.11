package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Фабрика с управляемым жизненным циклом создаваемых объектов.
 * Позволяет подготовить объект перед использованием и освободить ресурсы после.
 *
 * @param <T> тип создаваемого объекта
 */
@Environment(EnvType.CLIENT)
public interface ClosableFactory<T> {

	T create();

	default void prepare(T value) {
	}

	void close(T value);
}
