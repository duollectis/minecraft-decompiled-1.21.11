package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Типизированный дескриптор ресурса с ленивым разыменованием.
 * Пустой дескриптор {@link #EMPTY} выбрасывает исключение при попытке получить значение.
 *
 * @param <T> тип хранимого ресурса
 */
@Environment(EnvType.CLIENT)
public interface Handle<T> {

	Handle<?> EMPTY = () -> {
		throw new IllegalStateException("Cannot dereference handle with no underlying resource");
	};

	@SuppressWarnings("unchecked")
	static <T> Handle<T> empty() {
		return (Handle<T>) EMPTY;
	}

	T get();
}
