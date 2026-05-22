package net.minecraft.text;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.Codecs;

/**
 * Реестр типов источников NBT-данных для текстовых компонентов.
 *
 * <p>Регистрирует три встроенных типа: {@code "entity"}, {@code "block"} и {@code "storage"}.
 * Диспетчеризация при сериализации выполняется по полю {@code "source"}.</p>
 */
public class NbtDataSourceTypes {

	private static final Codecs.IdMapper<String, MapCodec<? extends NbtDataSource>> ID_MAPPER = new Codecs.IdMapper<>();
	public static final MapCodec<NbtDataSource> CODEC = TextCodecs.dispatchingCodec(
			ID_MAPPER,
			NbtDataSource::getCodec,
			"source"
	);

	static {
		ID_MAPPER.put("entity", EntityNbtDataSource.CODEC);
		ID_MAPPER.put("block", BlockNbtDataSource.CODEC);
		ID_MAPPER.put("storage", StorageNbtDataSource.CODEC);
	}
}
