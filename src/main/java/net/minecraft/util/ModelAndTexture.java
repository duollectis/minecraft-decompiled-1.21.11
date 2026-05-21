package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * {@code ModelAndTexture}.
 */
public record ModelAndTexture<T>(T model, AssetInfo.TextureAssetInfo asset) {

	public ModelAndTexture(T model, Identifier assetId) {
		this(model, new AssetInfo.TextureAssetInfo(assetId));
	}

	public static <T> MapCodec<ModelAndTexture<T>> createMapCodec(Codec<T> modelCodec, T model) {
		return RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    modelCodec.optionalFieldOf("model", model).forGetter(ModelAndTexture::model),
						                    AssetInfo.TextureAssetInfo.MAP_CODEC.forGetter(ModelAndTexture::asset)
				                    )
				                    .apply(instance, ModelAndTexture::new)
		);
	}

	public static <T> PacketCodec<RegistryByteBuf, ModelAndTexture<T>> createPacketCodec(PacketCodec<? super RegistryByteBuf, T> modelPacketCodec) {
		return PacketCodec.tuple(
				modelPacketCodec,
				ModelAndTexture::model,
				AssetInfo.TextureAssetInfo.PACKET_CODEC,
				ModelAndTexture::asset,
				ModelAndTexture::new
		);
	}
}
