package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code LayoutWidgets}.
 */
public class LayoutWidgets {

	private static final int SPACING = 4;

	private LayoutWidgets() {
	}

	/**
	 * Создаёт labeled widget.
	 *
	 * @param textRenderer text renderer
	 * @param widget widget
	 * @param label label
	 *
	 * @return LayoutWidget — результат операции
	 */
	public static LayoutWidget createLabeledWidget(TextRenderer textRenderer, Widget widget, Text label) {
		return createLabeledWidget(textRenderer, widget, label, positioner -> {});
	}

	public static LayoutWidget createLabeledWidget(
			TextRenderer textRenderer,
			Widget widget,
			Text label,
			Consumer<Positioner> callback
	) {
		DirectionalLayoutWidget directionalLayoutWidget = DirectionalLayoutWidget.vertical().spacing(4);
		directionalLayoutWidget.add(new TextWidget(label, textRenderer));
		directionalLayoutWidget.add(widget, callback);
		return directionalLayoutWidget;
	}
}
