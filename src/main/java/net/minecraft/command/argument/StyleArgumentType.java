package net.minecraft.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtParsing;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.packrat.Parser;

import java.util.Collection;
import java.util.List;

/**
 * {@code StyleArgumentType}.
 */
public class StyleArgumentType extends ParserBackedArgumentType<Style> {

	private static final Collection<String> EXAMPLES = List.of("{bold: true}", "{color: 'red'}", "{}");
	public static final DynamicCommandExceptionType INVALID_STYLE_EXCEPTION = new DynamicCommandExceptionType(
			style -> Text.stringifiedTranslatable("argument.style.invalid", style)
	);
	private static final DynamicOps<NbtElement> OPS = NbtOps.INSTANCE;
	private static final Parser<NbtElement> PARSER = SnbtParsing.createParser(OPS);

	private StyleArgumentType(RegistryWrapper.WrapperLookup registries) {
		super(PARSER.withDecoding(registries.getOps(OPS), PARSER, Style.Codecs.CODEC, INVALID_STYLE_EXCEPTION));
	}

	public static Style getStyle(CommandContext<ServerCommandSource> context, String style) {
		return (Style) context.getArgument(style, Style.class);
	}

	public static StyleArgumentType style(CommandRegistryAccess registryAccess) {
		return new StyleArgumentType(registryAccess);
	}

	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
