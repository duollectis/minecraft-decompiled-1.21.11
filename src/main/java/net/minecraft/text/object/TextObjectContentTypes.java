package net.minecraft.text.object;

import com.mojang.serialization.MapCodec;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.dynamic.Codecs;

/**
 * Реестр типов содержимого встроенных объектов в текстовых компонентах.
 *
 * <p>Регистрирует два встроенных типа: {@code "atlas"} ({@link AtlasTextObjectContents})
 * и {@code "player"} ({@link PlayerTextObjectContents}).
 * Диспетчеризация при сериализации выполняется по полю {@code "object"}.</p>
 */
public class TextObjectContentTypes {

	private static final Codecs.IdMapper<String, MapCodec<? extends TextObjectContents>> ID_MAPPER = new Codecs.IdMapper<>();
	public static final MapCodec<TextObjectContents> CODEC = TextCodecs.dispatchingCodec(
			ID_MAPPER,
			TextObjectContents::getCodec,
			"object"
	);

	static {
		ID_MAPPER.put("atlas", AtlasTextObjectContents.CODEC);
		ID_MAPPER.put("player", PlayerTextObjectContents.CODEC);
	}
}
