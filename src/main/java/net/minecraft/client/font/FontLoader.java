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

@Environment(EnvType.CLIENT)
/**
 * {@code FontLoader}.
 */
public interface FontLoader {

	MapCodec<FontLoader> CODEC = FontType.CODEC.dispatchMap(FontLoader::getType, FontType::getLoaderCodec);

	FontType getType();

	Either<FontLoader.Loadable, FontLoader.Reference> build();

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Loadable}.
	 */
	public interface Loadable {

		Font load(ResourceManager resourceManager) throws IOException;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Provider}.
	 */
	public record Provider(FontLoader definition, FontFilterType.FilterMap filter) {

		public static final Codec<FontLoader.Provider> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    FontLoader.CODEC.forGetter(FontLoader.Provider::definition),
						                    FontFilterType.FilterMap.CODEC
								                    .optionalFieldOf("filter", FontFilterType.FilterMap.NO_FILTER)
								                    .forGetter(FontLoader.Provider::filter)
				                    )
				                    .apply(instance, FontLoader.Provider::new)
		);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Reference}.
	 */
	public record Reference(Identifier id) {
	}
}
