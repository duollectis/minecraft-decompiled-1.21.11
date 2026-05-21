package net.minecraft.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtParsing;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.Texts;
import net.minecraft.util.packrat.Parser;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

/**
 * {@code TextArgumentType}.
 */
public class TextArgumentType extends ParserBackedArgumentType<Text> {

	private static final Collection<String>
			EXAMPLES =
			Arrays.asList("\"hello world\"", "'hello world'", "\"\"", "{text:\"hello world\"}", "[\"\"]");
	public static final DynamicCommandExceptionType INVALID_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(
			text -> Text.stringifiedTranslatable("argument.component.invalid", text)
	);
	private static final DynamicOps<NbtElement> OPS = NbtOps.INSTANCE;
	private static final Parser<NbtElement> PARSER = SnbtParsing.createParser(OPS);

	private TextArgumentType(RegistryWrapper.WrapperLookup registries) {
		super(PARSER.withDecoding(registries.getOps(OPS), PARSER, TextCodecs.CODEC, INVALID_COMPONENT_EXCEPTION));
	}

	public static Text getTextArgument(CommandContext<ServerCommandSource> context, String name) {
		return (Text) context.getArgument(name, Text.class);
	}

	public static Text parseTextArgument(
			CommandContext<ServerCommandSource> context,
			String name,
			@Nullable Entity sender
	) throws CommandSyntaxException {
		return Texts.parse((ServerCommandSource) context.getSource(), getTextArgument(context, name), sender, 0);
	}

	public static Text parseTextArgument(CommandContext<ServerCommandSource> context, String name)
	throws CommandSyntaxException {
		return parseTextArgument(context, name, ((ServerCommandSource) context.getSource()).getEntity());
	}

	public static TextArgumentType text(CommandRegistryAccess registryAccess) {
		return new TextArgumentType(registryAccess);
	}

	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
