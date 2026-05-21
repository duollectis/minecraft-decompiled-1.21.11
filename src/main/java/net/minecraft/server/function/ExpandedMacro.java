package net.minecraft.server.function;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * {@code ExpandedMacro}.
 */
public record ExpandedMacro<T>(
		Identifier id,
		List<SourcedCommandAction<T>> entries
) implements CommandFunction<T>, Procedure<T> {

	@Override
	public Procedure<T> withMacroReplaced(@Nullable NbtCompound arguments, CommandDispatcher<T> dispatcher)
	throws MacroException {
		return this;
	}
}
