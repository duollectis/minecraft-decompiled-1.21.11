package net.minecraft.client.font;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;

/**
 * Интерфейс загрузчика шрифта. Каждый тип шрифта реализует этот интерфейс
 * и возвращает либо загружаемый провайдер ({@link Loadable}), либо ссылку на другой шрифт ({@link Reference}).
 */
@Environment(EnvType.CLIENT)
public interface FontLoader {

	MapCodec<FontLoader> CODEC = FontType.CODEC.dispatchMap(FontLoader::getType, FontType::getLoaderCodec);

	FontType getType();

	Either<FontLoader.Loadable, FontLoader.Reference> build();

	/**
	 * Загружаемый провайдер шрифта — непосредственно читает данные из ресурсов.
	 */
	@Environment(EnvType.CLIENT)
	interface Loadable {

		Font load(ResourceManager resourceManager) throws IOException;
	}

	/**
	 * Провайдер шрифта с привязанной картой фильтров.
	 * Используется при десериализации конфигурации шрифтов из {@code fonts.json}.
	 */
	@Environment(EnvType.CLIENT)
	record Provider(FontLoader definition, FontFilterType.FilterMap filter) {

		public static final Codec<FontLoader.Provider> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						FontLoader.CODEC.forGetter(FontLoader.Provider::definition),
						FontFilterType.FilterMap.CODEC
								.optionalFieldOf("filter", FontFilterType.FilterMap.NO_FILTER)
								.forGetter(FontLoader.Provider::filter)
				).apply(instance, FontLoader.Provider::new)
		);
	}

	/**
	 * Ссылка на другой шрифт по идентификатору — используется для переиспользования уже загруженных шрифтов.
	 */
	@Environment(EnvType.CLIENT)
	record Reference(Identifier id) {
	}
}
