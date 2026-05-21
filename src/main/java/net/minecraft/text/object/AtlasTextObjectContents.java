package net.minecraft.text.object;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;

/**
 * {@code AtlasTextObjectContents}.
 */
public record AtlasTextObjectContents(Identifier atlas, Identifier sprite) implements TextObjectContents {

	public static final Identifier DEFAULT_ATLAS = Atlases.BLOCKS;
	public static final MapCodec<AtlasTextObjectContents> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Identifier.CODEC.optionalFieldOf("atlas", DEFAULT_ATLAS).forGetter(AtlasTextObjectContents::atlas),
					                    Identifier.CODEC.fieldOf("sprite").forGetter(AtlasTextObjectContents::sprite)
			                    )
			                    .apply(instance, AtlasTextObjectContents::new)
	);

	@Override
	public MapCodec<AtlasTextObjectContents> getCodec() {
		return CODEC;
	}

	@Override
	public StyleSpriteSource spriteSource() {
		return new StyleSpriteSource.Sprite(this.atlas, this.sprite);
	}

	private static String getShortIdString(Identifier id) {
		return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
	}

	@Override
	public String asText() {
		String string = getShortIdString(this.sprite);
		return this.atlas.equals(DEFAULT_ATLAS) ? "[" + string + "]"
		                                        : "[" + string + "@" + getShortIdString(this.atlas) + "]";
	}
}
