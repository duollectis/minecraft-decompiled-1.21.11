package net.minecraft.client.gui.navigation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code GuiNavigationPath}.
 */
public interface GuiNavigationPath {

	static GuiNavigationPath of(Element leaf) {
		return new GuiNavigationPath.Leaf(leaf);
	}

	static @Nullable GuiNavigationPath of(ParentElement element, @Nullable GuiNavigationPath childPath) {
		return childPath == null ? null : new GuiNavigationPath.IntermediaryNode(element, childPath);
	}

	static GuiNavigationPath of(Element leaf, ParentElement... elements) {
		GuiNavigationPath guiNavigationPath = of(leaf);

		for (ParentElement parentElement : elements) {
			guiNavigationPath = of(parentElement, guiNavigationPath);
		}

		return guiNavigationPath;
	}

	Element component();

	void setFocused(boolean focused);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code IntermediaryNode}.
	 */
	public record IntermediaryNode(ParentElement component, GuiNavigationPath childPath) implements GuiNavigationPath {

		@Override
		public void setFocused(boolean focused) {
			if (!focused) {
				this.component.setFocused(null);
			}
			else {
				this.component.setFocused(this.childPath.component());
			}

			this.childPath.setFocused(focused);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Leaf}.
	 */
	public record Leaf(Element component) implements GuiNavigationPath {

		@Override
		public void setFocused(boolean focused) {
			this.component.setFocused(focused);
		}
	}
}
