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

/**
 * Тултип виджета: хранит текст содержимого и опциональный текст для нарратора.
 * Кэширует разбитые на строки {@link OrderedText} и инвалидирует кэш
 * при смене языка игры, чтобы корректно отображать переключение локализации.
 */
@Environment(EnvType.CLIENT)
public class Tooltip implements Narratable {

	/** Максимальная ширина строки тултипа в пикселях. */
	private static final int ROW_LENGTH = 170;

	private final Text content;
	private final @Nullable Text narration;
	private @Nullable List<OrderedText> lines;
	private @Nullable Language language;

	private Tooltip(Text content, @Nullable Text narration) {
		this.content = content;
		this.narration = narration;
	}

	/**
	 * Создаёт тултип с отдельным текстом для нарратора доступности.
	 *
	 * @param content   текст, отображаемый в тултипе
	 * @param narration текст для нарратора (может быть {@code null} — тогда нарратор молчит)
	 */
	public static Tooltip of(Text content, @Nullable Text narration) {
		return new Tooltip(content, narration);
	}

	/** Создаёт тултип, где текст нарратора совпадает с отображаемым текстом. */
	public static Tooltip of(Text content) {
		return new Tooltip(content, content);
	}

	@Override
	public void appendNarrations(NarrationMessageBuilder builder) {
		if (narration != null) {
			builder.put(NarrationPart.HINT, narration);
		}
	}

	/**
	 * Возвращает строки тултипа, разбитые по ширине {@value #ROW_LENGTH} пикселей.
	 * Кэш инвалидируется при смене активного языка игры.
	 *
	 * @param client экземпляр клиента для доступа к {@code textRenderer}
	 */
	public List<OrderedText> getLines(MinecraftClient client) {
		Language currentLanguage = Language.getInstance();
		if (lines == null || currentLanguage != language) {
			lines = wrapLines(client, content);
			language = currentLanguage;
		}

		return lines;
	}

	/**
	 * Разбивает текст на строки по ширине {@value #ROW_LENGTH} пикселей.
	 *
	 * @param client экземпляр клиента
	 * @param text   текст для разбивки
	 */
	public static List<OrderedText> wrapLines(MinecraftClient client, Text text) {
		return client.textRenderer.wrapLines(text, ROW_LENGTH);
	}
}
