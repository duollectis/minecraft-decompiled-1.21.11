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

@Environment(EnvType.CLIENT)
/**
 * {@code MessageIndicator}.
 */
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
	private static final MessageIndicator SYSTEM = new MessageIndicator(13684944, null, SYSTEM_TEXT, "System");
	private static final MessageIndicator
			SINGLE_PLAYER =
			new MessageIndicator(13684944, null, SINGLE_PLAYER_TEXT, "System");
	private static final MessageIndicator
			NOT_SECURE =
			new MessageIndicator(13684944, null, NOT_SECURE_TEXT, "Not Secure");
	private static final MessageIndicator CHAT_ERROR = new MessageIndicator(16733525, null, ERROR_TEXT, "Chat Error");

	/**
	 * System.
	 *
	 * @return MessageIndicator — результат операции
	 */
	public static MessageIndicator system() {
		return SYSTEM;
	}

	/**
	 * Single player.
	 *
	 * @return MessageIndicator — результат операции
	 */
	public static MessageIndicator singlePlayer() {
		return SINGLE_PLAYER;
	}

	/**
	 * Not secure.
	 *
	 * @return MessageIndicator — результат операции
	 */
	public static MessageIndicator notSecure() {
		return NOT_SECURE;
	}

	/**
	 * Modified.
	 *
	 * @param originalText original text
	 *
	 * @return MessageIndicator — результат операции
	 */
	public static MessageIndicator modified(String originalText) {
		Text text = Text.literal(originalText).formatted(Formatting.GRAY);
		Text text2 = Text.empty().append(MODIFIED_TEXT).append(ScreenTexts.LINE_BREAK).append(text);
		return new MessageIndicator(6316128, MessageIndicator.Icon.CHAT_MODIFIED, text2, "Modified");
	}

	/**
	 * Chat error.
	 *
	 * @return MessageIndicator — результат операции
	 */
	public static MessageIndicator chatError() {
		return CHAT_ERROR;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Icon}.
	 */
	public static enum Icon {
		CHAT_MODIFIED(Identifier.ofVanilla("icon/chat_modified"), 9, 9);

		public final Identifier texture;
		public final int width;
		public final int height;

		private Icon(final Identifier texture, final int width, final int height) {
			this.texture = texture;
			this.width = width;
			this.height = height;
		}

		/**
		 * Draw.
		 *
		 * @param context context
		 * @param x x
		 * @param y y
		 */
		public void draw(DrawContext context, int x, int y) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, this.texture, x, y, this.width, this.height);
		}
	}
}
