package net.minecraft.text;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Identifier;

/**
 * {@code StyleSpriteSource}.
 */
public interface StyleSpriteSource {

	Codec<StyleSpriteSource> FONT_CODEC = Identifier.CODEC
			.flatComapMap(
					StyleSpriteSource.Font::new,
					instance -> instance instanceof StyleSpriteSource.Font font
					            ? DataResult.success(font.id())
					            : DataResult.error(() -> "Unsupported font description type: " + instance)
			);

	StyleSpriteSource.Font DEFAULT = new StyleSpriteSource.Font(Identifier.ofVanilla("default"));

	/**
	 * {@code Font}.
	 */
	public record Font(Identifier id) implements StyleSpriteSource {
	}

	/**
	 * {@code Player}.
	 */
	public record Player(ProfileComponent profile, boolean hat) implements StyleSpriteSource {
	}

	/**
	 * {@code Sprite}.
	 */
	public record Sprite(Identifier atlasId, Identifier spriteId) implements StyleSpriteSource {
	}
}
