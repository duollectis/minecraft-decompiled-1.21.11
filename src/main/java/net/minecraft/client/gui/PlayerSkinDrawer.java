package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;

/**
 * Утилитарный класс для отрисовки аватара игрока (лицо + шляпа) из текстуры скина.
 */
@Environment(EnvType.CLIENT)
public class PlayerSkinDrawer {

	public static final int FACE_WIDTH = 8;
	public static final int FACE_HEIGHT = 8;
	public static final int FACE_X = 8;
	public static final int FACE_Y = 8;
	public static final int FACE_OVERLAY_X = 40;
	public static final int FACE_OVERLAY_Y = 8;
	public static final int SKIN_TEXTURE_SIZE = 8;
	public static final int SKIN_RENDER_SIZE = 8;
	public static final int SKIN_TEXTURE_WIDTH = 64;
	public static final int SKIN_TEXTURE_HEIGHT = 64;

	public static void draw(DrawContext context, SkinTextures textures, int x, int y, int size) {
		draw(context, textures, x, y, size, -1);
	}

	public static void draw(DrawContext context, SkinTextures textures, int x, int y, int size, int color) {
		draw(context, textures.body().texturePath(), x, y, size, true, false, color);
	}

	public static void draw(
		DrawContext context,
		Identifier texture,
		int x,
		int y,
		int size,
		boolean hatVisible,
		boolean upsideDown,
		int color
	) {
		int faceV = FACE_Y + (upsideDown ? FACE_HEIGHT : 0);
		int faceHeight = FACE_HEIGHT * (upsideDown ? -1 : 1);

		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			texture,
			x, y,
			(float) FACE_X, faceV,
			size, size,
			FACE_WIDTH, faceHeight,
			SKIN_TEXTURE_WIDTH, SKIN_TEXTURE_HEIGHT,
			color
		);

		if (hatVisible) {
			drawHat(context, texture, x, y, size, upsideDown, color);
		}
	}

	private static void drawHat(
		DrawContext context,
		Identifier texture,
		int x,
		int y,
		int size,
		boolean upsideDown,
		int color
	) {
		int hatV = FACE_OVERLAY_Y + (upsideDown ? FACE_HEIGHT : 0);
		int hatHeight = FACE_HEIGHT * (upsideDown ? -1 : 1);

		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			texture,
			x, y,
			(float) FACE_OVERLAY_X, hatV,
			size, size,
			FACE_WIDTH, hatHeight,
			SKIN_TEXTURE_WIDTH, SKIN_TEXTURE_HEIGHT,
			color
		);
	}
}
