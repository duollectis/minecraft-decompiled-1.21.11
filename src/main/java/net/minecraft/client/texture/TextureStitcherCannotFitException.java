package net.minecraft.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Collection;
import java.util.Locale;

@Environment(EnvType.CLIENT)
/**
 * {@code TextureStitcherCannotFitException}.
 */
public class TextureStitcherCannotFitException extends RuntimeException {

	private final Collection<TextureStitcher.Stitchable> sprites;

	public TextureStitcherCannotFitException(
			TextureStitcher.Stitchable sprite,
			Collection<TextureStitcher.Stitchable> sprites
	) {
		super(
				String.format(
						Locale.ROOT,
						"Unable to fit: %s - size: %dx%d - Maybe try a lower resolution resourcepack?",
						sprite.getId(),
						sprite.getWidth(),
						sprite.getHeight()
				)
		);
		this.sprites = sprites;
	}

	public Collection<TextureStitcher.Stitchable> getSprites() {
		return this.sprites;
	}
}
