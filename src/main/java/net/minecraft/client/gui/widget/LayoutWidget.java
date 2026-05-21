package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code LayoutWidget}.
 */
public interface LayoutWidget extends Widget {

	void forEachElement(Consumer<Widget> consumer);

	@Override
	default void forEachChild(Consumer<ClickableWidget> consumer) {
		this.forEachElement(element -> element.forEachChild(consumer));
	}

	default void refreshPositions() {
		this.forEachElement(element -> {
			if (element instanceof LayoutWidget layoutWidget) {
				layoutWidget.refreshPositions();
			}
		});
	}
}
