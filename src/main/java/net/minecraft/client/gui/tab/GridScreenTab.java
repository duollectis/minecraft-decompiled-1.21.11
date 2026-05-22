package net.minecraft.client.gui.tab;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.SimplePositioningWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Реализация вкладки экрана на основе {@link GridWidget}.
 * Позиционирует содержимое по центру горизонтально и на 1/6 высоты вертикально,
 * что соответствует стандартному расположению элементов настроек в Minecraft.
 */
@Environment(EnvType.CLIENT)
public class GridScreenTab implements Tab {

	/** Вертикальное смещение сетки относительно области вкладки (1/6 высоты). */
	private static final float VERTICAL_ALIGNMENT = 1.0F / 6.0F;
	/** Горизонтальное смещение сетки — по центру. */
	private static final float HORIZONTAL_ALIGNMENT = 0.5F;

	private final Text title;
	protected final GridWidget grid = new GridWidget();

	public GridScreenTab(Text title) {
		this.title = title;
	}

	@Override
	public Text getTitle() {
		return title;
	}

	@Override
	public Text getNarratedHint() {
		return Text.empty();
	}

	@Override
	public void forEachChild(Consumer<ClickableWidget> consumer) {
		grid.forEachChild(consumer);
	}

	@Override
	public void refreshGrid(ScreenRect tabArea) {
		grid.refreshPositions();
		SimplePositioningWidget.setPos(grid, tabArea, HORIZONTAL_ALIGNMENT, VERTICAL_ALIGNMENT);
	}
}
