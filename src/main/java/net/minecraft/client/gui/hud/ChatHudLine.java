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

@Environment(EnvType.CLIENT)
/**
 * {@code ChatHudLine}.
 */
public record ChatHudLine(
		int creationTick,
		Text content,
		@Nullable MessageSignatureData signature,
		@Nullable MessageIndicator indicator
) {

	private static final int PADDING = 4;

	/**
	 * Ломает lines.
	 *
	 * @param textRenderer text renderer
	 * @param width width
	 *
	 * @return List — результат операции
	 */
	public List<OrderedText> breakLines(TextRenderer textRenderer, int width) {
		if (this.indicator != null && this.indicator.icon() != null) {
			width -= this.indicator.icon().width + 4 + 2;
		}

		return ChatMessages.breakRenderedChatMessageLines(this.content, width, textRenderer);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Visible}.
	 */
	public record Visible(
			int addedTime,
			OrderedText content,
			@Nullable MessageIndicator indicator,
			boolean endOfEntry
	) {

		public int getWidth(TextRenderer textRenderer) {
			return textRenderer.getWidth(this.content) + 4;
		}
	}
}
