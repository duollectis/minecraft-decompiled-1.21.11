package net.minecraft.client.realms.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ScrollableTextWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Urls;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code BuyRealmsScreen}.
 */
public class BuyRealmsScreen extends RealmsScreen {

	private static final Text POPUP_TEXT = Text.translatable("mco.selectServer.popup");
	private static final Text CLOSE_TEXT = Text.translatable("mco.selectServer.close");
	private static final Identifier POPUP_BACKGROUND_TEXTURE = Identifier.ofVanilla("popup/background");
	private static final Identifier TRIAL_AVAILABLE_TEXTURE = Identifier.ofVanilla("icon/trial_available");
	private static final ButtonTextures CROSS_BUTTON_TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("widget/cross_button"), Identifier.ofVanilla("widget/cross_button_highlighted")
	);
	private static final int IMAGE_WIDTH = 195;
	private static final int IMAGE_HEIGHT = 152;
	private static final int LOGO_PADDING = 6;
	private static final int CONTENT_PADDING = 4;
	private static final int BUTTON_MARGIN = 10;
	private static final int POPUP_WIDTH = 320;
	private static final int POPUP_HEIGHT = 172;
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 99;
	private static final int IMAGE_DISPLAY_TICKS = 100;
	private static List<Identifier> realmsImages = List.of();
	private final Screen parent;
	private final boolean trialAvailable;
	private @Nullable ButtonWidget trialButton;
	private int realmsImageIndex;
	private int realmsImageDisplayTime;

	public BuyRealmsScreen(Screen parent, boolean trialAvailable) {
		super(POPUP_TEXT);
		this.parent = parent;
		this.trialAvailable = trialAvailable;
	}

	public static void refreshImages(ResourceManager resourceManager) {
		Collection<Identifier>
				collection =
				resourceManager.findResources("textures/gui/images", id -> id.getPath().endsWith(".png")).keySet();
		realmsImages = collection.stream().filter(id -> id.getNamespace().equals("realms")).toList();
	}

	@Override
	protected void init() {
		this.parent.resize(this.width, this.height);
		if (this.trialAvailable) {
			this.trialButton = this.addDrawableChild(
					ButtonWidget
							.builder(
									Text.translatable("mco.selectServer.trial"),
									ConfirmLinkScreen.opening(this, Urls.JAVA_REALMS_TRIAL)
							)
							.dimensions(this.getRight() - BUTTON_MARGIN - BUTTON_HEIGHT, this.getBottom() - BUTTON_MARGIN - CONTENT_PADDING - 40, BUTTON_HEIGHT, 20)
							.build()
			);
		}

		this.addDrawableChild(
				ButtonWidget
						.builder(
								Text.translatable("mco.selectServer.buy"),
								ConfirmLinkScreen.opening(this, Urls.BUY_JAVA_REALMS)
						)
						.dimensions(this.getRight() - BUTTON_MARGIN - BUTTON_HEIGHT, this.getBottom() - BUTTON_MARGIN - 20, BUTTON_HEIGHT, 20)
						.build()
		);
		TexturedButtonWidget texturedButtonWidget = this.addDrawableChild(
				new TexturedButtonWidget(
						this.getLeft() + 4,
						this.getTop() + 4,
						14,
						14,
						CROSS_BUTTON_TEXTURES,
						button -> this.close(),
						CLOSE_TEXT
				)
		);
		texturedButtonWidget.setTooltip(Tooltip.of(CLOSE_TEXT));
		int i = 142 - (this.trialAvailable ? 40 : 20);
		ScrollableTextWidget scrollableTextWidget = new ScrollableTextWidget(
				this.getRight() - BUTTON_MARGIN - BUTTON_WIDTH, this.getTop() + BUTTON_MARGIN, BUTTON_WIDTH, i, POPUP_TEXT, this.textRenderer
		);
		if (scrollableTextWidget.textOverflows()) {
			scrollableTextWidget.setWidth(94);
		}

		this.addDrawableChild(scrollableTextWidget);
	}

	@Override
	public void tick() {
		super.tick();
		if (++this.realmsImageDisplayTime > IMAGE_DISPLAY_TICKS) {
			this.realmsImageDisplayTime = 0;
			this.realmsImageIndex = (this.realmsImageIndex + 1) % realmsImages.size();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		if (this.trialButton != null) {
			drawTrialAvailableTexture(context, this.trialButton);
		}
	}

	public static void drawTrialAvailableTexture(DrawContext context, ButtonWidget button) {
		int i = 8;
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				TRIAL_AVAILABLE_TEXTURE,
				button.getX() + button.getWidth() - 8 - 4,
				button.getY() + button.getHeight() / 2 - 4,
				8,
				8
		);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		this.parent.renderBackground(context, -1, -1, deltaTicks);
		context.createNewRootLayer();
		this.parent.render(context, -1, -1, deltaTicks);
		context.createNewRootLayer();
		this.renderInGameBackground(context);
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				POPUP_BACKGROUND_TEXTURE,
				this.getLeft(),
				this.getTop(),
				POPUP_WIDTH,
				POPUP_HEIGHT
		);
		if (!realmsImages.isEmpty()) {
			context.drawTexture(
					RenderPipelines.GUI_TEXTURED,
					realmsImages.get(this.realmsImageIndex),
					this.getLeft() + 10,
					this.getTop() + 10,
					0.0F,
					0.0F,
					195,
					152,
					195,
					152
			);
		}
	}

	private int getLeft() {
		return (this.width - POPUP_WIDTH) / 2;
	}

	private int getTop() {
		return (this.height - POPUP_HEIGHT) / 2;
	}

	private int getRight() {
		return this.getLeft() + POPUP_WIDTH;
	}

	private int getBottom() {
		return this.getTop() + POPUP_HEIGHT;
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}
}
