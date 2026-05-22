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

/**
 * Список с записями, каждая из которых может содержать несколько интерактивных дочерних элементов.
 * Расширяет {@link EntryListWidget}, добавляя поддержку горизонтальной навигации внутри записей
 * и делегирование фокуса дочерним виджетам через {@link ParentElement}.
 *
 * @param <E> тип записи списка
 */
@Environment(EnvType.CLIENT)
public abstract class ElementListWidget<E extends ElementListWidget.Entry<E>> extends EntryListWidget<E> {

	public ElementListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
		super(client, width, height, y, itemHeight);
	}

	@Override
	public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
		if (getEntryCount() == 0) {
			return null;
		}

		if (!(navigation instanceof GuiNavigation.Arrow arrow)) {
			return super.getNavigationPath(navigation);
		}

		E focused = getFocused();
		if (arrow.direction().getAxis() == NavigationAxis.HORIZONTAL && focused != null) {
			return GuiNavigationPath.of(this, focused.getNavigationPath(navigation));
		}

		int childIndex = -1;
		NavigationDirection direction = arrow.direction();
		if (focused != null) {
			childIndex = focused.children().indexOf(focused.getFocused());
		}

		if (childIndex == -1) {
			childIndex = switch (direction) {
				case LEFT -> {
					direction = NavigationDirection.DOWN;
					yield Integer.MAX_VALUE;
				}
				case RIGHT -> {
					direction = NavigationDirection.DOWN;
					yield 0;
				}
				default -> 0;
			};
		}

		E candidate = focused;
		GuiNavigationPath path;
		final NavigationDirection finalDirection = direction;
		final int finalChildIndex = childIndex;
		do {
			candidate = getNeighboringEntry(
					finalDirection,
					element -> !element.children().isEmpty(),
					candidate
			);
			if (candidate == null) {
				return null;
			}

			path = candidate.getNavigationPath(arrow, finalChildIndex);
		} while (path == null);

		return GuiNavigationPath.of(this, path);
	}

	@Override
	public void setFocused(@Nullable Element focused) {
		if (getFocused() == focused) {
			return;
		}

		super.setFocused(focused);
		if (focused == null) {
			setSelected(null);
		}
	}

	@Override
	public Selectable.SelectionType getType() {
		return isFocused() ? Selectable.SelectionType.FOCUSED : super.getType();
	}

	@Override
	protected boolean isEntrySelectionAllowed() {
		return false;
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		if (getHoveredEntry() instanceof E entry) {
			entry.appendNarrations(builder.nextMessage());
			appendNarrations(builder, entry);
		} else if (getFocused() instanceof E entry) {
			entry.appendNarrations(builder.nextMessage());
			appendNarrations(builder, entry);
		}
	}

	/**
	 * Базовая запись для {@link ElementListWidget}, поддерживающая дочерние интерактивные элементы.
	 * Реализует {@link ParentElement} для делегирования фокуса и навигации вложенным виджетам.
	 *
	 * @param <E> тип записи
	 */
	@Environment(EnvType.CLIENT)
	public abstract static class Entry<E extends ElementListWidget.Entry<E>> extends EntryListWidget.Entry<E> implements ParentElement {

		private @Nullable Element focused;
		private @Nullable Selectable focusedSelectable;
		private boolean dragging;

		@Override
		public boolean isDragging() {
			return dragging;
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
			return focused;
		}

		/**
		 * Возвращает путь навигации к дочернему элементу по заданному индексу.
		 * Используется при горизонтальной навигации между записями списка.
		 *
		 * @param navigation навигационное событие
		 * @param index индекс дочернего элемента, к которому нужно перейти
		 * @return путь навигации или {@code null}, если дочерних элементов нет
		 */
		public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation, int index) {
			if (children().isEmpty()) {
				return null;
			}

			GuiNavigationPath path = children()
					.get(Math.min(index, children().size() - 1))
					.getNavigationPath(navigation);
			return GuiNavigationPath.of(this, path);
		}

		@Override
		public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
			if (navigation instanceof GuiNavigation.Arrow arrow) {
				int step = switch (arrow.direction()) {
					case LEFT -> -1;
					case RIGHT -> 1;
					case UP, DOWN -> 0;
				};
				if (step == 0) {
					return null;
				}

				int startIndex = MathHelper.clamp(step + children().indexOf(getFocused()), 0, children().size() - 1);
				for (int index = startIndex; index >= 0 && index < children().size(); index += step) {
					Element element = children().get(index);
					GuiNavigationPath path = element.getNavigationPath(navigation);
					if (path != null) {
						return GuiNavigationPath.of(this, path);
					}
				}
			}

			return ParentElement.super.getNavigationPath(navigation);
		}

		public abstract List<? extends Selectable> selectableChildren();

		void appendNarrations(NarrationMessageBuilder builder) {
			List<? extends Selectable> selectables = selectableChildren();
			Screen.SelectedElementNarrationData narrationData = Screen.findSelectedElementData(selectables, focusedSelectable);
			if (narrationData == null) {
				return;
			}

			if (narrationData.selectType().isFocused()) {
				focusedSelectable = narrationData.selectable();
			}

			if (selectables.size() > 1) {
				builder.put(
						NarrationPart.POSITION,
						Text.translatable(
								"narrator.position.object_list",
								narrationData.index() + 1,
								selectables.size()
						)
				);
			}

			narrationData.selectable().appendNarrations(builder.nextMessage());
		}
	}
}
