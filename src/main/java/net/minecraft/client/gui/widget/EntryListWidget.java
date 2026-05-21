package net.minecraft.client.gui.widget;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
/**
 * {@code EntryListWidget}.
 */
public abstract class EntryListWidget<E extends EntryListWidget.Entry<E>> extends ContainerWidget {

	private static final Identifier
			MENU_LIST_BACKGROUND_TEXTURE =
			Identifier.ofVanilla("textures/gui/menu_list_background.png");
	private static final Identifier
			INWORLD_MENU_LIST_BACKGROUND_TEXTURE =
			Identifier.ofVanilla("textures/gui/inworld_menu_list_background.png");
	private static final int SPACER_HEIGHT = 2;
	protected final MinecraftClient client;
	protected final int itemHeight;
	private final List<E> children = new EntryListWidget.Entries();
	protected boolean centerListVertically = true;
	private @Nullable E selected;
	private @Nullable E hoveredEntry;

	public EntryListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
		super(0, y, width, height, ScreenTexts.EMPTY);
		this.client = client;
		this.itemHeight = itemHeight;
	}

	public @Nullable E getSelectedOrNull() {
		return this.selected;
	}

	public void setSelected(@Nullable E entry) {
		this.selected = entry;
		if (entry != null) {
			boolean bl = entry.getContentY() < this.getY();
			boolean bl2 = entry.getContentBottomEnd() > this.getBottom();
			if (this.client.getNavigationType().isKeyboard() || bl || bl2) {
				this.scrollTo(entry);
			}
		}
	}

	public @Nullable E getFocused() {
		return (E) super.getFocused();
	}

	@Override
	public final List<E> children() {
		return Collections.unmodifiableList(this.children);
	}

	/**
	 * Sort.
	 *
	 * @param comparator comparator
	 */
	protected void sort(Comparator<E> comparator) {
		this.children.sort(comparator);
		this.recalculateAllChildrenPositions();
	}

	/**
	 * Swap entries on positions.
	 *
	 * @param pos1 pos1
	 * @param pos2 pos2
	 */
	protected void swapEntriesOnPositions(int pos1, int pos2) {
		Collections.swap(this.children, pos1, pos2);
		this.recalculateAllChildrenPositions();
		this.scrollTo(this.children.get(pos2));
	}

	/**
	 * Очищает entries.
	 */
	protected void clearEntries() {
		this.children.clear();
		this.selected = null;
	}

	/**
	 * Очищает entries except.
	 *
	 * @param entryToKeep entry to keep
	 */
	protected void clearEntriesExcept(E entryToKeep) {
		this.children.removeIf(entry -> entry != entryToKeep);
		if (this.selected != entryToKeep) {
			this.setSelected(null);
		}
	}

	/**
	 * Replace entries.
	 *
	 * @param collection collection
	 */
	public void replaceEntries(Collection<E> collection) {
		this.clearEntries();

		for (E entry : collection) {
			this.addEntry(entry);
		}
	}

	private int getYOfFirstEntry() {
		return this.getY() + 2;
	}

	public int getYOfNextEntry() {
		int i = this.getYOfFirstEntry() - (int) this.getScrollY();

		for (E entry : this.children) {
			i += entry.getHeight();
		}

		return i;
	}

	/**
	 * Добавляет entry.
	 *
	 * @param entry entry
	 *
	 * @return int — результат операции
	 */
	protected int addEntry(E entry) {
		return this.addEntry(entry, this.itemHeight);
	}

	/**
	 * Добавляет entry.
	 *
	 * @param entry entry
	 * @param entryHeight entry height
	 *
	 * @return int — результат операции
	 */
	protected int addEntry(E entry, int entryHeight) {
		entry.setX(this.getRowLeft());
		entry.setWidth(this.getRowWidth());
		entry.setY(this.getYOfNextEntry());
		entry.setHeight(entryHeight);
		this.children.add(entry);
		return this.children.size() - 1;
	}

	/**
	 * Добавляет entry to top.
	 *
	 * @param entry entry
	 */
	protected void addEntryToTop(E entry) {
		this.addEntryToTop(entry, this.itemHeight);
	}

	/**
	 * Добавляет entry to top.
	 *
	 * @param entry entry
	 * @param entryHeight entry height
	 */
	protected void addEntryToTop(E entry, int entryHeight) {
		double d = this.getMaxScrollY() - this.getScrollY();
		entry.setHeight(entryHeight);
		this.children.addFirst(entry);
		this.recalculateAllChildrenPositions();
		this.setScrollY(this.getMaxScrollY() - d);
	}

	private void recalculateAllChildrenPositions() {
		int i = this.getYOfFirstEntry() - (int) this.getScrollY();

		for (E entry : this.children) {
			entry.setY(i);
			i += entry.getHeight();
			entry.setX(this.getRowLeft());
			entry.setWidth(this.getRowWidth());
		}
	}

	/**
	 * Удаляет entry without scrolling.
	 *
	 * @param entry entry
	 */
	protected void removeEntryWithoutScrolling(E entry) {
		double d = this.getMaxScrollY() - this.getScrollY();
		this.removeEntry(entry);
		this.setScrollY(this.getMaxScrollY() - d);
	}

	protected int getEntryCount() {
		return this.children().size();
	}

	protected boolean isEntrySelectionAllowed() {
		return true;
	}

	protected final @Nullable E getEntryAtPosition(double x, double y) {
		for (E entry : this.children) {
			if (entry.isMouseOver(x, y)) {
				return entry;
			}
		}

		return null;
	}

	/**
	 * Position.
	 *
	 * @param width width
	 * @param layout layout
	 */
	public void position(int width, ThreePartsLayoutWidget layout) {
		this.position(width, layout.getContentHeight(), layout.getHeaderHeight());
	}

	/**
	 * Position.
	 *
	 * @param width width
	 * @param height height
	 * @param y y
	 */
	public void position(int width, int height, int y) {
		this.position(width, height, 0, y);
	}

	/**
	 * Position.
	 *
	 * @param width width
	 * @param height height
	 * @param x x
	 * @param y y
	 */
	public void position(int width, int height, int x, int y) {
		this.setDimensions(width, height);
		this.setPosition(x, y);
		this.recalculateAllChildrenPositions();
		if (this.getSelectedOrNull() != null) {
			this.scrollTo(this.getSelectedOrNull());
		}

		this.refreshScroll();
	}

	@Override
	protected int getContentsHeightWithPadding() {
		int i = 0;

		for (E entry : this.children) {
			i += entry.getHeight();
		}

		return i + 4;
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		this.hoveredEntry = this.isMouseOver(mouseX, mouseY) ? this.getEntryAtPosition(mouseX, mouseY) : null;
		this.drawMenuListBackground(context);
		this.enableScissor(context);
		this.renderList(context, mouseX, mouseY, deltaTicks);
		context.disableScissor();
		this.drawHeaderAndFooterSeparators(context);
		this.drawScrollbar(context, mouseX, mouseY);
	}

	/**
	 * Draw header and footer separators.
	 *
	 * @param context context
	 */
	protected void drawHeaderAndFooterSeparators(DrawContext context) {
		Identifier
				identifier =
				this.client.world == null ? Screen.HEADER_SEPARATOR_TEXTURE : Screen.INWORLD_HEADER_SEPARATOR_TEXTURE;
		Identifier
				identifier2 =
				this.client.world == null ? Screen.FOOTER_SEPARATOR_TEXTURE : Screen.INWORLD_FOOTER_SEPARATOR_TEXTURE;
		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				identifier,
				this.getX(),
				this.getY() - 2,
				0.0F,
				0.0F,
				this.getWidth(),
				2,
				32,
				2
		);
		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				identifier2,
				this.getX(),
				this.getBottom(),
				0.0F,
				0.0F,
				this.getWidth(),
				2,
				32,
				2
		);
	}

	/**
	 * Draw menu list background.
	 *
	 * @param context context
	 */
	protected void drawMenuListBackground(DrawContext context) {
		Identifier
				identifier =
				this.client.world == null ? MENU_LIST_BACKGROUND_TEXTURE : INWORLD_MENU_LIST_BACKGROUND_TEXTURE;
		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				identifier,
				this.getX(),
				this.getY(),
				this.getRight(),
				this.getBottom() + (int) this.getScrollY(),
				this.getWidth(),
				this.getHeight(),
				32,
				32
		);
	}

	/**
	 * Включает scissor.
	 *
	 * @param context context
	 */
	protected void enableScissor(DrawContext context) {
		context.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
	}

	/**
	 * Scroll to.
	 *
	 * @param entry entry
	 */
	protected void scrollTo(E entry) {
		int i = entry.getY() - this.getY() - 2;
		if (i < 0) {
			this.scroll(i);
		}

		int j = this.getBottom() - entry.getY() - entry.getHeight() - 2;
		if (j < 0) {
			this.scroll(-j);
		}
	}

	/**
	 * Center scroll on.
	 *
	 * @param entry entry
	 */
	protected void centerScrollOn(E entry) {
		int i = 0;

		for (E entry2 : this.children) {
			if (entry2 == entry) {
				i += entry2.getHeight() / 2;
				break;
			}

			i += entry2.getHeight();
		}

		this.setScrollY(i - this.height / 2.0);
	}

	private void scroll(int amount) {
		this.setScrollY(this.getScrollY() + amount);
	}

	@Override
	public void setScrollY(double scrollY) {
		super.setScrollY(scrollY);
		this.recalculateAllChildrenPositions();
	}

	@Override
	protected double getDeltaYPerScroll() {
		return this.itemHeight / 2.0;
	}

	@Override
	protected int getScrollbarX() {
		return this.getRowRight() + 6 + 2;
	}

	@Override
	public Optional<Element> hoveredElement(double mouseX, double mouseY) {
		return Optional.ofNullable(this.getEntryAtPosition(mouseX, mouseY));
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			this.setFocused(null);
		}
	}

	@Override
	public void setFocused(@Nullable Element focused) {
		E entry = this.getFocused();
		if (entry != focused && entry instanceof ParentElement parentElement) {
			parentElement.setFocused(null);
		}

		super.setFocused(focused);
		int i = this.children.indexOf(focused);
		if (i >= 0) {
			E entry2 = this.children.get(i);
			this.setSelected(entry2);
		}
	}

	protected @Nullable E getNeighboringEntry(NavigationDirection direction) {
		return this.getNeighboringEntry(direction, entry -> true);
	}

	protected @Nullable E getNeighboringEntry(NavigationDirection direction, Predicate<E> predicate) {
		return this.getNeighboringEntry(direction, predicate, this.getSelectedOrNull());
	}

	protected @Nullable E getNeighboringEntry(
			NavigationDirection direction,
			Predicate<E> predicate,
			@Nullable E selected
	) {
		int i = switch (direction) {
			case RIGHT, LEFT -> 0;
			case UP -> -1;
			case DOWN -> 1;
		};
		if (!this.children().isEmpty() && i != 0) {
			int j;
			if (selected == null) {
				j = i > 0 ? 0 : this.children().size() - 1;
			}
			else {
				j = this.children().indexOf(selected) + i;
			}

			for (int k = j; k >= 0 && k < this.children.size(); k += i) {
				E entry = this.children().get(k);
				if (predicate.test(entry)) {
					return entry;
				}
			}
		}

		return null;
	}

	/**
	 * Отрисовывает list.
	 *
	 * @param context context
	 * @param mouseX mouse x
	 * @param mouseY mouse y
	 * @param deltaTicks delta ticks
	 */
	protected void renderList(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		for (E entry : this.children) {
			if (entry.getY() + entry.getHeight() >= this.getY() && entry.getY() <= this.getBottom()) {
				this.renderEntry(context, mouseX, mouseY, deltaTicks, entry);
			}
		}
	}

	/**
	 * Отрисовывает entry.
	 *
	 * @param context context
	 * @param mouseX mouse x
	 * @param mouseY mouse y
	 * @param delta delta
	 * @param entry entry
	 */
	protected void renderEntry(DrawContext context, int mouseX, int mouseY, float delta, E entry) {
		if (this.isEntrySelectionAllowed() && this.getSelectedOrNull() == entry) {
			int i = this.isFocused() ? -1 : -8355712;
			this.drawSelectionHighlight(context, entry, i);
		}

		entry.render(context, mouseX, mouseY, Objects.equals(this.hoveredEntry, entry), delta);
	}

	/**
	 * Draw selection highlight.
	 *
	 * @param context context
	 * @param entry entry
	 * @param color color
	 */
	protected void drawSelectionHighlight(DrawContext context, E entry, int color) {
		int i = entry.getX();
		int j = entry.getY();
		int k = i + entry.getWidth();
		int l = j + entry.getHeight();
		context.fill(i, j, k, l, color);
		context.fill(i + 1, j + 1, k - 1, l - 1, -16777216);
	}

	public int getRowLeft() {
		return this.getX() + this.width / 2 - this.getRowWidth() / 2;
	}

	public int getRowRight() {
		return this.getRowLeft() + this.getRowWidth();
	}

	public int getRowTop(int index) {
		return this.children.get(index).getY();
	}

	public int getRowBottom(int index) {
		E entry = this.children.get(index);
		return entry.getY() + entry.getHeight();
	}

	public int getRowWidth() {
		return 220;
	}

	@Override
	public Selectable.SelectionType getType() {
		if (this.isFocused()) {
			return Selectable.SelectionType.FOCUSED;
		}
		else {
			return this.hoveredEntry != null ? Selectable.SelectionType.HOVERED : Selectable.SelectionType.NONE;
		}
	}

	/**
	 * Удаляет entries.
	 *
	 * @param entries entries
	 */
	protected void removeEntries(List<E> entries) {
		entries.forEach(this::removeEntry);
	}

	/**
	 * Удаляет entry.
	 *
	 * @param entry entry
	 */
	protected void removeEntry(E entry) {
		boolean bl = this.children.remove(entry);
		if (bl) {
			this.recalculateAllChildrenPositions();
			if (entry == this.getSelectedOrNull()) {
				this.setSelected(null);
			}
		}
	}

	protected @Nullable E getHoveredEntry() {
		return this.hoveredEntry;
	}

	void setEntryParentList(EntryListWidget.Entry<E> entry) {
		entry.parentList = this;
	}

	/**
	 * Append narrations.
	 *
	 * @param builder builder
	 * @param entry entry
	 */
	protected void appendNarrations(NarrationMessageBuilder builder, E entry) {
		List<E> list = this.children();
		if (list.size() > 1) {
			int i = list.indexOf(entry);
			if (i != -1) {
				builder.put(NarrationPart.POSITION, Text.translatable("narrator.position.list", i + 1, list.size()));
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Entries}.
	 */
	class Entries extends AbstractList<E> {

		private final List<E> entries = Lists.newArrayList();

		/**
		 * Get.
		 *
		 * @param i i
		 *
		 * @return E — 
		 */
		public E get(int i) {
			return this.entries.get(i);
		}

		@Override
		public int size() {
			return this.entries.size();
		}

		/**
		 * Set.
		 *
		 * @param i i
		 * @param entry entry
		 *
		 * @return E — результат операции
		 */
		public E set(int i, E entry) {
			E entry2 = this.entries.set(i, entry);
			EntryListWidget.this.setEntryParentList(entry);
			return entry2;
		}

		/**
		 * Add.
		 *
		 * @param i i
		 * @param entry entry
		 */
		public void add(int i, E entry) {
			this.entries.add(i, entry);
			EntryListWidget.this.setEntryParentList(entry);
		}

		/**
		 * Remove.
		 *
		 * @param i i
		 *
		 * @return E — результат операции
		 */
		public E remove(int i) {
			return this.entries.remove(i);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Entry}.
	 */
	protected abstract static class Entry<E extends EntryListWidget.Entry<E>> implements Element, Widget {

		public static final int PADDING = 2;
		private int x = 0;
		private int y = 0;
		private int width = 0;
		private int height;
		@Deprecated
		EntryListWidget<E> parentList;

		@Override
		public void setFocused(boolean focused) {
		}

		@Override
		public boolean isFocused() {
			return this.parentList.getFocused() == this;
		}

		/**
		 * Render.
		 *
		 * @param context context
		 * @param mouseX mouse x
		 * @param mouseY mouse y
		 * @param hovered hovered
		 * @param deltaTicks delta ticks
		 */
		public abstract void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks);

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return this.getNavigationFocus().contains((int) mouseX, (int) mouseY);
		}

		@Override
		public void setX(int x) {
			this.x = x;
		}

		@Override
		public void setY(int y) {
			this.y = y;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		public int getContentX() {
			return this.getX() + 2;
		}

		public int getContentY() {
			return this.getY() + 2;
		}

		public int getContentHeight() {
			return this.getHeight() - 4;
		}

		public int getContentMiddleY() {
			return this.getContentY() + this.getContentHeight() / 2;
		}

		public int getContentBottomEnd() {
			return this.getContentY() + this.getContentHeight();
		}

		public int getContentWidth() {
			return this.getWidth() - 4;
		}

		public int getContentMiddleX() {
			return this.getContentX() + this.getContentWidth() / 2;
		}

		public int getContentRightEnd() {
			return this.getContentX() + this.getContentWidth();
		}

		@Override
		public int getX() {
			return this.x;
		}

		@Override
		public int getY() {
			return this.y;
		}

		@Override
		public int getWidth() {
			return this.width;
		}

		@Override
		public int getHeight() {
			return this.height;
		}

		@Override
		public void forEachChild(Consumer<ClickableWidget> consumer) {
		}

		@Override
		public ScreenRect getNavigationFocus() {
			return Widget.super.getNavigationFocus();
		}
	}
}
