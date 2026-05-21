package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.navigation.NavigationAxis;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code ElementListWidget}.
 */
public abstract class ElementListWidget<E extends ElementListWidget.Entry<E>> extends EntryListWidget<E> {

	public ElementListWidget(MinecraftClient minecraftClient, int i, int j, int k, int l) {
		super(minecraftClient, i, j, k, l);
	}

	@Override
	public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
		if (this.getEntryCount() == 0) {
			return null;
		}
		else if (!(navigation instanceof GuiNavigation.Arrow arrow)) {
			return super.getNavigationPath(navigation);
		}
		else {
			E entry = this.getFocused();
			if (arrow.direction().getAxis() == NavigationAxis.HORIZONTAL && entry != null) {
				return GuiNavigationPath.of(this, entry.getNavigationPath(navigation));
			}
			else {
				int i = -1;
				NavigationDirection navigationDirection = arrow.direction();
				if (entry != null) {
					i = entry.children().indexOf(entry.getFocused());
				}

				if (i == -1) {
					switch (navigationDirection) {
						case LEFT:
							i = Integer.MAX_VALUE;
							navigationDirection = NavigationDirection.DOWN;
							break;
						case RIGHT:
							i = 0;
							navigationDirection = NavigationDirection.DOWN;
							break;
						default:
							i = 0;
					}
				}

				E entry2 = entry;

				GuiNavigationPath guiNavigationPath;
				do {
					entry2 =
							this.getNeighboringEntry(
									navigationDirection,
									element -> !element.children().isEmpty(),
									entry2
							);
					if (entry2 == null) {
						return null;
					}

					guiNavigationPath = entry2.getNavigationPath(arrow, i);
				}
				while (guiNavigationPath == null);

				return GuiNavigationPath.of(this, guiNavigationPath);
			}
		}
	}

	@Override
	public void setFocused(@Nullable Element focused) {
		if (this.getFocused() != focused) {
			super.setFocused(focused);
			if (focused == null) {
				this.setSelected(null);
			}
		}
	}

	@Override
	public Selectable.SelectionType getType() {
		return this.isFocused() ? Selectable.SelectionType.FOCUSED : super.getType();
	}

	@Override
	protected boolean isEntrySelectionAllowed() {
		return false;
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		if (this.getHoveredEntry() instanceof E entry) {
			entry.appendNarrations(builder.nextMessage());
			this.appendNarrations(builder, entry);
		}
		else if (this.getFocused() instanceof E entry2) {
			entry2.appendNarrations(builder.nextMessage());
			this.appendNarrations(builder, entry2);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Entry}.
	 */
	public abstract static class Entry<E extends ElementListWidget.Entry<E>> extends EntryListWidget.Entry<E> implements ParentElement {

		private @Nullable Element focused;
		private @Nullable Selectable focusedSelectable;
		private boolean dragging;

		@Override
		public boolean isDragging() {
			return this.dragging;
		}

		@Override
		public void setDragging(boolean dragging) {
			this.dragging = dragging;
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			return ParentElement.super.mouseClicked(click, doubled);
		}

		@Override
		public void setFocused(@Nullable Element focused) {
			if (this.focused != null) {
				this.focused.setFocused(false);
			}

			if (focused != null) {
				focused.setFocused(true);
			}

			this.focused = focused;
		}

		@Override
		public @Nullable Element getFocused() {
			return this.focused;
		}

		public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation, int index) {
			if (this.children().isEmpty()) {
				return null;
			}
			else {
				GuiNavigationPath
						guiNavigationPath =
						this.children().get(Math.min(index, this.children().size() - 1)).getNavigationPath(navigation);
				return GuiNavigationPath.of(this, guiNavigationPath);
			}
		}

		@Override
		public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
			if (navigation instanceof GuiNavigation.Arrow arrow) {
				int i = switch (arrow.direction()) {
					case LEFT -> -1;
					case RIGHT -> 1;
					case UP, DOWN -> 0;
				};
				if (i == 0) {
					return null;
				}

				int j = MathHelper.clamp(i + this.children().indexOf(this.getFocused()), 0, this.children().size() - 1);

				for (int k = j; k >= 0 && k < this.children().size(); k += i) {
					Element element = this.children().get(k);
					GuiNavigationPath guiNavigationPath = element.getNavigationPath(navigation);
					if (guiNavigationPath != null) {
						return GuiNavigationPath.of(this, guiNavigationPath);
					}
				}
			}

			return ParentElement.super.getNavigationPath(navigation);
		}

		/**
		 * Selectable children.
		 *
		 * @return List — результат операции
		 */
		public abstract List<? extends Selectable> selectableChildren();

		void appendNarrations(NarrationMessageBuilder builder) {
			List<? extends Selectable> list = this.selectableChildren();
			Screen.SelectedElementNarrationData
					selectedElementNarrationData =
					Screen.findSelectedElementData(list, this.focusedSelectable);
			if (selectedElementNarrationData != null) {
				if (selectedElementNarrationData.selectType().isFocused()) {
					this.focusedSelectable = selectedElementNarrationData.selectable();
				}

				if (list.size() > 1) {
					builder.put(NarrationPart.POSITION,
							Text.translatable(
									"narrator.position.object_list",
									selectedElementNarrationData.index() + 1,
									list.size()
							)
					);
				}

				selectedElementNarrationData.selectable().appendNarrations(builder.nextMessage());
			}
		}
	}
}
