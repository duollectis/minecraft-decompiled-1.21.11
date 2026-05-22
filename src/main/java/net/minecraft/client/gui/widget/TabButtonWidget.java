package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Кнопка вкладки в панели навигации. Отображает текстуру, подчёркивание активной вкладки
 * и фоновую текстуру меню для выбранной вкладки.
 */
@Environment(EnvType.CLIENT)
public class TabButtonWidget extends ClickableWidget.InactivityIndicatingWidget {

	private static final ButtonTextures TAB_BUTTON_TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("widget/tab_selected"),
			Identifier.ofVanilla("widget/tab"),
			Identifier.ofVanilla("widget/tab_selected_highlighted"),
			Identifier.ofVanilla("widget/tab_highlighted")
	);
	private static final int TEXT_PADDING = 3;
	private static final int LEFT_PADDING = 1;
	private static final int RIGHT_PADDING = 1;
	private static final int CONTENT_PADDING = 4;
	private static final int LINE_HEIGHT = 2;
	private final TabManager tabManager;
	private final Tab tab;

	public TabButtonWidget(TabManager tabManager, Tab tab, int width, int height) {
		super(0, 0, width, height, tab.getTitle());
		this.tabManager = tabManager;
		this.tab = tab;
	}

	private static final int TEXT_COLOR_ACTIVE = -1;
	private static final int TEXT_COLOR_INACTIVE = -6250336;

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				TAB_BUTTON_TEXTURES.get(isCurrentTab(), isSelected()),
				getX(),
				getY(),
				width,
				height
		);
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		int textColor = active ? TEXT_COLOR_ACTIVE : TEXT_COLOR_INACTIVE;

		if (isCurrentTab()) {
			renderBackgroundTexture(context, getX() + 2, getY() + 2, getRight() - 2, getBottom());
			drawCurrentTabLine(context, textRenderer, textColor);
		}

		drawMessage(context.getHoverListener(this, DrawContext.HoverType.NONE));
		setCursor(context);
	}

	protected void renderBackgroundTexture(DrawContext context, int left, int top, int right, int bottom) {
		Screen.renderBackgroundTexture(
				context,
				Screen.MENU_BACKGROUND_TEXTURE,
				left,
				top,
				0.0F,
				0.0F,
				right - left,
				bottom - top
		);
	}

	private void drawMessage(DrawnTextConsumer textConsumer) {
		int textLeft = getX() + LEFT_PADDING;
		int textTop = getY() + (isCurrentTab() ? 0 : TEXT_PADDING);
		int textRight = getX() + getWidth() - RIGHT_PADDING;
		int textBottom = getY() + getHeight();
		textConsumer.text(getMessage(), textLeft, textRight, textTop, textBottom);
	}

	private void drawCurrentTabLine(DrawContext context, TextRenderer textRenderer, int color) {
		int lineWidth = Math.min(textRenderer.getWidth(getMessage()), getWidth() - CONTENT_PADDING);
		int lineX = getX() + (getWidth() - lineWidth) / 2;
		int lineY = getY() + getHeight() - LINE_HEIGHT;
		context.fill(lineX, lineY, lineX + lineWidth, lineY + 1, color);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, Text.translatable("gui.narrate.tab", tab.getTitle()));
		builder.put(NarrationPart.HINT, tab.getNarratedHint());
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	public Tab getTab() {
		return tab;
	}

	public boolean isCurrentTab() {
		return tabManager.getCurrentTab() == tab;
	}
}
