package net.minecraft.client.gui.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Индикатор статуса сообщения чата: системное, небезопасное, изменённое или ошибочное.
 */
@Environment(EnvType.CLIENT)
public record MessageIndicator(
	int indicatorColor,
	MessageIndicator.@Nullable Icon icon,
	@Nullable Text text,
	@Nullable String loggedName
) {

	private static final Text SYSTEM_TEXT = Text.translatable("chat.tag.system");
	private static final Text SINGLE_PLAYER_TEXT = Text.translatable("chat.tag.system_single_player");
	private static final Text NOT_SECURE_TEXT = Text.translatable("chat.tag.not_secure");
	private static final Text MODIFIED_TEXT = Text.translatable("chat.tag.modified");
	private static final Text ERROR_TEXT = Text.translatable("chat.tag.error");
	private static final int NOT_SECURE_COLOR = 13684944;
	private static final int MODIFIED_COLOR = 6316128;
	private static final int CHAT_ERROR_COLOR = 16733525;
	private static final MessageIndicator SYSTEM = new MessageIndicator(NOT_SECURE_COLOR, null, SYSTEM_TEXT, "System");
	private static final MessageIndicator SINGLE_PLAYER = new MessageIndicator(NOT_SECURE_COLOR, null, SINGLE_PLAYER_TEXT, "System");
	private static final MessageIndicator NOT_SECURE = new MessageIndicator(NOT_SECURE_COLOR, null, NOT_SECURE_TEXT, "Not Secure");
	private static final MessageIndicator CHAT_ERROR = new MessageIndicator(CHAT_ERROR_COLOR, null, ERROR_TEXT, "Chat Error");

	public static MessageIndicator system() {
		return SYSTEM;
	}

	public static MessageIndicator singlePlayer() {
		return SINGLE_PLAYER;
	}

	public static MessageIndicator notSecure() {
		return NOT_SECURE;
	}

	public static MessageIndicator modified(String originalText) {
		Text original = Text.literal(originalText).formatted(Formatting.GRAY);
		Text tooltip = Text.empty().append(MODIFIED_TEXT).append(ScreenTexts.LINE_BREAK).append(original);
		return new MessageIndicator(MODIFIED_COLOR, MessageIndicator.Icon.CHAT_MODIFIED, tooltip, "Modified");
	}

	public static MessageIndicator chatError() {
		return CHAT_ERROR;
	}

	/**
	 * Иконка индикатора сообщения с текстурой и размерами для отрисовки.
	 */
	@Environment(EnvType.CLIENT)
	public enum Icon {
		CHAT_MODIFIED(Identifier.ofVanilla("icon/chat_modified"), 9, 9);

		public final Identifier texture;
		public final int width;
		public final int height;

		Icon(Identifier texture, int width, int height) {
			this.texture = texture;
			this.width = width;
			this.height = height;
		}

		public void draw(DrawContext context, int x, int y) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, width, height);
		}
	}
}
