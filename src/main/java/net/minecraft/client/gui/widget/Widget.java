package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code Widget}.
 */
public interface Widget {

	void setX(int x);

	void setY(int y);

	int getX();

	int getY();

	int getWidth();

	int getHeight();

	default ScreenRect getNavigationFocus() {
		return new ScreenRect(this.getX(), this.getY(), this.getWidth(), this.getHeight());
	}

	default void setPosition(int x, int y) {
		this.setX(x);
		this.setY(y);
	}

	void forEachChild(Consumer<ClickableWidget> consumer);
}
