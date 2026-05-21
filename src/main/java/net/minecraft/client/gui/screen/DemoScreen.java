package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Urls;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
/**
 * {@code DemoScreen}.
 */
public class DemoScreen extends Screen {

	private static final Identifier DEMO_BG = Identifier.ofVanilla("textures/gui/demo_background.png");
	private static final int DEMO_BG_WIDTH = 256;
	private static final int DEMO_BG_HEIGHT = 256;
	private static final int TEXT_COLOR = -14737633;
	private MultilineText movementText = MultilineText.EMPTY;
	private MultilineText fullWrappedText = MultilineText.EMPTY;

	public DemoScreen() {
		super(Text.translatable("demo.help.title"));
	}

	@Override
	protected void init() {
		int i = -16;
		this.addDrawableChild(ButtonWidget.builder(
				Text.translatable("demo.help.buy"), button -> {
					button.active = false;
					Util.getOperatingSystem().open(Urls.BUY_JAVA);
				}
		).dimensions(this.width / 2 - 116, this.height / 2 + 62 + -16, 114, 20).build());
		this.addDrawableChild(ButtonWidget.builder(
				Text.translatable("demo.help.later"), button -> {
					this.client.setScreen(null);
					this.client.mouse.lockCursor();
				}
		).dimensions(this.width / 2 + 2, this.height / 2 + 62 + -16, 114, 20).build());
		GameOptions gameOptions = this.client.options;
		this.movementText = MultilineText.create(
				this.textRenderer,
				this.applyTextStyle(
						Text.translatable(
								"demo.help.movementShort",
								gameOptions.forwardKey.getBoundKeyLocalizedText(),
								gameOptions.leftKey.getBoundKeyLocalizedText(),
								gameOptions.backKey.getBoundKeyLocalizedText(),
								gameOptions.rightKey.getBoundKeyLocalizedText()
						)
				),
				this.applyTextStyle(Text.translatable("demo.help.movementMouse")),
				this.applyTextStyle(Text.translatable("demo.help.jump", gameOptions.jumpKey.getBoundKeyLocalizedText())),
				this.applyTextStyle(Text.translatable(
						"demo.help.inventory",
						gameOptions.inventoryKey.getBoundKeyLocalizedText()
				))
		);
		this.fullWrappedText =
				MultilineText.create(
						this.textRenderer,
						Text.translatable("demo.help.fullWrapped").withoutShadow().withColor(-14737633),
						218
				);
	}

	private Text applyTextStyle(MutableText mutableText) {
		return mutableText.withoutShadow().withColor(-11579569);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.renderBackground(context, mouseX, mouseY, deltaTicks);
		int i = (this.width - 248) / 2;
		int j = (this.height - 166) / 2;
		context.drawTexture(RenderPipelines.GUI_TEXTURED, DEMO_BG, i, j, 0.0F, 0.0F, 248, 166, 256, 256);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		int i = (this.width - 248) / 2 + 10;
		int j = (this.height - 166) / 2 + 8;
		DrawnTextConsumer drawnTextConsumer = context.getTextConsumer();
		context.drawText(this.textRenderer, this.title, i, j, -14737633, false);
		j = this.movementText.draw(Alignment.LEFT, i, j + 12, 12, drawnTextConsumer);
		this.fullWrappedText.draw(Alignment.LEFT, i, j + 20, 9, drawnTextConsumer);
	}
}
