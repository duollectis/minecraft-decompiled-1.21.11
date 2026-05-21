package net.minecraft.client.render.entity.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.StringIdentifiable;

@Environment(EnvType.CLIENT)
/**
 * {@code VillagerResourceMetadata}.
 */
public record VillagerResourceMetadata(VillagerResourceMetadata.HatType hatType) {

	public static final Codec<VillagerResourceMetadata> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    VillagerResourceMetadata.HatType.CODEC
							                    .optionalFieldOf("hat", VillagerResourceMetadata.HatType.NONE)
							                    .forGetter(VillagerResourceMetadata::hatType)
			                    )
			                    .apply(instance, VillagerResourceMetadata::new)
	);
	public static final ResourceMetadataSerializer<VillagerResourceMetadata>
			SERIALIZER =
			new ResourceMetadataSerializer<>("villager", CODEC);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code HatType}.
	 */
	public static enum HatType implements StringIdentifiable {
		NONE("none"),
		PARTIAL("partial"),
		FULL("full");

		public static final Codec<VillagerResourceMetadata.HatType>
				CODEC =
				StringIdentifiable.createCodec(VillagerResourceMetadata.HatType::values);
		private final String name;

		private HatType(final String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return this.name;
		}
	}
}
