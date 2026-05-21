package net.minecraft.util.function;

@FunctionalInterface
/**
 * {@code Object2FloatFunction}.
 */
public interface Object2FloatFunction<T> {

	float applyAsFloat(T object);
}
