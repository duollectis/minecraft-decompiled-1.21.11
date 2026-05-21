package net.minecraft.client.resource.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.MipmapStrategy;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;

@Environment(EnvType.CLIENT)
/**
 * {@code TextureResourceMetadata}.
 */
public record TextureResourceMetadata(
		boolean blur,
		boolean clamp,
		MipmapStrategy mipmapStrategy,
		float alphaCutoffBias
) {

	public static final boolean DEFAULT_BLUR = false;
	public static final boolean DEFAULT_CLAMP = false;
	public static final float DEFAULT_ALPHA_CUTOFF_BIAS = 0.0F;
	public static final Codec<TextureResourceMetadata> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codec.BOOL.optionalFieldOf("blur", false).forGetter(TextureResourceMetadata::blur),
					                    Codec.BOOL.optionalFieldOf("clamp", false).forGetter(TextureResourceMetadata::clamp),
					                    MipmapStrategy.CODEC
							                    .optionalFieldOf("mipmap_strategy", MipmapStrategy.AUTO)
							                    .forGetter(TextureResourceMetadata::mipmapStrategy),
					                    Codec.FLOAT
							                    .optionalFieldOf("alpha_cutoff_bias", 0.0F)
							                    .forGetter(TextureResourceMetadata::alphaCutoffBias)
			                    )
			                    .apply(instance, TextureResourceMetadata::new)
	);
	public static final ResourceMetadataSerializer<TextureResourceMetadata>
			SERIALIZER =
			new ResourceMetadataSerializer<>("texture", CODEC);
}
