package net.minecraft.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.packrat.ParsingRule;
import net.minecraft.util.packrat.ParsingState;
import org.jspecify.annotations.Nullable;

/**
 * Правило парсинга, оборачивающее {@link StringNbtReader} в интерфейс {@link ParsingRule}.
 * <p>
 * Используется в системе packrat-парсинга для встраивания NBT-литералов
 * в более широкий контекст разбора команд или конфигурационных файлов.
 * При ошибке парсинга добавляет исключение в список ошибок состояния и возвращает {@code null}.
 *
 * @param <T> целевой тип данных, определяемый переданным {@link DynamicOps}
 */
public class NbtParsingRule<T> implements ParsingRule<StringReader, Dynamic<?>> {

	private final StringNbtReader<T> nbtReader;

	public NbtParsingRule(DynamicOps<T> ops) {
		nbtReader = StringNbtReader.fromOps(ops);
	}

	@Override
	public @Nullable Dynamic<T> parse(ParsingState<StringReader> parsingState) {
		parsingState.getReader().skipWhitespace();
		int cursorBeforeParse = parsingState.getCursor();

		try {
			return new Dynamic(nbtReader.getOps(), nbtReader.readAsArgument(parsingState.getReader()));
		}
		catch (Exception exception) {
			parsingState.getErrors().add(cursorBeforeParse, exception);
			return null;
		}
	}
}
