package net.minecraft.client.gui.widget;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Виджет навигации по вкладкам. Управляет отображением кнопок вкладок, разделителем заголовка
 * и переключением активной вкладки через мышь, клавиатуру (Ctrl+Tab, Ctrl+1..9) и программно.
 */
@Environment(EnvType.CLIENT)
public class TabNavigationWidget extends AbstractParentElement implements Drawable, Selectable {

	private static final int NO_TAB_SELECTED = -1;
	private static final int MAX_TAB_NAV_WIDTH = 400;
	private static final int TAB_HEIGHT = 24;
	private static final int TAB_SIDE_PADDING = 14;
	private static final Text USAGE_NARRATION_TEXT = Text.translatable("narration.tab_navigation.usage");
	private final DirectionalLayoutWidget grid = DirectionalLayoutWidget.horizontal();
	private int tabNavWidth;
	private final TabManager tabManager;
	private final ImmutableList<Tab> tabs;
	private final ImmutableList<TabButtonWidget> tabButtons;

	TabNavigationWidget(int x, TabManager tabManager, Iterable<Tab> tabs) {
		tabNavWidth = x;
		this.tabManager = tabManager;
		this.tabs = ImmutableList.copyOf(tabs);
		grid.getMainPositioner().alignHorizontalCenter();

		ImmutableList.Builder<TabButtonWidget> builder = ImmutableList.builder();
		for (Tab tab : tabs) {
			builder.add(grid.add(new TabButtonWidget(tabManager, tab, 0, TAB_HEIGHT)));
		}

		tabButtons = builder.build();
	}

	public static TabNavigationWidget.Builder builder(TabManager tabManager, int width) {
		return new TabNavigationWidget.Builder(tabManager, width);
	}

	public void setWidth(int width) {
		tabNavWidth = width;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= grid.getX()
				&& mouseY >= grid.getY()
				&& mouseX < grid.getX() + grid.getWidth()
				&& mouseY < grid.getY() + grid.getHeight();
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (getFocused() != null) {
			setFocused(null);
		}
	}

	@Override
	public void setFocused(@Nullable Element focused) {
		super.setFocused(focused);
		if (focused instanceof TabButtonWidget tabButtonWidget && tabButtonWidget.isInteractable()) {
			tabManager.setCurrentTab(tabButtonWidget.getTab(), true);
		}
	}

	@Override
	public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
		if (isFocused()) {
			return navigation instanceof GuiNavigation.Tab ? null : super.getNavigationPath(navigation);
		}

		TabButtonWidget currentButton = getCurrentTabButton();
		if (currentButton != null) {
			return GuiNavigationPath.of(this, GuiNavigationPath.of(currentButton));
		}

		return navigation instanceof GuiNavigation.Tab ? null : super.getNavigationPath(navigation);
	}

	@Override
	public List<? extends Element> children() {
		return tabButtons;
	}

	public List<Tab> getTabs() {
		return tabs;
	}

	@Override
	public Selectable.SelectionType getType() {
		return tabButtons
				.stream()
				.map(ClickableWidget::getType)
				.max(Comparator.naturalOrder())
				.orElse(Selectable.SelectionType.NONE);
	}

	@Override
	public void appendNarrations(NarrationMessageBuilder builder) {
		Optional<TabButtonWidget> hoveredOrCurrent = tabButtons
				.stream()
				.filter(ClickableWidget::isHovered)
				.findFirst()
				.or(() -> Optional.ofNullable(getCurrentTabButton()));

		hoveredOrCurrent.ifPresent(button -> {
			appendNarrations(builder.nextMessage(), button);
			button.appendNarrations(builder);
		});

		if (isFocused()) {
			builder.put(NarrationPart.USAGE, USAGE_NARRATION_TEXT);
		}
	}

	protected void appendNarrations(NarrationMessageBuilder builder, TabButtonWidget button) {
		if (tabs.size() <= 1) {
			return;
		}

		int buttonIndex = tabButtons.indexOf(button);
		if (buttonIndex != -1) {
			builder.put(NarrationPart.POSITION, Text.translatable("narrator.position.tab", buttonIndex + 1, tabs.size()));
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int separatorY = grid.getY() + grid.getHeight() - 2;
		int firstTabX = tabButtons.get(0).getX();
		int lastTabRight = tabButtons.get(tabButtons.size() - 1).getRight();

		context.drawTexture(RenderPipelines.GUI_TEXTURED, Screen.HEADER_SEPARATOR_TEXTURE, 0, separatorY, 0.0F, 0.0F, firstTabX, 2, 32, 2);
		context.drawTexture(RenderPipelines.GUI_TEXTURED, Screen.HEADER_SEPARATOR_TEXTURE, lastTabRight, separatorY, 0.0F, 0.0F, tabNavWidth, 2, 32, 2);

		for (TabButtonWidget button : tabButtons) {
			button.render(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Override
	public ScreenRect getNavigationFocus() {
		return grid.getNavigationFocus();
	}

	public void init() {
		int availableWidth = Math.min(MAX_TAB_NAV_WIDTH, tabNavWidth) - TAB_SIDE_PADDING * 2;
		int tabWidth = MathHelper.roundUpToMultiple(availableWidth / tabs.size(), 2);

		for (TabButtonWidget button : tabButtons) {
			button.setWidth(tabWidth);
		}

		grid.refreshPositions();
		grid.setX(MathHelper.roundUpToMultiple((tabNavWidth - availableWidth) / 2, 2));
		grid.setY(0);
	}

	public void selectTab(int index, boolean clickSound) {
		if (isFocused()) {
			setFocused(tabButtons.get(index));
		}
		else if (tabButtons.get(index).isInteractable()) {
			tabManager.setCurrentTab(tabs.get(index), clickSound);
		}
	}

	public void setTabActive(int index, boolean active) {
		if (index >= 0 && index < tabButtons.size()) {
			tabButtons.get(index).active = active;
		}
	}

	public void setTabTooltip(int index, @Nullable Tooltip tooltip) {
		if (index >= 0 && index < tabButtons.size()) {
			tabButtons.get(index).setTooltip(tooltip);
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.hasCtrlOrCmd()) {
			int tabIndex = getTabForKey(input);
			if (tabIndex != -1) {
				selectTab(MathHelper.clamp(tabIndex, 0, tabs.size() - 1), true);
				return true;
			}
		}

		return false;
	}

	private int getTabForKey(KeyInput keyInput) {
		return getTabForKey(getCurrentTabIndex(), keyInput);
	}

	private int getTabForKey(int index, KeyInput keyInput) {
		int numericKey = keyInput.asNumber();
		if (numericKey != -1) {
			return Math.floorMod(numericKey - 1, 10);
		}

		if (keyInput.isTab() && index != -1) {
			int nextIndex = keyInput.hasShift() ? index - 1 : index + 1;
			int wrappedIndex = Math.floorMod(nextIndex, tabs.size());
			return tabButtons.get(wrappedIndex).active ? wrappedIndex : getTabForKey(wrappedIndex, keyInput);
		}

		return NO_TAB_SELECTED;
	}

	private int getCurrentTabIndex() {
		Tab currentTab = tabManager.getCurrentTab();
		int index = tabs.indexOf(currentTab);
		return index != -1 ? index : NO_TAB_SELECTED;
	}

	private @Nullable TabButtonWidget getCurrentTabButton() {
		int index = getCurrentTabIndex();
		return index != NO_TAB_SELECTED ? tabButtons.get(index) : null;
	}

	/**
	 * Строитель для создания экземпляров {@link TabNavigationWidget}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final int width;
		private final TabManager tabManager;
		private final List<Tab> tabs = new ArrayList<>();

		Builder(TabManager tabManager, int width) {
			this.tabManager = tabManager;
			this.width = width;
		}

		public TabNavigationWidget.Builder tabs(Tab... tabs) {
			Collections.addAll(this.tabs, tabs);
			return this;
		}

		public TabNavigationWidget build() {
			return new TabNavigationWidget(width, tabManager, tabs);
		}
	}
}
