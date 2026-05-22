package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.item.tooltip.TooltipData;

import java.util.List;

/**
 * Компонент тултипа для отображения списка игровых профилей (скинов).
 * Используется, например, в тултипе книги с подписями или предметов,
 * связанных с конкретными игроками. Каждая строка содержит иконку скина и имя.
 */
@Environment(EnvType.CLIENT)
public class ProfilesTooltipComponent implements TooltipComponent {

	private static final int PROFILE_ICON_SIZE = 10;
	private static final int PROFILE_ROW_HEIGHT = 12;
	/** Горизонтальный отступ иконки от левого края компонента. */
	private static final int ICON_LEFT_PADDING = 2;
	/** Вертикальный отступ первой строки от верхнего края компонента. */
	private static final int ROWS_TOP_PADDING = 2;
	/** Отступ текста имени от правого края иконки. */
	private static final int TEXT_LEFT_PADDING = 4;
	/** Вертикальный отступ текста внутри строки. */
	private static final int TEXT_TOP_PADDING = 2;
	/** Дополнительный горизонтальный зазор между иконкой и правым краем компонента. */
	private static final int WIDTH_EXTRA_PADDING = 6;
	/** Цвет текста имени профиля (белый). */
	private static final int TEXT_COLOR = -1;

	private final List<PlayerSkinCache.Entry> profiles;

	public ProfilesTooltipComponent(ProfilesData data) {
		profiles = data.profiles();
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return profiles.size() * PROFILE_ROW_HEIGHT + ROWS_TOP_PADDING;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		int maxWidth = 0;

		for (PlayerSkinCache.Entry entry : profiles) {
			int nameWidth = textRenderer.getWidth(entry.getProfile().name());
			if (nameWidth > maxWidth) {
				maxWidth = nameWidth;
			}
		}

		return maxWidth + PROFILE_ICON_SIZE + WIDTH_EXTRA_PADDING;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		for (int i = 0; i < profiles.size(); i++) {
			PlayerSkinCache.Entry entry = profiles.get(i);
			int entryY = y + ROWS_TOP_PADDING + i * PROFILE_ROW_HEIGHT;
			PlayerSkinDrawer.draw(context, entry.getTextures(), x + ICON_LEFT_PADDING, entryY, PROFILE_ICON_SIZE);
			context.drawTextWithShadow(
				textRenderer,
				entry.getProfile().name(),
				x + PROFILE_ICON_SIZE + TEXT_LEFT_PADDING,
				entryY + TEXT_TOP_PADDING,
				TEXT_COLOR
			);
		}
	}

	/** Данные для создания компонента тултипа профилей. */
	@Environment(EnvType.CLIENT)
	public record ProfilesData(List<PlayerSkinCache.Entry> profiles) implements TooltipData {
	}
}
