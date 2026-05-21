package net.minecraft.resource.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resource.PackVersion;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.dynamic.Range;

/**
 * {@code PackResourceMetadata}.
 */
public record PackResourceMetadata(Text description, Range<PackVersion> supportedFormats) {

	private static final Codec<PackResourceMetadata> DESCRIPTION_CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(TextCodecs.CODEC.fieldOf("description").forGetter(PackResourceMetadata::description))
					.apply(instance,
							description -> new PackResourceMetadata(
									description,
									new Range<>(PackVersion.of(Integer.MAX_VALUE))
							)
					)
	);
	public static final ResourceMetadataSerializer<PackResourceMetadata>
			CLIENT_RESOURCES_SERIALIZER =
			new ResourceMetadataSerializer<>(
					"pack", createCodec(ResourceType.CLIENT_RESOURCES)
			);
	public static final ResourceMetadataSerializer<PackResourceMetadata>
			SERVER_DATA_SERIALIZER =
			new ResourceMetadataSerializer<>(
					"pack", createCodec(ResourceType.SERVER_DATA)
			);
	public static final ResourceMetadataSerializer<PackResourceMetadata>
			DESCRIPTION_SERIALIZER =
			new ResourceMetadataSerializer<>("pack", DESCRIPTION_CODEC);

	private static Codec<PackResourceMetadata> createCodec(ResourceType type) {
		return RecordCodecBuilder.create(
				instance -> instance.group(
						                    TextCodecs.CODEC.fieldOf("description").forGetter(PackResourceMetadata::description),
						                    PackVersion.createRangeCodec(type).forGetter(PackResourceMetadata::supportedFormats)
				                    )
				                    .apply(instance, PackResourceMetadata::new)
		);
	}

	public static ResourceMetadataSerializer<PackResourceMetadata> getSerializerFor(ResourceType type) {
		return switch (type) {
			case CLIENT_RESOURCES -> CLIENT_RESOURCES_SERIALIZER;
			case SERVER_DATA -> SERVER_DATA_SERIALIZER;
		};
	}
}
