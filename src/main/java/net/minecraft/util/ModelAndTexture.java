package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * Пара «модель + текстура», используемая для описания визуального представления объектов.
 * Хранит ссылку на модель произвольного типа и информацию о текстурном ассете.
 *
 * @param <T> тип модели
 */
public record ModelAndTexture<T>(T model, AssetInfo.TextureAssetInfo asset) {

	public ModelAndTexture(T model, Identifier assetId) {
		this(model, new AssetInfo.TextureAssetInfo(assetId));
	}

	/**
	 * Создаёт {@link MapCodec} для сериализации, где поле {@code model} является опциональным
	 * с заданным значением по умолчанию.
	 *
	 * @param modelCodec кодек для типа модели
	 * @param defaultModel значение модели по умолчанию
	 * @return кодек для {@link ModelAndTexture}
	 */
	public static <T> MapCodec<ModelAndTexture<T>> createMapCodec(Codec<T> modelCodec, T defaultModel) {
		return RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				modelCodec.optionalFieldOf("model", defaultModel).forGetter(ModelAndTexture::model),
				AssetInfo.TextureAssetInfo.MAP_CODEC.forGetter(ModelAndTexture::asset)
			).apply(instance, ModelAndTexture::new)
		);
	}

	/**
	 * Создаёт {@link PacketCodec} для сетевой передачи пары модель+текстура.
	 *
	 * @param modelPacketCodec кодек для сетевой передачи модели
	 * @return пакетный кодек для {@link ModelAndTexture}
	 */
	public static <T> PacketCodec<RegistryByteBuf, ModelAndTexture<T>> createPacketCodec(
		PacketCodec<? super RegistryByteBuf, T> modelPacketCodec
	) {
		return PacketCodec.tuple(
			modelPacketCodec,
			ModelAndTexture::model,
			AssetInfo.TextureAssetInfo.PACKET_CODEC,
			ModelAndTexture::asset,
			ModelAndTexture::new
		);
	}
}
