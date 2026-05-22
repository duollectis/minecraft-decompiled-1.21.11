package net.minecraft.client.gui.navigation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import org.jspecify.annotations.Nullable;

/**
 * Путь навигации по дереву GUI-элементов от корня до конечного фокусируемого элемента.
 */
@Environment(EnvType.CLIENT)
public interface GuiNavigationPath {

	static GuiNavigationPath of(Element leaf) {
		return new GuiNavigationPath.Leaf(leaf);
	}

	static @Nullable GuiNavigationPath of(ParentElement element, @Nullable GuiNavigationPath childPath) {
		return childPath == null ? null : new GuiNavigationPath.IntermediaryNode(element, childPath);
	}

	static GuiNavigationPath of(Element leaf, ParentElement... elements) {
		GuiNavigationPath path = of(leaf);

		for (ParentElement parentElement : elements) {
			path = of(parentElement, path);
		}

		return path;
	}

	Element component();

	void setFocused(boolean focused);

	@Environment(EnvType.CLIENT)
	public record IntermediaryNode(ParentElement component, GuiNavigationPath childPath) implements GuiNavigationPath {

		@Override
		public void setFocused(boolean focused) {
			if (focused) {
				component.setFocused(childPath.component());
			} else {
				component.setFocused(null);
			}

			childPath.setFocused(focused);
		}
	}

	@Environment(EnvType.CLIENT)
	public record Leaf(Element component) implements GuiNavigationPath {

		@Override
		public void setFocused(boolean focused) {
			component.setFocused(focused);
		}
	}
}
