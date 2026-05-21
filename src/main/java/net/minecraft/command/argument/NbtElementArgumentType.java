package net.minecraft.command.argument;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtParsing;
import net.minecraft.util.packrat.Parser;

import java.util.Arrays;
import java.util.Collection;

/**
 * {@code NbtElementArgumentType}.
 */
public class NbtElementArgumentType extends ParserBackedArgumentType<NbtElement> {

	private static final Collection<String>
			EXAMPLES =
			Arrays.asList("0", "0b", "0l", "0.0", "\"foo\"", "{foo=bar}", "[0]");
	private static final Parser<NbtElement> PARSER = SnbtParsing.createParser(NbtOps.INSTANCE);

	private NbtElementArgumentType() {
		super(PARSER);
	}

	public static NbtElementArgumentType nbtElement() {
		return new NbtElementArgumentType();
	}

	public static <S> NbtElement getNbtElement(CommandContext<S> context, String name) {
		return (NbtElement) context.getArgument(name, NbtElement.class);
	}

	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
