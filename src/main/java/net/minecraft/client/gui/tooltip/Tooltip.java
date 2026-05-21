package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Narratable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code Tooltip}.
 */
public class Tooltip implements Narratable {

	private static final int ROW_LENGTH = 170;
	private final Text content;
	private @Nullable List<OrderedText> lines;
	private @Nullable Language language;
	private final @Nullable Text narration;

	private Tooltip(Text content, @Nullable Text narration) {
		this.content = content;
		this.narration = narration;
	}

	/**
	 * Of.
	 *
	 * @param content content
	 * @param narration narration
	 *
	 * @return Tooltip — результат операции
	 */
	public static Tooltip of(Text content, @Nullable Text narration) {
		return new Tooltip(content, narration);
	}

	/**
	 * Of.
	 *
	 * @param content content
	 *
	 * @return Tooltip — результат операции
	 */
	public static Tooltip of(Text content) {
		return new Tooltip(content, content);
	}

	@Override
	public void appendNarrations(NarrationMessageBuilder builder) {
		if (this.narration != null) {
			builder.put(NarrationPart.HINT, this.narration);
		}
	}

	public List<OrderedText> getLines(MinecraftClient client) {
		Language language = Language.getInstance();
		if (this.lines == null || language != this.language) {
			this.lines = wrapLines(client, this.content);
			this.language = language;
		}

		return this.lines;
	}

	/**
	 * Wrap lines.
	 *
	 * @param client client
	 * @param text text
	 *
	 * @return List — результат операции
	 */
	public static List<OrderedText> wrapLines(MinecraftClient client, Text text) {
		return client.textRenderer.wrapLines(text, 170);
	}
}
