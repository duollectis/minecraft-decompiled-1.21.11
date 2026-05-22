package net.minecraft.client.font;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringIdentifiable;

/**
 * Перечисление всех поддерживаемых типов шрифтов. Каждый тип связан
 * со своим {@link MapCodec} для десериализации конфигурации из {@code fonts.json}.
 */
@Environment(EnvType.CLIENT)
public enum FontType implements StringIdentifiable {
	BITMAP("bitmap", BitmapFont.Loader.CODEC),
	TTF("ttf", TrueTypeFontLoader.CODEC),
	SPACE("space", SpaceFont.Loader.CODEC),
	UNIHEX("unihex", UnihexFont.Loader.CODEC),
	REFERENCE("reference", ReferenceFont.CODEC);

	public static final Codec<FontType> CODEC = StringIdentifiable.createCodec(FontType::values);
	private final String id;
	private final MapCodec<? extends FontLoader> loaderCodec;

	FontType(final String id, final MapCodec<? extends FontLoader> loaderCodec) {
		this.id = id;
		this.loaderCodec = loaderCodec;
	}

	@Override
	public String asString() {
		return id;
	}

	public MapCodec<? extends FontLoader> getLoaderCodec() {
		return loaderCodec;
	}
}
