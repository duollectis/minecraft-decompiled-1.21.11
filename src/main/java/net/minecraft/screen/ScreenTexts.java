package net.minecraft.screen;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

import java.util.Arrays;
import java.util.Collection;

/**
 * Утилитарный класс с предзагруженными текстовыми константами для стандартных элементов GUI.
 * <p>
 * Все константы — транслируемые тексты, загружаемые один раз при инициализации класса.
 * Методы-утилиты предоставляют удобные способы форматирования и объединения текстов.
 */
public class ScreenTexts {

	public static final Text EMPTY = Text.empty();
	public static final Text ON = Text.translatable("options.on");
	public static final Text OFF = Text.translatable("options.off");
	public static final Text DONE = Text.translatable("gui.done");
	public static final Text CANCEL = Text.translatable("gui.cancel");
	public static final Text YES = Text.translatable("gui.yes");
	public static final Text NO = Text.translatable("gui.no");
	public static final Text OK = Text.translatable("gui.ok");
	public static final Text PROCEED = Text.translatable("gui.proceed");
	public static final Text CONTINUE = Text.translatable("gui.continue");
	public static final Text BACK = Text.translatable("gui.back");
	public static final Text TO_TITLE = Text.translatable("gui.toTitle");
	public static final Text ACKNOWLEDGE = Text.translatable("gui.acknowledge");
	public static final Text OPEN_LINK = Text.translatable("chat.link.open");
	public static final Text COPY = Text.translatable("chat.copy");
	public static final Text COPY_LINK_TO_CLIPBOARD = Text.translatable("gui.copy_link_to_clipboard");
	public static final Text DISCONNECT = Text.translatable("menu.disconnect");
	public static final Text RETURN_TO_MENU = Text.translatable("menu.returnToMenu");
	public static final Text CONNECT_FAILED_TRANSFER = Text.translatable("connect.failed.transfer");
	public static final Text CONNECT_FAILED = Text.translatable("connect.failed");
	public static final Text LINE_BREAK = Text.literal("\n");
	public static final Text SENTENCE_SEPARATOR = Text.literal(". ");
	public static final Text ELLIPSIS = Text.literal("...");
	public static final Text SPACE = space();

	public static MutableText space() {
		return Text.literal(" ");
	}

	public static MutableText days(long days) {
		return Text.translatable("gui.days", days);
	}

	public static MutableText hours(long hours) {
		return Text.translatable("gui.hours", hours);
	}

	public static MutableText minutes(long minutes) {
		return Text.translatable("gui.minutes", minutes);
	}

	public static Text onOrOff(boolean on) {
		return on ? ON : OFF;
	}

	public static Text returnToMenuOrDisconnect(boolean singleplayer) {
		return singleplayer ? RETURN_TO_MENU : DISCONNECT;
	}

	public static MutableText composeToggleText(Text text, boolean value) {
		return Text.translatable(value ? "options.on.composed" : "options.off.composed", text);
	}

	public static MutableText composeGenericOptionText(Text text, Text value) {
		return Text.translatable("options.generic_value", text, value);
	}

	/**
	 * Объединяет предложения через разделитель {@link #SENTENCE_SEPARATOR} (". ").
	 * Последнее предложение разделителем не завершается.
	 *
	 * @param sentences предложения для объединения
	 * @return объединённый изменяемый текст
	 */
	public static MutableText joinSentences(Text... sentences) {
		MutableText result = Text.empty();

		for (int index = 0; index < sentences.length; index++) {
			result.append(sentences[index]);

			if (index != sentences.length - 1) {
				result.append(SENTENCE_SEPARATOR);
			}
		}

		return result;
	}

	public static Text joinLines(Text... texts) {
		return joinLines(Arrays.asList(texts));
	}

	public static Text joinLines(Collection<? extends Text> texts) {
		return Texts.join(texts, LINE_BREAK);
	}
}
