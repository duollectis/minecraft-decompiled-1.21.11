package net.minecraft.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code MissingSprite}.
 */
public final class MissingSprite {

	private static final int WIDTH = 16;
	private static final int HEIGHT = 16;
	private static final String MISSINGNO_ID = "missingno";
	private static final Identifier MISSINGNO = Identifier.ofVanilla("missingno");

	/**
	 * Создаёт image.
	 *
	 * @return NativeImage — результат операции
	 */
	public static NativeImage createImage() {
		return createImage(WIDTH, HEIGHT);
	}

	/**
	 * Создаёт image.
	 *
	 * @param width width
	 * @param height height
	 *
	 * @return NativeImage — результат операции
	 */
	public static NativeImage createImage(int width, int height) {
		NativeImage nativeImage = new NativeImage(width, height, false);
		int i = -524040;

		for (int j = 0; j < height; j++) {
			for (int k = 0; k < width; k++) {
				if (j < height / 2 ^ k < width / 2) {
					nativeImage.setColorArgb(k, j, -524040);
				}
				else {
					nativeImage.setColorArgb(k, j, -16777216);
				}
			}
		}

		return nativeImage;
	}

	/**
	 * Создаёт sprite contents.
	 *
	 * @return SpriteContents — результат операции
	 */
	public static SpriteContents createSpriteContents() {
		NativeImage nativeImage = createImage(WIDTH, HEIGHT);
		return new SpriteContents(MISSINGNO, new SpriteDimensions(WIDTH, HEIGHT), nativeImage);
	}

	public static Identifier getMissingSpriteId() {
		return MISSINGNO;
	}
}
