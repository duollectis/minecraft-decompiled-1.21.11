package net.minecraft.entity.player;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.AssetInfo;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Набор текстур скина игрока: тело, плащ, элитра и тип модели.
 * Флаг {@code secure} указывает, что текстуры получены с доверенного сервера Mojang.
 */
public record SkinTextures(
	AssetInfo.TextureAsset body,
	AssetInfo.@Nullable TextureAsset cape,
	AssetInfo.@Nullable TextureAsset elytra,
	PlayerSkinType model,
	boolean secure
) {

	/**
	 * Создаёт набор текстур без флага безопасности (для клиентских переопределений).
	 */
	public static SkinTextures create(
		AssetInfo.TextureAsset body,
		AssetInfo.@Nullable TextureAsset cape,
		AssetInfo.@Nullable TextureAsset elytra,
		PlayerSkinType model
	) {
		return new SkinTextures(body, cape, elytra, model, false);
	}

	/**
	 * Применяет переопределение скина поверх текущих текстур.
	 * Поля из {@code override} заменяют соответствующие поля только если они заданы.
	 *
	 * @param override переопределение скина
	 * @return новый объект с применёнными переопределениями, или {@code this} если переопределение пустое
	 */
	public SkinTextures withOverride(SkinOverride override) {
		return override.equals(SkinOverride.EMPTY)
			? this
			: create(
				(AssetInfo.TextureAsset) DataFixUtils.orElse(override.body, body),
				(AssetInfo.TextureAsset) DataFixUtils.orElse(override.cape, cape),
				(AssetInfo.TextureAsset) DataFixUtils.orElse(override.elytra, elytra),
				override.model.orElse(model)
			);
	}

	/**
	 * Частичное переопределение текстур скина. Все поля опциональны —
	 * незаданные поля не перезаписывают базовые текстуры.
	 */
	public record SkinOverride(
		Optional<AssetInfo.TextureAssetInfo> body,
		Optional<AssetInfo.TextureAssetInfo> cape,
		Optional<AssetInfo.TextureAssetInfo> elytra,
		Optional<PlayerSkinType> model
	) {

		public static final SkinOverride EMPTY = new SkinOverride(
			Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
		);
		public static final MapCodec<SkinOverride> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				AssetInfo.TextureAssetInfo.CODEC.optionalFieldOf("texture").forGetter(SkinOverride::body),
				AssetInfo.TextureAssetInfo.CODEC.optionalFieldOf("cape").forGetter(SkinOverride::cape),
				AssetInfo.TextureAssetInfo.CODEC.optionalFieldOf("elytra").forGetter(SkinOverride::elytra),
				PlayerSkinType.CODEC.optionalFieldOf("model").forGetter(SkinOverride::model)
			).apply(instance, SkinOverride::create)
		);
		public static final PacketCodec<ByteBuf, SkinOverride> PACKET_CODEC = PacketCodec.tuple(
			AssetInfo.TextureAssetInfo.PACKET_CODEC.collect(PacketCodecs::optional),
			SkinOverride::body,
			AssetInfo.TextureAssetInfo.PACKET_CODEC.collect(PacketCodecs::optional),
			SkinOverride::cape,
			AssetInfo.TextureAssetInfo.PACKET_CODEC.collect(PacketCodecs::optional),
			SkinOverride::elytra,
			PlayerSkinType.PACKET_CODEC.collect(PacketCodecs::optional),
			SkinOverride::model,
			SkinOverride::create
		);

		/**
		 * Фабричный метод, возвращающий {@link #EMPTY} если все поля пусты,
		 * иначе создаёт новый экземпляр.
		 */
		public static SkinOverride create(
			Optional<AssetInfo.TextureAssetInfo> texture,
			Optional<AssetInfo.TextureAssetInfo> cape,
			Optional<AssetInfo.TextureAssetInfo> elytra,
			Optional<PlayerSkinType> model
		) {
			return texture.isEmpty() && cape.isEmpty() && elytra.isEmpty() && model.isEmpty()
				? EMPTY
				: new SkinOverride(texture, cape, elytra, model);
		}
	}
}
