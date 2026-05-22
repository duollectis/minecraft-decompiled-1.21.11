package net.minecraft.util.packrat;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для правил разбора, которые сначала читают {@link Identifier},
 * а затем выполняют дополнительную обработку на основе этого идентификатора.
 * Обеспечивает единообразную обработку ошибок и автодополнение идентификаторов.
 */
public abstract class IdentifiableParsingRule<C, V> implements ParsingRule<StringReader, V>, IdentifierSuggestable {

	private final ParsingRuleEntry<StringReader, Identifier> idParsingRule;
	protected final C callbacks;
	private final CursorExceptionType<CommandSyntaxException> exception;

	protected IdentifiableParsingRule(ParsingRuleEntry<StringReader, Identifier> idParsingRule, C callbacks) {
		this.idParsingRule = idParsingRule;
		this.callbacks = callbacks;
		exception = CursorExceptionType.create(Identifier.COMMAND_EXCEPTION);
	}

	@Override
	public @Nullable V parse(ParsingState<StringReader> state) {
		state.getReader().skipWhitespace();

		int startCursor = state.getCursor();
		Identifier identifier = state.parse(idParsingRule);

		if (identifier == null) {
			state.getErrors().add(startCursor, this, exception);
			return null;
		}

		try {
			return parse((ImmutableStringReader) state.getReader(), identifier);
		} catch (Exception e) {
			state.getErrors().add(startCursor, this, e);
			return null;
		}
	}

	protected abstract V parse(ImmutableStringReader reader, Identifier id) throws Exception;
}
