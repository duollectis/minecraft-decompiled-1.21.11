package net.minecraft.server.function;

import net.minecraft.command.SourcedCommandAction;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Исполняемая последовательность команд с идентификатором.
 * Реализуется как {@link ExpandedMacro} — финальный список действий после подстановки макросов.
 */
public interface Procedure<T> {

	Identifier id();

	List<SourcedCommandAction<T>> entries();
}
