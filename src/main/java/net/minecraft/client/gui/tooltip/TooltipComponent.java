package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.tooltip.BundleTooltipData;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.OrderedText;

/**
 * Интерфейс компонента тултипа — единица отображения в составном тултипе предмета.
 * Каждый компонент отвечает за свои размеры и отрисовку.
 *
 * <p>Стандартные реализации:
 * <ul>
 *   <li>{@link OrderedTextTooltipComponent} — текстовая строка</li>
 *   <li>{@link BundleTooltipComponent} — содержимое сумки</li>
 *   <li>{@link ProfilesTooltipComponent} — список игровых профилей</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public interface TooltipComponent {

	/** Создаёт текстовый компонент из {@link OrderedText}. */
	static TooltipComponent of(OrderedText text) {
		return new OrderedTextTooltipComponent(text);
	}

	/**
	 * Создаёт компонент из данных тултипа предмета.
	 * Поддерживаемые типы: {@link BundleTooltipData}, {@link ProfilesTooltipComponent.ProfilesData}.
	 *
	 * @throws IllegalArgumentException если тип {@code tooltipData} не поддерживается
	 */
	static TooltipComponent of(TooltipData tooltipData) {
		return switch (tooltipData) {
			case BundleTooltipData bundleTooltipData -> new BundleTooltipComponent(bundleTooltipData.contents());
			case ProfilesTooltipComponent.ProfilesData profilesData -> new ProfilesTooltipComponent(profilesData);
			default -> throw new IllegalArgumentException("Unknown TooltipComponent");
		};
	}

	int getHeight(TextRenderer textRenderer);

	int getWidth(TextRenderer textRenderer);

	/** Возвращает {@code true}, если тултип должен оставаться видимым при движении мыши. */
	default boolean isSticky() {
		return false;
	}

	default void drawText(DrawContext context, TextRenderer textRenderer, int x, int y) {
	}

	default void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
	}
}
