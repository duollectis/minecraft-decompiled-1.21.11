package net.minecraft.util.function;

@FunctionalInterface
/**
 * {@code Finishable}.
 */
public interface Finishable {

	void finish(boolean success);
}
