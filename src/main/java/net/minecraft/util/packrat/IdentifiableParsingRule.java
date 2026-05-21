package net.minecraft.util.packrat;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * {@code IdentifiableParsingRule}.
 */
public abstract class IdentifiableParsingRule<C, V> implements ParsingRule<StringReader, V>, IdentifierSuggestable {

	private final ParsingRuleEntry<StringReader, Identifier> idParsingRule;
	protected final C callbacks;
	private final CursorExceptionType<CommandSyntaxException> exception;

	protected IdentifiableParsingRule(ParsingRuleEntry<StringReader, Identifier> idParsingRule, C callbacks) {
		this.idParsingRule = idParsingRule;
		this.callbacks = callbacks;
		this.exception = CursorExceptionType.create(Identifier.COMMAND_EXCEPTION);
	}

	@Override
	public @Nullable V parse(ParsingState<StringReader> state) {
		state.getReader().skipWhitespace();
		int i = state.getCursor();
		Identifier identifier = state.parse(this.idParsingRule);
		if (identifier != null) {
			try {
				return this.parse((ImmutableStringReader) state.getReader(), identifier);
			}
			catch (Exception var5) {
				state.getErrors().add(i, this, var5);
				return null;
			}
		}
		else {
			state.getErrors().add(i, this, this.exception);
			return null;
		}
	}

	protected abstract V parse(ImmutableStringReader reader, Identifier id) throws Exception;
}
