package net.minecraft.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface ArgumentGetter<T, R> {
   R apply(T context) throws CommandSyntaxException;
}
