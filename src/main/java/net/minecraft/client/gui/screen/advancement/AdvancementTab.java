package net.minecraft.client.gui.screen.advancement;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Вкладка экрана достижений, отображающая дерево достижений одной категории.
 * Поддерживает прокрутку и перетаскивание дерева.
 */
@Environment(EnvType.CLIENT)
public class AdvancementTab {

	private static final int PAGE_WIDTH = 234;
	private static final int PAGE_HEIGHT = 113;
	private static final int TILE_SIZE = 16;
	private static final int TILE_COLS = 15;
	private static final int TILE_ROWS = 8;
	private static final int CENTER_X = 117;
	private static final int CENTER_Y = 56;
	private static final int WIDGET_WIDTH = 28;
	private static final int WIDGET_HEIGHT = 27;
	private static final float ALPHA_FADE_IN = 0.02F;
	private static final float ALPHA_FADE_OUT = 0.04F;
	private static final float ALPHA_MAX_TOOLTIP = 0.3F;

	private final MinecraftClient client;
	private final AdvancementsScreen screen;
	private final AdvancementTabType type;
	private final int index;
	private final PlacedAdvancement root;
	private final AdvancementDisplay display;
	private final ItemStack icon;
	private final Text title;
	private final AdvancementWidget rootWidget;
	private final Map<AdvancementEntry, AdvancementWidget> widgets = Maps.newLinkedHashMap();
	private double originX;
	private double originY;
	private int minPanX = Integer.MAX_VALUE;
	private int minPanY = Integer.MAX_VALUE;
	private int maxPanX = Integer.MIN_VALUE;
	private int maxPanY = Integer.MIN_VALUE;
	private float alpha;
	private boolean initialized;

	public AdvancementTab(
		MinecraftClient client,
		AdvancementsScreen screen,
		AdvancementTabType type,
		int index,
		PlacedAdvancement root,
		AdvancementDisplay display
	) {
		this.client = client;
		this.screen = screen;
		this.type = type;
		this.index = index;
		this.root = root;
		this.display = display;
		icon = display.getIcon();
		title = display.getTitle();
		rootWidget = new AdvancementWidget(this, client, root, display);
		addWidget(rootWidget, root.getAdvancementEntry());
	}

	public AdvancementTabType getType() {
		return type;
	}

	public int getIndex() {
		return index;
	}

	public PlacedAdvancement getRoot() {
		return root;
	}

	public Text getTitle() {
		return title;
	}

	public AdvancementDisplay getDisplay() {
		return display;
	}

	public void drawBackground(DrawContext context, int x, int y, int mouseX, int mouseY, boolean selected) {
		int tabX = x + type.getTabX(index);
		int tabY = y + type.getTabY(index);
		type.drawBackground(context, tabX, tabY, selected, index);
		if (!selected && mouseX > tabX && mouseY > tabY
			&& mouseX < tabX + type.getWidth()
			&& mouseY < tabY + type.getHeight()) {
			context.setCursor(StandardCursors.POINTING_HAND);
		}
	}

	public void drawIcon(DrawContext context, int x, int y) {
		type.drawIcon(context, x, y, index, icon);
	}

	public void render(DrawContext context, int x, int y) {
		if (!initialized) {
			originX = CENTER_X - (maxPanX + minPanX) / 2;
			originY = CENTER_Y - (maxPanY + minPanY) / 2;
			initialized = true;
		}

		context.enableScissor(x, y, x + PAGE_WIDTH, y + PAGE_HEIGHT);
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(x, y);

		Identifier backgroundTexture = display
			.getBackground()
			.map(AssetInfo.TextureAssetInfo::texturePath)
			.orElse(TextureManager.MISSING_IDENTIFIER);
		int originFloorX = MathHelper.floor(originX);
		int originFloorY = MathHelper.floor(originY);
		int tileOffsetX = originFloorX % TILE_SIZE;
		int tileOffsetY = originFloorY % TILE_SIZE;

		for (int col = -1; col <= TILE_COLS; col++) {
			for (int row = -1; row <= TILE_ROWS; row++) {
				context.drawTexture(
					RenderPipelines.GUI_TEXTURED,
					backgroundTexture,
					tileOffsetX + TILE_SIZE * col,
					tileOffsetY + TILE_SIZE * row,
					0.0F,
					0.0F,
					TILE_SIZE,
					TILE_SIZE,
					TILE_SIZE,
					TILE_SIZE
				);
			}
		}

		rootWidget.renderLines(context, originFloorX, originFloorY, true);
		rootWidget.renderLines(context, originFloorX, originFloorY, false);
		rootWidget.renderWidgets(context, originFloorX, originFloorY);
		context.getMatrices().popMatrix();
		context.disableScissor();
	}

	public void drawWidgetTooltip(DrawContext context, int mouseX, int mouseY, int x, int y) {
		context.fill(0, 0, PAGE_WIDTH, PAGE_HEIGHT, MathHelper.floor(alpha * 255.0F) << 24);
		boolean tooltipShown = false;
		int originFloorX = MathHelper.floor(originX);
		int originFloorY = MathHelper.floor(originY);

		if (mouseX > 0 && mouseX < PAGE_WIDTH && mouseY > 0 && mouseY < PAGE_HEIGHT) {
			for (AdvancementWidget widget : widgets.values()) {
				if (widget.shouldRender(originFloorX, originFloorY, mouseX, mouseY)) {
					tooltipShown = true;
					widget.drawTooltip(context, originFloorX, originFloorY, alpha, x, y);
					break;
				}
			}
		}

		alpha = tooltipShown
			? MathHelper.clamp(alpha + ALPHA_FADE_IN, 0.0F, ALPHA_MAX_TOOLTIP)
			: MathHelper.clamp(alpha - ALPHA_FADE_OUT, 0.0F, 1.0F);
	}

	public boolean isClickOnTab(int screenX, int screenY, double mouseX, double mouseY) {
		return type.isClickOnTab(screenX, screenY, index, mouseX, mouseY);
	}

	public static @Nullable AdvancementTab create(
		MinecraftClient client,
		AdvancementsScreen screen,
		int index,
		PlacedAdvancement root
	) {
		Optional<AdvancementDisplay> displayOpt = root.getAdvancement().display();
		if (displayOpt.isEmpty()) {
			return null;
		}

		for (AdvancementTabType tabType : AdvancementTabType.values()) {
			if (index < tabType.getTabCount()) {
				return new AdvancementTab(client, screen, tabType, index, root, displayOpt.get());
			}

			index -= tabType.getTabCount();
		}

		return null;
	}

	public void move(double offsetX, double offsetY) {
		if (canScrollHorizontally()) {
			originX = MathHelper.clamp(originX + offsetX, -(maxPanX - PAGE_WIDTH), 0.0);
		}

		if (canScrollVertically()) {
			originY = MathHelper.clamp(originY + offsetY, -(maxPanY - PAGE_HEIGHT), 0.0);
		}
	}

	public boolean canScrollHorizontally() {
		return maxPanX - minPanX > PAGE_WIDTH;
	}

	public boolean canScrollVertically() {
		return maxPanY - minPanY > PAGE_HEIGHT;
	}

	public void addAdvancement(PlacedAdvancement advancement) {
		advancement.getAdvancement().display().ifPresent(advDisplay -> {
			AdvancementWidget widget = new AdvancementWidget(this, client, advancement, advDisplay);
			addWidget(widget, advancement.getAdvancementEntry());
		});
	}

	private void addWidget(AdvancementWidget widget, AdvancementEntry advancement) {
		widgets.put(advancement, widget);
		int widgetLeft = widget.getX();
		int widgetRight = widgetLeft + WIDGET_WIDTH;
		int widgetTop = widget.getY();
		int widgetBottom = widgetTop + WIDGET_HEIGHT;
		minPanX = Math.min(minPanX, widgetLeft);
		maxPanX = Math.max(maxPanX, widgetRight);
		minPanY = Math.min(minPanY, widgetTop);
		maxPanY = Math.max(maxPanY, widgetBottom);

		for (AdvancementWidget advWidget : widgets.values()) {
			advWidget.addToTree();
		}
	}

	public @Nullable AdvancementWidget getWidget(AdvancementEntry advancement) {
		return widgets.get(advancement);
	}

	public AdvancementsScreen getScreen() {
		return screen;
	}
}
