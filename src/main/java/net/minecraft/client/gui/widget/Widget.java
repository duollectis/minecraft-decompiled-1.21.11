package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;

import java.util.function.Consumer;

/**
 * Базовый интерфейс всех GUI-виджетов клиента.
 * Определяет минимальный контракт: позиция, размеры, навигационный прямоугольник
 * и итерация по дочерним {@link ClickableWidget}-ам.
 *
 * <p>Расширяется {@link LayoutWidget} для контейнеров и
 * {@link ClickableWidget} для интерактивных элементов.</p>
 */
@Environment(EnvType.CLIENT)
public interface Widget {

	void setX(int x);

	void setY(int y);

	int getX();

	int getY();

	int getWidth();

	int getHeight();

	/**
	 * Возвращает прямоугольник виджета для клавиатурной навигации.
	 * По умолчанию совпадает с границами виджета.
	 */
	default ScreenRect getNavigationFocus() {
		return new ScreenRect(getX(), getY(), getWidth(), getHeight());
	}

	default void setPosition(int x, int y) {
		setX(x);
		setY(y);
	}

	/**
	 * Итерирует по всем дочерним {@link ClickableWidget}-ам этого виджета.
	 * Для листовых виджетов реализация пустая; для контейнеров — рекурсивная.
	 *
	 * @param consumer обработчик каждого дочернего виджета
	 */
	void forEachChild(Consumer<ClickableWidget> consumer);
}
