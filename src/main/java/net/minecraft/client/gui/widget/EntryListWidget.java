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

/**
 * Базовый прокручиваемый список с записями фиксированной высоты.
 * Управляет позиционированием, рендерингом, навигацией и нарративом для всех дочерних записей.
 * Является основой для {@link ElementListWidget} и {@link AlwaysSelectedEntryListWidget}.
 *
 * @param <E> тип записи списка
 */
@Environment(EnvType.CLIENT)
public abstract class EntryListWidget<E extends EntryListWidget.Entry<E>> extends ContainerWidget {

	private static final Identifier MENU_LIST_BACKGROUND_TEXTURE =
			Identifier.ofVanilla("textures/gui/menu_list_background.png");
	private static final Identifier INWORLD_MENU_LIST_BACKGROUND_TEXTURE =
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
		return selected;
	}

	public void setSelected(@Nullable E entry) {
		selected = entry;
		if (entry == null) {
			return;
		}

		boolean aboveView = entry.getContentY() < getY();
		boolean belowView = entry.getContentBottomEnd() > getBottom();
		if (client.getNavigationType().isKeyboard() || aboveView || belowView) {
			scrollTo(entry);
		}
	}

	public @Nullable E getFocused() {
		return (E) super.getFocused();
	}

	@Override
	public final List<E> children() {
		return Collections.unmodifiableList(children);
	}

	protected void sort(Comparator<E> comparator) {
		children.sort(comparator);
		recalculateAllChildrenPositions();
	}

	protected void swapEntriesOnPositions(int pos1, int pos2) {
		Collections.swap(children, pos1, pos2);
		recalculateAllChildrenPositions();
		scrollTo(children.get(pos2));
	}

	protected void clearEntries() {
		children.clear();
		selected = null;
	}

	protected void clearEntriesExcept(E entryToKeep) {
		children.removeIf(entry -> entry != entryToKeep);
		if (selected != entryToKeep) {
			setSelected(null);
		}
	}

	public void replaceEntries(Collection<E> collection) {
		clearEntries();
		for (E entry : collection) {
			addEntry(entry);
		}
	}

	private int getYOfFirstEntry() {
		return getY() + SPACER_HEIGHT;
	}

	public int getYOfNextEntry() {
		int y = getYOfFirstEntry() - (int) getScrollY();
		for (E entry : children) {
			y += entry.getHeight();
		}

		return y;
	}

	protected int addEntry(E entry) {
		return addEntry(entry, itemHeight);
	}

	protected int addEntry(E entry, int entryHeight) {
		entry.setX(getRowLeft());
		entry.setWidth(getRowWidth());
		entry.setY(getYOfNextEntry());
		entry.setHeight(entryHeight);
		children.add(entry);
		return children.size() - 1;
	}

	protected void addEntryToTop(E entry) {
		addEntryToTop(entry, itemHeight);
	}

	protected void addEntryToTop(E entry, int entryHeight) {
		double remainingScroll = getMaxScrollY() - getScrollY();
		entry.setHeight(entryHeight);
		children.addFirst(entry);
		recalculateAllChildrenPositions();
		setScrollY(getMaxScrollY() - remainingScroll);
	}

	private void recalculateAllChildrenPositions() {
		int y = getYOfFirstEntry() - (int) getScrollY();
		for (E entry : children) {
			entry.setY(y);
			y += entry.getHeight();
			entry.setX(getRowLeft());
			entry.setWidth(getRowWidth());
		}
	}

	protected void removeEntryWithoutScrolling(E entry) {
		double remainingScroll = getMaxScrollY() - getScrollY();
		removeEntry(entry);
		setScrollY(getMaxScrollY() - remainingScroll);
	}

	protected int getEntryCount() {
		return children().size();
	}

	protected boolean isEntrySelectionAllowed() {
		return true;
	}

	protected final @Nullable E getEntryAtPosition(double x, double y) {
		for (E entry : children) {
			if (entry.isMouseOver(x, y)) {
				return entry;
			}
		}

		return null;
	}

	public void position(int width, ThreePartsLayoutWidget layout) {
		position(width, layout.getContentHeight(), layout.getHeaderHeight());
	}

	public void position(int width, int height, int y) {
		position(width, height, 0, y);
	}

	public void position(int width, int height, int x, int y) {
		setDimensions(width, height);
		setPosition(x, y);
		recalculateAllChildrenPositions();
		if (getSelectedOrNull() != null) {
			scrollTo(getSelectedOrNull());
		}

		refreshScroll();
	}

	@Override
	protected int getContentsHeightWithPadding() {
		int total = 0;
		for (E entry : children) {
			total += entry.getHeight();
		}

		return total + 4;
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		hoveredEntry = isMouseOver(mouseX, mouseY) ? getEntryAtPosition(mouseX, mouseY) : null;
		drawMenuListBackground(context);
		enableScissor(context);
		renderList(context, mouseX, mouseY, deltaTicks);
		context.disableScissor();
		drawHeaderAndFooterSeparators(context);
		drawScrollbar(context, mouseX, mouseY);
	}

	protected void drawHeaderAndFooterSeparators(DrawContext context) {
		Identifier headerTexture = client.world == null
				? Screen.HEADER_SEPARATOR_TEXTURE
				: Screen.INWORLD_HEADER_SEPARATOR_TEXTURE;
		Identifier footerTexture = client.world == null
				? Screen.FOOTER_SEPARATOR_TEXTURE
				: Screen.INWORLD_FOOTER_SEPARATOR_TEXTURE;

		context.drawTexture(RenderPipelines.GUI_TEXTURED, headerTexture, getX(), getY() - 2, 0.0F, 0.0F, getWidth(), 2, 32, 2);
		context.drawTexture(RenderPipelines.GUI_TEXTURED, footerTexture, getX(), getBottom(), 0.0F, 0.0F, getWidth(), 2, 32, 2);
	}

	protected void drawMenuListBackground(DrawContext context) {
		Identifier texture = client.world == null
				? MENU_LIST_BACKGROUND_TEXTURE
				: INWORLD_MENU_LIST_BACKGROUND_TEXTURE;

		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				texture,
				getX(),
				getY(),
				getRight(),
				getBottom() + (int) getScrollY(),
				getWidth(),
				getHeight(),
				32,
				32
		);
	}

	protected void enableScissor(DrawContext context) {
		context.enableScissor(getX(), getY(), getRight(), getBottom());
	}

	protected void scrollTo(E entry) {
		int topOffset = entry.getY() - getY() - SPACER_HEIGHT;
		if (topOffset < 0) {
			scroll(topOffset);
		}

		int bottomOffset = getBottom() - entry.getY() - entry.getHeight() - SPACER_HEIGHT;
		if (bottomOffset < 0) {
			scroll(-bottomOffset);
		}
	}

	protected void centerScrollOn(E entry) {
		int offset = 0;
		for (E candidate : children) {
			if (candidate == entry) {
				offset += candidate.getHeight() / 2;
				break;
			}

			offset += candidate.getHeight();
		}

		setScrollY(offset - height / 2.0);
	}

	private void scroll(int amount) {
		setScrollY(getScrollY() + amount);
	}

	@Override
	public void setScrollY(double scrollY) {
		super.setScrollY(scrollY);
		recalculateAllChildrenPositions();
	}

	@Override
	protected double getDeltaYPerScroll() {
		return itemHeight / 2.0;
	}

	@Override
	protected int getScrollbarX() {
		return getRowRight() + 6 + 2;
	}

	@Override
	public Optional<Element> hoveredElement(double mouseX, double mouseY) {
		return Optional.ofNullable(getEntryAtPosition(mouseX, mouseY));
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			setFocused(null);
		}
	}

	@Override
	public void setFocused(@Nullable Element focused) {
		E current = getFocused();
		if (current != focused && current instanceof ParentElement parentElement) {
			parentElement.setFocused(null);
		}

		super.setFocused(focused);
		int index = children.indexOf(focused);
		if (index >= 0) {
			setSelected(children.get(index));
		}
	}

	protected @Nullable E getNeighboringEntry(NavigationDirection direction) {
		return getNeighboringEntry(direction, entry -> true);
	}

	protected @Nullable E getNeighboringEntry(NavigationDirection direction, Predicate<E> predicate) {
		return getNeighboringEntry(direction, predicate, getSelectedOrNull());
	}

	protected @Nullable E getNeighboringEntry(
			NavigationDirection direction,
			Predicate<E> predicate,
			@Nullable E selected
	) {
		int step = switch (direction) {
			case RIGHT, LEFT -> 0;
			case UP -> -1;
			case DOWN -> 1;
		};
		if (children().isEmpty() || step == 0) {
			return null;
		}

		int startIndex;
		if (selected == null) {
			startIndex = step > 0 ? 0 : children().size() - 1;
		} else {
			startIndex = children().indexOf(selected) + step;
		}

		for (int index = startIndex; index >= 0 && index < children.size(); index += step) {
			E entry = children().get(index);
			if (predicate.test(entry)) {
				return entry;
			}
		}

		return null;
	}

	protected void renderList(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		for (E entry : children) {
			if (entry.getY() + entry.getHeight() >= getY() && entry.getY() <= getBottom()) {
				renderEntry(context, mouseX, mouseY, deltaTicks, entry);
			}
		}
	}

	protected void renderEntry(DrawContext context, int mouseX, int mouseY, float delta, E entry) {
		if (isEntrySelectionAllowed() && getSelectedOrNull() == entry) {
			int borderColor = isFocused() ? -1 : -8355712;
			drawSelectionHighlight(context, entry, borderColor);
		}

		entry.render(context, mouseX, mouseY, Objects.equals(hoveredEntry, entry), delta);
	}

	protected void drawSelectionHighlight(DrawContext context, E entry, int color) {
		int x1 = entry.getX();
		int y1 = entry.getY();
		int x2 = x1 + entry.getWidth();
		int y2 = y1 + entry.getHeight();
		context.fill(x1, y1, x2, y2, color);
		context.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, -16777216);
	}

	public int getRowLeft() {
		return getX() + width / 2 - getRowWidth() / 2;
	}

	public int getRowRight() {
		return getRowLeft() + getRowWidth();
	}

	public int getRowTop(int index) {
		return children.get(index).getY();
	}

	public int getRowBottom(int index) {
		E entry = children.get(index);
		return entry.getY() + entry.getHeight();
	}

	public int getRowWidth() {
		return 220;
	}

	@Override
	public Selectable.SelectionType getType() {
		if (isFocused()) {
			return Selectable.SelectionType.FOCUSED;
		}

		return hoveredEntry != null ? Selectable.SelectionType.HOVERED : Selectable.SelectionType.NONE;
	}

	protected void removeEntries(List<E> entries) {
		entries.forEach(this::removeEntry);
	}

	protected void removeEntry(E entry) {
		boolean removed = children.remove(entry);
		if (!removed) {
			return;
		}

		recalculateAllChildrenPositions();
		if (entry == getSelectedOrNull()) {
			setSelected(null);
		}
	}

	protected @Nullable E getHoveredEntry() {
		return hoveredEntry;
	}

	void setEntryParentList(EntryListWidget.Entry<E> entry) {
		entry.parentList = this;
	}

	protected void appendNarrations(NarrationMessageBuilder builder, E entry) {
		List<E> list = children();
		if (list.size() <= 1) {
			return;
		}

		int index = list.indexOf(entry);
		if (index != -1) {
			builder.put(NarrationPart.POSITION, Text.translatable("narrator.position.list", index + 1, list.size()));
		}
	}

	@Environment(EnvType.CLIENT)
	class Entries extends AbstractList<E> {

		private final List<E> entries = Lists.newArrayList();

		@Override
		public E get(int index) {
			return entries.get(index);
		}

		@Override
		public int size() {
			return entries.size();
		}

		@Override
		public E set(int index, E entry) {
			E previous = entries.set(index, entry);
			EntryListWidget.this.setEntryParentList(entry);
			return previous;
		}

		@Override
		public void add(int index, E entry) {
			entries.add(index, entry);
			EntryListWidget.this.setEntryParentList(entry);
		}

		@Override
		public E remove(int index) {
			return entries.remove(index);
		}
	}

	/**
	 * Базовая запись списка. Хранит позицию и размер, делегирует проверку наведения
	 * через {@link #getNavigationFocus()}.
	 *
	 * @param <E> тип записи
	 */
	@Environment(EnvType.CLIENT)
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
			return parentList.getFocused() == this;
		}

		public abstract void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks);

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return getNavigationFocus().contains((int) mouseX, (int) mouseY);
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
			return getX() + PADDING;
		}

		public int getContentY() {
			return getY() + PADDING;
		}

		public int getContentHeight() {
			return getHeight() - 4;
		}

		public int getContentMiddleY() {
			return getContentY() + getContentHeight() / 2;
		}

		public int getContentBottomEnd() {
			return getContentY() + getContentHeight();
		}

		public int getContentWidth() {
			return getWidth() - 4;
		}

		public int getContentMiddleX() {
			return getContentX() + getContentWidth() / 2;
		}

		public int getContentRightEnd() {
			return getContentX() + getContentWidth();
		}

		@Override
		public int getX() {
			return x;
		}

		@Override
		public int getY() {
			return y;
		}

		@Override
		public int getWidth() {
			return width;
		}

		@Override
		public int getHeight() {
			return height;
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
