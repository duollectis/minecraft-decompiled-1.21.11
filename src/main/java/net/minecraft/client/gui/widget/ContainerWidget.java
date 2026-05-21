package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ContainerWidget}.
 */
public abstract class ContainerWidget extends ScrollableWidget implements ParentElement {

	private @Nullable Element focusedElement;
	private boolean dragging;

	public ContainerWidget(int i, int j, int k, int l, Text text) {
		super(i, j, k, l, text);
	}

	@Override
	public final boolean isDragging() {
		return this.dragging;
	}

	@Override
	public final void setDragging(boolean dragging) {
		this.dragging = dragging;
	}

	@Override
	public @Nullable Element getFocused() {
		return this.focusedElement;
	}

	@Override
	public void setFocused(@Nullable Element focused) {
		if (this.focusedElement != null) {
			this.focusedElement.setFocused(false);
		}

		if (focused != null) {
			focused.setFocused(true);
		}

		this.focusedElement = focused;
	}

	@Override
	public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
		return ParentElement.super.getNavigationPath(navigation);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		boolean bl = this.checkScrollbarDragged(click);
		return ParentElement.super.mouseClicked(click, doubled) || bl;
	}

	@Override
	public boolean mouseReleased(Click click) {
		super.mouseReleased(click);
		return ParentElement.super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		super.mouseDragged(click, offsetX, offsetY);
		return ParentElement.super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean isFocused() {
		return ParentElement.super.isFocused();
	}

	@Override
	public void setFocused(boolean focused) {
		ParentElement.super.setFocused(focused);
	}
}
