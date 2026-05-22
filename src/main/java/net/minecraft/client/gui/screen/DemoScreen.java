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

/**
 * Экран справки демо-режима с подсказками по управлению и кнопками покупки/закрытия.
 */
@Environment(EnvType.CLIENT)
public class DemoScreen extends Screen {

	private static final Identifier DEMO_BG = Identifier.ofVanilla("textures/gui/demo_background.png");
	private static final int DEMO_BG_WIDTH = 256;
	private static final int DEMO_BG_HEIGHT = 256;
	private static final int PANEL_WIDTH = 248;
	private static final int PANEL_HEIGHT = 166;
	private static final int PANEL_TEXT_PADDING = 10;
	private static final int PANEL_TITLE_PADDING = 8;
	private static final int MOVEMENT_LINE_HEIGHT = 12;
	private static final int FULL_TEXT_LINE_HEIGHT = 9;
	private static final int FULL_TEXT_GAP = 20;
	private static final int FULL_TEXT_MAX_WIDTH = 218;
	private static final int BUTTON_WIDTH = 114;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_Y_OFFSET = -16;
	private static final int BUTTON_Y_FROM_CENTER = 62;
	private static final int BUTTON_LEFT_X_OFFSET = -116;
	private static final int BUTTON_RIGHT_X_OFFSET = 2;
	private static final int TEXT_COLOR = -14737633;
	private static final int MOVEMENT_TEXT_COLOR = -11579569;

	private MultilineText movementText = MultilineText.EMPTY;
	private MultilineText fullWrappedText = MultilineText.EMPTY;

	public DemoScreen() {
		super(Text.translatable("demo.help.title"));
	}

	@Override
	protected void init() {
		int buttonY = height / 2 + BUTTON_Y_FROM_CENTER + BUTTON_Y_OFFSET;
		addDrawableChild(ButtonWidget.builder(
			Text.translatable("demo.help.buy"), button -> {
				button.active = false;
				Util.getOperatingSystem().open(Urls.BUY_JAVA);
			}
		).dimensions(width / 2 + BUTTON_LEFT_X_OFFSET, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

		addDrawableChild(ButtonWidget.builder(
			Text.translatable("demo.help.later"), button -> {
				client.setScreen(null);
				client.mouse.lockCursor();
			}
		).dimensions(width / 2 + BUTTON_RIGHT_X_OFFSET, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

		GameOptions gameOptions = client.options;
		movementText = MultilineText.create(
			textRenderer,
			applyTextStyle(Text.translatable(
				"demo.help.movementShort",
				gameOptions.forwardKey.getBoundKeyLocalizedText(),
				gameOptions.leftKey.getBoundKeyLocalizedText(),
				gameOptions.backKey.getBoundKeyLocalizedText(),
				gameOptions.rightKey.getBoundKeyLocalizedText()
			)),
			applyTextStyle(Text.translatable("demo.help.movementMouse")),
			applyTextStyle(Text.translatable("demo.help.jump", gameOptions.jumpKey.getBoundKeyLocalizedText())),
			applyTextStyle(Text.translatable("demo.help.inventory", gameOptions.inventoryKey.getBoundKeyLocalizedText()))
		);
		fullWrappedText = MultilineText.create(
			textRenderer,
			Text.translatable("demo.help.fullWrapped").withoutShadow().withColor(TEXT_COLOR),
			FULL_TEXT_MAX_WIDTH
		);
	}

	private Text applyTextStyle(MutableText text) {
		return text.withoutShadow().withColor(MOVEMENT_TEXT_COLOR);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.renderBackground(context, mouseX, mouseY, deltaTicks);
		int panelX = (width - PANEL_WIDTH) / 2;
		int panelY = (height - PANEL_HEIGHT) / 2;
		context.drawTexture(RenderPipelines.GUI_TEXTURED, DEMO_BG, panelX, panelY, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, DEMO_BG_WIDTH, DEMO_BG_HEIGHT);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		int textX = (width - PANEL_WIDTH) / 2 + PANEL_TEXT_PADDING;
		int textY = (height - PANEL_HEIGHT) / 2 + PANEL_TITLE_PADDING;
		DrawnTextConsumer textConsumer = context.getTextConsumer();

		context.drawText(textRenderer, title, textX, textY, TEXT_COLOR, false);
		textY = movementText.draw(Alignment.LEFT, textX, textY + MOVEMENT_LINE_HEIGHT, MOVEMENT_LINE_HEIGHT, textConsumer);
		fullWrappedText.draw(Alignment.LEFT, textX, textY + FULL_TEXT_GAP, FULL_TEXT_LINE_HEIGHT, textConsumer);
	}
}
