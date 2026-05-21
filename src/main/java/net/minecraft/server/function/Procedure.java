package net.minecraft.server.function;

import net.minecraft.command.SourcedCommandAction;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * {@code Procedure}.
 */
public interface Procedure<T> {

	Identifier id();

	List<SourcedCommandAction<T>> entries();
}
