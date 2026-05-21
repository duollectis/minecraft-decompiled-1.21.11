package net.minecraft.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
/**
 * {@code ArgumentGetter}.
 */
public interface ArgumentGetter<T, R> {

	R apply(T context) throws CommandSyntaxException;
}
