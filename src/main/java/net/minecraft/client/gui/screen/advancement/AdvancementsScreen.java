package net.minecraft.client.gui.screen.advancement;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.AdvancementTabC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Экран достижений игрока. Отображает дерево достижений по вкладкам,
 * поддерживает перетаскивание и прокрутку дерева мышью.
 */
@Environment(EnvType.CLIENT)
public class AdvancementsScreen extends Screen implements ClientAdvancementManager.Listener {

	private static final Identifier WINDOW_TEXTURE = Identifier.ofVanilla("textures/gui/advancements/window.png");
	public static final int WINDOW_WIDTH = 252;
	public static final int WINDOW_HEIGHT = 140;
	private static final int PAGE_OFFSET_X = 9;
	private static final int PAGE_OFFSET_Y = 18;
	public static final int PAGE_WIDTH = 234;
	public static final int PAGE_HEIGHT = 113;
	private static final int TITLE_OFFSET_X = 8;
	private static final int TITLE_OFFSET_Y = 6;
	private static final int TEXTURE_WIDTH = 256;
	private static final int TEXTURE_HEIGHT = 256;
	public static final int TAB_PADDING = 16;
	public static final int TAB_HEIGHT = 16;
	public static final int ICON_WIDTH = 14;
	public static final int ICON_PADDING = 7;
	private static final double SCROLL_SPEED = 16.0;
	private static final Text SAD_LABEL_TEXT = Text.translatable("advancements.sad_label");
	private static final Text EMPTY_TEXT = Text.translatable("advancements.empty");
	private static final Text ADVANCEMENTS_TEXT = Text.translatable("gui.advancements");
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	private final @Nullable Screen parent;
	private final ClientAdvancementManager advancementHandler;
	private final Map<AdvancementEntry, AdvancementTab> tabs = Maps.newLinkedHashMap();
	private @Nullable AdvancementTab selectedTab;
	private boolean movingTab;

	public AdvancementsScreen(ClientAdvancementManager advancementHandler) {
		this(advancementHandler, null);
	}

	public AdvancementsScreen(ClientAdvancementManager advancementHandler, @Nullable Screen parent) {
		super(ADVANCEMENTS_TEXT);
		this.advancementHandler = advancementHandler;
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout.addHeader(ADVANCEMENTS_TEXT, textRenderer);
		tabs.clear();
		selectedTab = null;
		advancementHandler.setListener(this);

		if (tabs.isEmpty()) {
			advancementHandler.selectTab(null, true);
		}
		else {
			AdvancementTab firstTab = tabs.values().iterator().next();
			advancementHandler.selectTab(firstTab.getRoot().getAdvancementEntry(), true);
		}

		layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).width(200).build());
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	@Override
	public void removed() {
		advancementHandler.setListener(null);
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
		if (networkHandler != null) {
			networkHandler.sendPacket(AdvancementTabC2SPacket.close());
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0) {
			int windowX = (width - WINDOW_WIDTH) / 2;
			int windowY = (height - WINDOW_HEIGHT) / 2;

			for (AdvancementTab advancementTab : tabs.values()) {
				if (advancementTab.isClickOnTab(windowX, windowY, click.x(), click.y())) {
					advancementHandler.selectTab(advancementTab.getRoot().getAdvancementEntry(), true);
					break;
				}
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (!client.options.advancementsKey.matchesKey(input)) {
			return super.keyPressed(input);
		}

		client.setScreen(null);
		client.mouse.lockCursor();
		return true;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		int windowX = (width - WINDOW_WIDTH) / 2;
		int windowY = (height - WINDOW_HEIGHT) / 2;

		context.createNewRootLayer();
		drawAdvancementTree(context, windowX, windowY);
		context.createNewRootLayer();
		drawWindow(context, windowX, windowY, mouseX, mouseY);

		if (movingTab && selectedTab != null) {
			if (selectedTab.canScrollHorizontally() && selectedTab.canScrollVertically()) {
				context.setCursor(StandardCursors.RESIZE_ALL);
			} else if (selectedTab.canScrollHorizontally()) {
				context.setCursor(StandardCursors.RESIZE_EW);
			} else if (selectedTab.canScrollVertically()) {
				context.setCursor(StandardCursors.RESIZE_NS);
			}
		}

		drawWidgetTooltip(context, mouseX, mouseY, windowX, windowY);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (click.button() != 0) {
			movingTab = false;
			return false;
		}

		if (!movingTab) {
			movingTab = true;
		} else if (selectedTab != null) {
			selectedTab.move(offsetX, offsetY);
		}

		return true;
	}

	@Override
	public boolean mouseReleased(Click click) {
		movingTab = false;
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (selectedTab == null) {
			return false;
		}

		selectedTab.move(horizontalAmount * SCROLL_SPEED, verticalAmount * SCROLL_SPEED);
		return true;
	}

	private void drawAdvancementTree(DrawContext context, int x, int y) {
		AdvancementTab advancementTab = selectedTab;
		if (advancementTab == null) {
			context.fill(x + 9, y + PAGE_OFFSET_Y, x + 9 + PAGE_WIDTH, y + PAGE_OFFSET_Y + PAGE_HEIGHT, -16777216);
			int centerX = x + 9 + 117;
			context.drawCenteredTextWithShadow(textRenderer, EMPTY_TEXT, centerX, y + PAGE_OFFSET_Y + 56 - 9 / 2, -1);
			context.drawCenteredTextWithShadow(textRenderer, SAD_LABEL_TEXT, centerX, y + PAGE_OFFSET_Y + PAGE_HEIGHT - 9, -1);
		} else {
			advancementTab.render(context, x + 9, y + PAGE_OFFSET_Y);
		}
	}

	public void drawWindow(DrawContext context, int x, int y, int mouseX, int mouseY) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, x, y, 0.0F, 0.0F, WINDOW_WIDTH, WINDOW_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

		if (tabs.size() > 1) {
			for (AdvancementTab advancementTab : tabs.values()) {
				advancementTab.drawBackground(context, x, y, mouseX, mouseY, advancementTab == selectedTab);
			}

			for (AdvancementTab advancementTab : tabs.values()) {
				advancementTab.drawIcon(context, x, y);
			}
		}

		context.drawText(
				textRenderer,
				selectedTab != null ? selectedTab.getTitle() : ADVANCEMENTS_TEXT,
				x + TITLE_OFFSET_X,
				y + TITLE_OFFSET_Y,
				-12566464,
				false
		);
	}

	private void drawWidgetTooltip(DrawContext context, int mouseX, int mouseY, int x, int y) {
		if (selectedTab != null) {
			context.getMatrices().pushMatrix();
			context.getMatrices().translate(x + PAGE_OFFSET_X, y + PAGE_OFFSET_Y);
			context.createNewRootLayer();
			selectedTab.drawWidgetTooltip(context, mouseX - x - PAGE_OFFSET_X, mouseY - y - PAGE_OFFSET_Y, x, y);
			context.getMatrices().popMatrix();
		}

		if (tabs.size() > 1) {
			for (AdvancementTab advancementTab : tabs.values()) {
				if (advancementTab.isClickOnTab(x, y, mouseX, mouseY)) {
					context.drawTooltip(textRenderer, advancementTab.getTitle(), mouseX, mouseY);
				}
			}
		}
	}

	@Override
	public void onRootAdded(PlacedAdvancement root) {
		AdvancementTab advancementTab = AdvancementTab.create(client, this, tabs.size(), root);
		if (advancementTab != null) {
			tabs.put(root.getAdvancementEntry(), advancementTab);
		}
	}

	@Override
	public void onRootRemoved(PlacedAdvancement root) {
	}

	@Override
	public void onDependentAdded(PlacedAdvancement dependent) {
		AdvancementTab advancementTab = getTab(dependent);
		if (advancementTab != null) {
			advancementTab.addAdvancement(dependent);
		}
	}

	@Override
	public void onDependentRemoved(PlacedAdvancement dependent) {
	}

	@Override
	public void setProgress(PlacedAdvancement advancement, AdvancementProgress progress) {
		AdvancementWidget advancementWidget = getAdvancementWidget(advancement);
		if (advancementWidget != null) {
			advancementWidget.setProgress(progress);
		}
	}

	@Override
	public void selectTab(@Nullable AdvancementEntry advancement) {
		selectedTab = tabs.get(advancement);
	}

	@Override
	public void onClear() {
		tabs.clear();
		selectedTab = null;
	}

	public @Nullable AdvancementWidget getAdvancementWidget(PlacedAdvancement advancement) {
		AdvancementTab advancementTab = getTab(advancement);
		return advancementTab == null ? null : advancementTab.getWidget(advancement.getAdvancementEntry());
	}

	private @Nullable AdvancementTab getTab(PlacedAdvancement advancement) {
		PlacedAdvancement root = advancement.getRoot();
		return tabs.get(root.getAdvancementEntry());
	}
}
