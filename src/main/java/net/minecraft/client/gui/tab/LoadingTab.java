package net.minecraft.client.gui.tab;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.LoadingWidget;
import net.minecraft.client.gui.widget.SimplePositioningWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Вкладка экрана, отображающая индикатор загрузки вместо обычного содержимого.
 * Используется при асинхронной загрузке данных (например, список серверов, скины).
 * Виджет загрузки центрируется в области вкладки с небольшим отступом снизу.
 */
@Environment(EnvType.CLIENT)
public class LoadingTab implements Tab {

	/** Нижний отступ виджета загрузки для визуального смещения от центра вверх. */
	private static final int LOADING_WIDGET_BOTTOM_MARGIN = 30;
	/** Горизонтальное и вертикальное выравнивание по центру области вкладки. */
	private static final float CENTER_ALIGNMENT = 0.5F;

	private final Text title;
	private final Text narratedHint;
	protected final DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical();

	/**
	 * Создаёт вкладку загрузки с центрированным индикатором.
	 *
	 * @param textRenderer  рендерер текста для виджета загрузки
	 * @param title         заголовок вкладки (отображается на кнопке вкладки)
	 * @param narratedHint  текст для нарратора доступности
	 */
	public LoadingTab(TextRenderer textRenderer, Text title, Text narratedHint) {
		this.title = title;
		this.narratedHint = narratedHint;

		LoadingWidget loadingWidget = new LoadingWidget(textRenderer, narratedHint);
		layout.getMainPositioner().alignVerticalCenter().alignHorizontalCenter();
		layout.add(loadingWidget, positioner -> positioner.marginBottom(LOADING_WIDGET_BOTTOM_MARGIN));
	}

	@Override
	public Text getTitle() {
		return title;
	}

	@Override
	public Text getNarratedHint() {
		return narratedHint;
	}

	@Override
	public void forEachChild(Consumer<ClickableWidget> consumer) {
		layout.forEachChild(consumer);
	}

	@Override
	public void refreshGrid(ScreenRect tabArea) {
		layout.refreshPositions();
		SimplePositioningWidget.setPos(layout, tabArea, CENTER_ALIGNMENT, CENTER_ALIGNMENT);
	}
}
