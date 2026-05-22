package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.random.Random;

/**
 * Отрисовывает логотип Minecraft и надпись «Edition» на экране заставки.
 * Поддерживает пасхалку «Minceraft» с вероятностью 1/10000.
 */
@Environment(EnvType.CLIENT)
public class LogoDrawer {

	public static final Identifier LOGO_TEXTURE = Identifier.ofVanilla("textures/gui/title/minecraft.png");
	public static final Identifier MINCERAFT_TEXTURE = Identifier.ofVanilla("textures/gui/title/minceraft.png");
	public static final Identifier EDITION_TEXTURE = Identifier.ofVanilla("textures/gui/title/edition.png");
	public static final int LOGO_REGION_WIDTH = 256;
	public static final int LOGO_REGION_HEIGHT = 44;
	private static final int LOGO_TEXTURE_WIDTH = 256;
	private static final int LOGO_TEXTURE_HEIGHT = 64;
	private static final int EDITION_REGION_WIDTH = 128;
	private static final int EDITION_REGION_HEIGHT = 14;
	private static final int EDITION_TEXTURE_WIDTH = 128;
	private static final int EDITION_TEXTURE_HEIGHT = 16;
	public static final int LOGO_BASE_Y = 30;
	private static final int LOGO_AND_EDITION_OVERLAP = 7;
	private final boolean minceraft = Random.create().nextFloat() < 1.0E-4;
	private final boolean ignoreAlpha;

	public LogoDrawer(boolean ignoreAlpha) {
		this.ignoreAlpha = ignoreAlpha;
	}

	public void draw(DrawContext context, int screenWidth, float alpha) {
		draw(context, screenWidth, alpha, LOGO_BASE_Y);
	}

	public void draw(DrawContext context, int screenWidth, float alpha, int y) {
		int logoX = screenWidth / 2 - 128;
		float effectiveAlpha = ignoreAlpha ? 1.0F : alpha;
		int color = ColorHelper.getWhite(effectiveAlpha);

		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			minceraft ? MINCERAFT_TEXTURE : LOGO_TEXTURE,
			logoX,
			y,
			0.0F,
			0.0F,
			LOGO_REGION_WIDTH,
			LOGO_REGION_HEIGHT,
			LOGO_REGION_WIDTH,
			LOGO_TEXTURE_HEIGHT,
			color
		);

		int editionX = screenWidth / 2 - LOGO_TEXTURE_HEIGHT;
		int editionY = y + LOGO_REGION_HEIGHT - LOGO_AND_EDITION_OVERLAP;

		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			EDITION_TEXTURE,
			editionX,
			editionY,
			0.0F,
			0.0F,
			EDITION_TEXTURE_WIDTH,
			EDITION_REGION_HEIGHT,
			EDITION_TEXTURE_WIDTH,
			EDITION_TEXTURE_HEIGHT,
			color
		);
	}

	public boolean shouldIgnoreAlpha() {
		return ignoreAlpha;
	}
}
