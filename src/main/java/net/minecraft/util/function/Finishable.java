package net.minecraft.util.function;

/**
 * Функциональный интерфейс для объектов, которые могут быть завершены
 * с указанием результата — успешно или нет.
 */
@FunctionalInterface
public interface Finishable {

	void finish(boolean success);
}
