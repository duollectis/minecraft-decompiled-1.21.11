package net.minecraft.text.object;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;

/**
 * Содержимое встроенного объекта, отображающего спрайт из атласа текстур.
 *
 * <p>Поле {@code atlas} по умолчанию равно {@link #DEFAULT_ATLAS} ({@code minecraft:blocks}),
 * что позволяет сокращённо указывать только {@code sprite} для стандартных блочных иконок.
 * Метод {@link #asText()} возвращает компактное строковое представление:
 * {@code [sprite]} для стандартного атласа или {@code [sprite@atlas]} для нестандартного.</p>
 */
public record AtlasTextObjectContents(Identifier atlas, Identifier sprite) implements TextObjectContents {

	/** Атлас по умолчанию — стандартный атлас блоков Minecraft. */
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
		return new StyleSpriteSource.Sprite(atlas, sprite);
	}

	@Override
	public String asText() {
		String spriteString = toShortId(sprite);
		return atlas.equals(DEFAULT_ATLAS)
				? "[" + spriteString + "]"
				: "[" + spriteString + "@" + toShortId(atlas) + "]";
	}

	/** Возвращает краткую форму идентификатора: только путь для {@code minecraft:}, полный — для остальных. */
	private static String toShortId(Identifier id) {
		return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
	}
}
