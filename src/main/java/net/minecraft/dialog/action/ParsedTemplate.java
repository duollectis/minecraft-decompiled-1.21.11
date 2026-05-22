package net.minecraft.dialog.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.command.MacroInvocation;

import java.util.List;
import java.util.Map;

/**
 * Разобранный шаблон строки с переменными для подстановки.
 * <p>
 * Используется в {@link DynamicRunCommandDialogAction} для формирования команды
 * с подстановкой значений полей ввода диалога.
 * Переменные в шаблоне должны соответствовать именам полей ввода.
 */
public class ParsedTemplate {

	public static final Codec<ParsedTemplate> CODEC = Codec.STRING.comapFlatMap(
		ParsedTemplate::parse,
		template -> template.raw
	);

	/**
	 * Кодек для валидации имён переменных шаблона.
	 * Имя должно быть допустимым именем макроса ({@link MacroInvocation#isValidMacroName}).
	 */
	public static final Codec<String> NAME_CODEC = Codec.STRING.validate(
		name -> MacroInvocation.isValidMacroName(name)
			? DataResult.success(name)
			: DataResult.error(() -> name + " is not a valid input name")
	);

	private final String raw;
	private final MacroInvocation parsed;

	private ParsedTemplate(String raw, MacroInvocation parsed) {
		this.raw = raw;
		this.parsed = parsed;
	}

	private static DataResult<ParsedTemplate> parse(String raw) {
		try {
			MacroInvocation macro = MacroInvocation.parse(raw);
			return DataResult.success(new ParsedTemplate(raw, macro));
		} catch (Exception cause) {
			return DataResult.error(() -> "Failed to parse template " + raw + ": " + cause.getMessage());
		}
	}

	/**
	 * Применяет шаблон, подставляя значения переменных из переданной карты.
	 * Переменные, отсутствующие в карте, заменяются пустой строкой.
	 *
	 * @param args карта имён переменных к их строковым значениям
	 * @return результирующая строка с подставленными значениями
	 */
	public String apply(Map<String, String> args) {
		List<String> values = parsed.variables()
			.stream()
			.map(variable -> args.getOrDefault(variable, ""))
			.toList();
		return parsed.apply(values);
	}
}
