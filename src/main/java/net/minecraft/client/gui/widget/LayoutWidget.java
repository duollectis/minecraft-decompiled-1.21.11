package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Consumer;

/**
 * Интерфейс виджета-контейнера, управляющего позиционированием дочерних элементов.
 * Расширяет {@link Widget} возможностью итерации по всем дочерним {@link Widget}-ам
 * и рекурсивного обновления их позиций.
 *
 * <p>Реализации: {@link GridWidget}, {@link DirectionalLayoutWidget},
 * {@link SimplePositioningWidget} и другие.</p>
 */
@Environment(EnvType.CLIENT)
public interface LayoutWidget extends Widget {

	/**
	 * Итерирует по всем непосредственным дочерним элементам лейаута.
	 *
	 * @param consumer обработчик каждого дочернего виджета
	 */
	void forEachElement(Consumer<Widget> consumer);

	@Override
	default void forEachChild(Consumer<ClickableWidget> consumer) {
		forEachElement(element -> element.forEachChild(consumer));
	}

	/**
	 * Рекурсивно обновляет позиции всех вложенных {@link LayoutWidget}-ов.
	 * Вызывается после изменения размеров или позиции контейнера.
	 */
	default void refreshPositions() {
		forEachElement(element -> {
			if (element instanceof LayoutWidget layoutWidget) {
				layoutWidget.refreshPositions();
			}
		});
	}
}
