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

@Environment(EnvType.CLIENT)
/**
 * {@code AdvancementTab}.
 */
public class AdvancementTab {

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
		this.icon = display.getIcon();
		this.title = display.getTitle();
		this.rootWidget = new AdvancementWidget(this, client, root, display);
		this.addWidget(this.rootWidget, root.getAdvancementEntry());
	}

	public AdvancementTabType getType() {
		return this.type;
	}

	public int getIndex() {
		return this.index;
	}

	public PlacedAdvancement getRoot() {
		return this.root;
	}

	public Text getTitle() {
		return this.title;
	}

	public AdvancementDisplay getDisplay() {
		return this.display;
	}

	public void drawBackground(DrawContext context, int x, int y, int mouseX, int mouseY, boolean selected) {
		int i = x + this.type.getTabX(this.index);
		int j = y + this.type.getTabY(this.index);
		this.type.drawBackground(context, i, j, selected, this.index);
		if (!selected && mouseX > i && mouseY > j && mouseX < i + this.type.getWidth()
				&& mouseY < j + this.type.getHeight()) {
			context.setCursor(StandardCursors.POINTING_HAND);
		}
	}

	public void drawIcon(DrawContext context, int x, int y) {
		this.type.drawIcon(context, x, y, this.index, this.icon);
	}

	public void render(DrawContext context, int x, int y) {
		if (!this.initialized) {
			this.originX = 117 - (this.maxPanX + this.minPanX) / 2;
			this.originY = 56 - (this.maxPanY + this.minPanY) / 2;
			this.initialized = true;
		}

		context.enableScissor(x, y, x + 234, y + 113);
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(x, y);
		Identifier
				identifier =
				this.display
						.getBackground()
						.map(AssetInfo.TextureAssetInfo::texturePath)
						.orElse(TextureManager.MISSING_IDENTIFIER);
		int i = MathHelper.floor(this.originX);
		int j = MathHelper.floor(this.originY);
		int k = i % 16;
		int l = j % 16;

		for (int m = -1; m <= 15; m++) {
			for (int n = -1; n <= 8; n++) {
				context.drawTexture(
						RenderPipelines.GUI_TEXTURED,
						identifier,
						k + 16 * m,
						l + 16 * n,
						0.0F,
						0.0F,
						16,
						16,
						16,
						16
				);
			}
		}

		this.rootWidget.renderLines(context, i, j, true);
		this.rootWidget.renderLines(context, i, j, false);
		this.rootWidget.renderWidgets(context, i, j);
		context.getMatrices().popMatrix();
		context.disableScissor();
	}

	public void drawWidgetTooltip(DrawContext context, int mouseX, int mouseY, int x, int y) {
		context.fill(0, 0, 234, 113, MathHelper.floor(this.alpha * 255.0F) << 24);
		boolean bl = false;
		int i = MathHelper.floor(this.originX);
		int j = MathHelper.floor(this.originY);
		if (mouseX > 0 && mouseX < 234 && mouseY > 0 && mouseY < 113) {
			for (AdvancementWidget advancementWidget : this.widgets.values()) {
				if (advancementWidget.shouldRender(i, j, mouseX, mouseY)) {
					bl = true;
					advancementWidget.drawTooltip(context, i, j, this.alpha, x, y);
					break;
				}
			}
		}

		if (bl) {
			this.alpha = MathHelper.clamp(this.alpha + 0.02F, 0.0F, 0.3F);
		}
		else {
			this.alpha = MathHelper.clamp(this.alpha - 0.04F, 0.0F, 1.0F);
		}
	}

	public boolean isClickOnTab(int screenX, int screenY, double mouseX, double mouseY) {
		return this.type.isClickOnTab(screenX, screenY, this.index, mouseX, mouseY);
	}

	public static @Nullable AdvancementTab create(
			MinecraftClient client,
			AdvancementsScreen screen,
			int index,
			PlacedAdvancement root
	) {
		Optional<AdvancementDisplay> optional = root.getAdvancement().display();
		if (optional.isEmpty()) {
			return null;
		}
		else {
			for (AdvancementTabType advancementTabType : AdvancementTabType.values()) {
				if (index < advancementTabType.getTabCount()) {
					return new AdvancementTab(client, screen, advancementTabType, index, root, optional.get());
				}

				index -= advancementTabType.getTabCount();
			}

			return null;
		}
	}

	public void move(double offsetX, double offsetY) {
		if (this.canScrollHorizontally()) {
			this.originX = MathHelper.clamp(this.originX + offsetX, (double) (-(this.maxPanX - 234)), 0.0);
		}

		if (this.canScrollVertically()) {
			this.originY = MathHelper.clamp(this.originY + offsetY, (double) (-(this.maxPanY - 113)), 0.0);
		}
	}

	public boolean canScrollHorizontally() {
		return this.maxPanX - this.minPanX > 234;
	}

	public boolean canScrollVertically() {
		return this.maxPanY - this.minPanY > 113;
	}

	public void addAdvancement(PlacedAdvancement advancement) {
		Optional<AdvancementDisplay> optional = advancement.getAdvancement().display();
		if (!optional.isEmpty()) {
			AdvancementWidget advancementWidget = new AdvancementWidget(this, this.client, advancement, optional.get());
			this.addWidget(advancementWidget, advancement.getAdvancementEntry());
		}
	}

	private void addWidget(AdvancementWidget widget, AdvancementEntry advancement) {
		this.widgets.put(advancement, widget);
		int i = widget.getX();
		int j = i + 28;
		int k = widget.getY();
		int l = k + 27;
		this.minPanX = Math.min(this.minPanX, i);
		this.maxPanX = Math.max(this.maxPanX, j);
		this.minPanY = Math.min(this.minPanY, k);
		this.maxPanY = Math.max(this.maxPanY, l);

		for (AdvancementWidget advancementWidget : this.widgets.values()) {
			advancementWidget.addToTree();
		}
	}

	public @Nullable AdvancementWidget getWidget(AdvancementEntry advancement) {
		return this.widgets.get(advancement);
	}

	public AdvancementsScreen getScreen() {
		return this.screen;
	}
}
