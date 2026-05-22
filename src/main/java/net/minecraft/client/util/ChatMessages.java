package net.minecraft.client.util;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;

import java.util.List;
import java.util.Optional;

/**
 * Утилитарный класс для разбивки сообщений чата на строки с учётом ширины экрана.
 * Поддерживает визуальный отступ для строк-продолжений переноса.
 */
@Environment(EnvType.CLIENT)
public class ChatMessages {

	private static final OrderedText SPACES = OrderedText.styled(32, Style.EMPTY);

	private static String getRenderedChatMessage(String message) {
		return MinecraftClient.getInstance().options.getChatColors().getValue()
			? message
			: Formatting.strip(message);
	}

	/**
	 * Разбивает сообщение чата на строки с учётом ширины и настроек цвета.
	 * Строки, являющиеся продолжением переноса, предваряются пробелом для визуального отступа.
	 *
	 * @param message      исходное сообщение
	 * @param width        максимальная ширина строки в пикселях
	 * @param textRenderer рендерер текста для измерения ширины
	 * @return список строк {@link OrderedText} для отображения
	 */
	public static List<OrderedText> breakRenderedChatMessageLines(
		StringVisitable message,
		int width,
		TextRenderer textRenderer
	) {
		TextCollector textCollector = new TextCollector();
		message.visit(
			(style, text) -> {
				textCollector.add(StringVisitable.styled(getRenderedChatMessage(text), style));
				return Optional.empty();
			},
			Style.EMPTY
		);

		List<OrderedText> lines = Lists.newArrayList();
		textRenderer.getTextHandler().wrapLines(
			textCollector.getCombined(),
			width,
			Style.EMPTY,
			(text, lastLineWrapped) -> {
				OrderedText orderedText = Language.getInstance().reorder(text);
				lines.add(lastLineWrapped ? OrderedText.concat(SPACES, orderedText) : orderedText);
			}
		);

		return lines.isEmpty() ? Lists.newArrayList(OrderedText.EMPTY) : lines;
	}
}
