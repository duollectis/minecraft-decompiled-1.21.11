package net.minecraft.client.gui.tab;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Контракт вкладки экрана: заголовок, нарративная подсказка, дочерние виджеты и позиционирование.
 */
@Environment(EnvType.CLIENT)
public interface Tab {

	Text getTitle();

	Text getNarratedHint();

	void forEachChild(Consumer<ClickableWidget> consumer);

	void refreshGrid(ScreenRect tabArea);
}
