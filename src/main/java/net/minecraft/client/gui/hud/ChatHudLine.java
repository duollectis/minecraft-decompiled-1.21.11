package net.minecraft.client.gui.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Строка чата с метаданными: тик создания, подпись и индикатор сообщения.
 */
@Environment(EnvType.CLIENT)
public record ChatHudLine(
	int creationTick,
	Text content,
	@Nullable MessageSignatureData signature,
	@Nullable MessageIndicator indicator
) {

	private static final int PADDING = 4;
	private static final int ICON_MARGIN = 2;

	public List<OrderedText> breakLines(TextRenderer textRenderer, int width) {
		if (indicator != null && indicator.icon() != null) {
			width -= indicator.icon().width + PADDING + ICON_MARGIN;
		}

		return ChatMessages.breakRenderedChatMessageLines(content, width, textRenderer);
	}

	/**
	 * Видимая (отрендеренная) строка чата с временем добавления и флагом конца записи.
	 */
	@Environment(EnvType.CLIENT)
	public record Visible(
		int addedTime,
		OrderedText content,
		@Nullable MessageIndicator indicator,
		boolean endOfEntry
	) {

		public int getWidth(TextRenderer textRenderer) {
			return textRenderer.getWidth(content) + PADDING;
		}
	}
}
