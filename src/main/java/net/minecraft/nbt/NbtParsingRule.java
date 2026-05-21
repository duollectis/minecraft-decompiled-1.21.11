package net.minecraft.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.packrat.ParsingRule;
import net.minecraft.util.packrat.ParsingState;
import org.jspecify.annotations.Nullable;

/**
 * {@code NbtParsingRule}.
 */
public class NbtParsingRule<T> implements ParsingRule<StringReader, Dynamic<?>> {

	private final StringNbtReader<T> nbtReader;

	public NbtParsingRule(DynamicOps<T> ops) {
		this.nbtReader = StringNbtReader.fromOps(ops);
	}

	/**
	 * Parse.
	 *
	 * @param parsingState parsing state
	 *
	 * @return @Nullable Dynamic — результат операции
	 */
	public @Nullable Dynamic<T> parse(ParsingState<StringReader> parsingState) {
		parsingState.getReader().skipWhitespace();
		int i = parsingState.getCursor();

		try {
			return new Dynamic(this.nbtReader.getOps(), this.nbtReader.readAsArgument(parsingState.getReader()));
		}
		catch (Exception var4) {
			parsingState.getErrors().add(i, var4);
			return null;
		}
	}
}
