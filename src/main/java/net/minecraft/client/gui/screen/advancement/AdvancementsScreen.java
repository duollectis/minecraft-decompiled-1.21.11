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

@Environment(EnvType.CLIENT)
/**
 * {@code AdvancementsScreen}.
 */
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
		this.layout.addHeader(ADVANCEMENTS_TEXT, this.textRenderer);
		this.tabs.clear();
		this.selectedTab = null;
		this.advancementHandler.setListener(this);
		if (this.selectedTab == null && !this.tabs.isEmpty()) {
			AdvancementTab advancementTab = this.tabs.values().iterator().next();
			this.advancementHandler.selectTab(advancementTab.getRoot().getAdvancementEntry(), true);
		}
		else {
			this.advancementHandler.selectTab(
					this.selectedTab == null ? null : this.selectedTab.getRoot().getAdvancementEntry(),
					true
			);
		}

		this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close()).width(200).build());
		this.layout.forEachChild(child -> {
			ClickableWidget var10000 = this.addDrawableChild(child);
		});
		this.refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		this.layout.refreshPositions();
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	@Override
	public void removed() {
		this.advancementHandler.setListener(null);
		ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.getNetworkHandler();
		if (clientPlayNetworkHandler != null) {
			clientPlayNetworkHandler.sendPacket(AdvancementTabC2SPacket.close());
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0) {
			int i = (this.width - 252) / 2;
			int j = (this.height - 140) / 2;

			for (AdvancementTab advancementTab : this.tabs.values()) {
				if (advancementTab.isClickOnTab(i, j, click.x(), click.y())) {
					this.advancementHandler.selectTab(advancementTab.getRoot().getAdvancementEntry(), true);
					break;
				}
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.client.options.advancementsKey.matchesKey(input)) {
			this.client.setScreen(null);
			this.client.mouse.lockCursor();
			return true;
		}
		else {
			return super.keyPressed(input);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		int i = (this.width - 252) / 2;
		int j = (this.height - 140) / 2;
		context.createNewRootLayer();
		this.drawAdvancementTree(context, i, j);
		context.createNewRootLayer();
		this.drawWindow(context, i, j, mouseX, mouseY);
		if (this.movingTab && this.selectedTab != null) {
			if (this.selectedTab.canScrollHorizontally() && this.selectedTab.canScrollVertically()) {
				context.setCursor(StandardCursors.RESIZE_ALL);
			}
			else if (this.selectedTab.canScrollHorizontally()) {
				context.setCursor(StandardCursors.RESIZE_EW);
			}
			else if (this.selectedTab.canScrollVertically()) {
				context.setCursor(StandardCursors.RESIZE_NS);
			}
		}

		this.drawWidgetTooltip(context, mouseX, mouseY, i, j);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (click.button() != 0) {
			this.movingTab = false;
			return false;
		}
		else {
			if (!this.movingTab) {
				this.movingTab = true;
			}
			else if (this.selectedTab != null) {
				this.selectedTab.move(offsetX, offsetY);
			}

			return true;
		}
	}

	@Override
	public boolean mouseReleased(Click click) {
		this.movingTab = false;
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (this.selectedTab != null) {
			this.selectedTab.move(horizontalAmount * 16.0, verticalAmount * 16.0);
			return true;
		}
		else {
			return false;
		}
	}

	private void drawAdvancementTree(DrawContext context, int x, int y) {
		AdvancementTab advancementTab = this.selectedTab;
		if (advancementTab == null) {
			context.fill(x + 9, y + 18, x + 9 + 234, y + 18 + 113, -16777216);
			int i = x + 9 + 117;
			context.drawCenteredTextWithShadow(this.textRenderer, EMPTY_TEXT, i, y + 18 + 56 - 9 / 2, -1);
			context.drawCenteredTextWithShadow(this.textRenderer, SAD_LABEL_TEXT, i, y + 18 + 113 - 9, -1);
		}
		else {
			advancementTab.render(context, x + 9, y + 18);
		}
	}

	public void drawWindow(DrawContext context, int x, int y, int mouseX, int mouseY) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, x, y, 0.0F, 0.0F, 252, 140, 256, 256);
		if (this.tabs.size() > 1) {
			for (AdvancementTab advancementTab : this.tabs.values()) {
				advancementTab.drawBackground(context, x, y, mouseX, mouseY, advancementTab == this.selectedTab);
			}

			for (AdvancementTab advancementTab : this.tabs.values()) {
				advancementTab.drawIcon(context, x, y);
			}
		}

		context.drawText(
				this.textRenderer,
				this.selectedTab != null ? this.selectedTab.getTitle() : ADVANCEMENTS_TEXT,
				x + 8,
				y + 6,
				-12566464,
				false
		);
	}

	private void drawWidgetTooltip(DrawContext context, int mouseX, int mouseY, int x, int y) {
		if (this.selectedTab != null) {
			context.getMatrices().pushMatrix();
			context.getMatrices().translate(x + 9, y + 18);
			context.createNewRootLayer();
			this.selectedTab.drawWidgetTooltip(context, mouseX - x - 9, mouseY - y - 18, x, y);
			context.getMatrices().popMatrix();
		}

		if (this.tabs.size() > 1) {
			for (AdvancementTab advancementTab : this.tabs.values()) {
				if (advancementTab.isClickOnTab(x, y, mouseX, mouseY)) {
					context.drawTooltip(this.textRenderer, advancementTab.getTitle(), mouseX, mouseY);
				}
			}
		}
	}

	@Override
	public void onRootAdded(PlacedAdvancement root) {
		AdvancementTab advancementTab = AdvancementTab.create(this.client, this, this.tabs.size(), root);
		if (advancementTab != null) {
			this.tabs.put(root.getAdvancementEntry(), advancementTab);
		}
	}

	@Override
	public void onRootRemoved(PlacedAdvancement root) {
	}

	@Override
	public void onDependentAdded(PlacedAdvancement dependent) {
		AdvancementTab advancementTab = this.getTab(dependent);
		if (advancementTab != null) {
			advancementTab.addAdvancement(dependent);
		}
	}

	@Override
	public void onDependentRemoved(PlacedAdvancement dependent) {
	}

	@Override
	public void setProgress(PlacedAdvancement advancement, AdvancementProgress progress) {
		AdvancementWidget advancementWidget = this.getAdvancementWidget(advancement);
		if (advancementWidget != null) {
			advancementWidget.setProgress(progress);
		}
	}

	@Override
	public void selectTab(@Nullable AdvancementEntry advancement) {
		this.selectedTab = this.tabs.get(advancement);
	}

	@Override
	public void onClear() {
		this.tabs.clear();
		this.selectedTab = null;
	}

	public @Nullable AdvancementWidget getAdvancementWidget(PlacedAdvancement advancement) {
		AdvancementTab advancementTab = this.getTab(advancement);
		return advancementTab == null ? null : advancementTab.getWidget(advancement.getAdvancementEntry());
	}

	private @Nullable AdvancementTab getTab(PlacedAdvancement advancement) {
		PlacedAdvancement placedAdvancement = advancement.getRoot();
		return this.tabs.get(placedAdvancement.getAdvancementEntry());
	}
}
