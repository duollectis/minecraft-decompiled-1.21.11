package net.minecraft.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * Функциональный интерфейс для извлечения аргумента из контекста команды.
 *
 * @param <T> тип контекста команды
 * @param <R> тип возвращаемого аргумента
 */
@FunctionalInterface
public interface ArgumentGetter<T, R> {

	R apply(T context) throws CommandSyntaxException;
}
