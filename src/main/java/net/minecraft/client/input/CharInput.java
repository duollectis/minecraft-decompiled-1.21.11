package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringHelper;

/**
 * Событие ввода символа с клавиатуры: Unicode-кодпоинт и битовая маска модификаторов.
 * Используется для обработки текстового ввода в виджетах (текстовые поля, чат).
 */
@Environment(EnvType.CLIENT)
public record CharInput(int codepoint, @AbstractInput.Modifier int modifiers) {

	public String asString() {
		return Character.toString(codepoint);
	}

	public boolean isValidChar() {
		return StringHelper.isValidChar(codepoint);
	}
}
