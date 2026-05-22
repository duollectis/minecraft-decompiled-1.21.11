package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.navigation.Navigable;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.jspecify.annotations.Nullable;

/**
 * Базовый интерфейс для всех интерактивных элементов GUI.
 * Обрабатывает события мыши, клавиатуры и навигации.
 */
@Environment(EnvType.CLIENT)
public interface Element extends Navigable {

	default void mouseMoved(double mouseX, double mouseY) {
	}

	default boolean mouseClicked(Click click, boolean doubled) {
		return false;
	}

	default boolean mouseReleased(Click click) {
		return false;
	}

	default boolean mouseDragged(Click click, double offsetX, double offsetY) {
		return false;
	}

	default boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		return false;
	}

	default boolean keyPressed(KeyInput input) {
		return false;
	}

	default boolean keyReleased(KeyInput input) {
		return false;
	}

	default boolean charTyped(CharInput input) {
		return false;
	}

	default @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
		return null;
	}

	default boolean isMouseOver(double mouseX, double mouseY) {
		return false;
	}

	void setFocused(boolean focused);

	boolean isFocused();

	default boolean isClickable() {
		return true;
	}

	default @Nullable GuiNavigationPath getFocusedPath() {
		return this.isFocused() ? GuiNavigationPath.of(this) : null;
	}

	default ScreenRect getNavigationFocus() {
		return ScreenRect.empty();
	}

	default ScreenRect getBorder(NavigationDirection direction) {
		return this.getNavigationFocus().getBorder(direction);
	}
}
