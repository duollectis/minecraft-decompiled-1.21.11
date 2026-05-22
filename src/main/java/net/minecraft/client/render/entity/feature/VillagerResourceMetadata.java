package net.minecraft.client.render.entity.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.StringIdentifiable;

/**
 * Метаданные ресурса жителя: описывает, как шляпа взаимодействует с профессиональным головным убором.
 * <p>
 * Хранится в файле {@code villager.mcmeta} рядом с текстурой жителя.
 * Тип {@link HatType} определяет, нужно ли скрывать часть или всю шляпу
 * при надевании профессионального головного убора.
 */
@Environment(EnvType.CLIENT)
public record VillagerResourceMetadata(VillagerResourceMetadata.HatType hatType) {

	public static final Codec<VillagerResourceMetadata> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    VillagerResourceMetadata.HatType.CODEC
							                    .optionalFieldOf("hat", VillagerResourceMetadata.HatType.NONE)
							                    .forGetter(VillagerResourceMetadata::hatType)
			                    )
			                    .apply(instance, VillagerResourceMetadata::new)
	);

	public static final ResourceMetadataSerializer<VillagerResourceMetadata> SERIALIZER =
			new ResourceMetadataSerializer<>("villager", CODEC);

	/** Режим отображения шляпы жителя при наличии профессионального головного убора. */
	@Environment(EnvType.CLIENT)
	public enum HatType implements StringIdentifiable {
		/** Шляпа отображается полностью. */
		NONE("none"),
		/** Верхняя часть шляпы скрыта. */
		PARTIAL("partial"),
		/** Шляпа полностью скрыта. */
		FULL("full");

		public static final Codec<VillagerResourceMetadata.HatType> CODEC =
				StringIdentifiable.createCodec(VillagerResourceMetadata.HatType::values);

		private final String name;

		HatType(final String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
