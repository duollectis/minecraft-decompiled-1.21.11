package net.minecraft.entity.player;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.AssetInfo;
import org.jspecify.annotations.Nullable;

public record SkinTextures(
   AssetInfo.TextureAsset body, AssetInfo.@Nullable TextureAsset cape, AssetInfo.@Nullable TextureAsset elytra, PlayerSkinType model, boolean secure
) {
   public static SkinTextures create(
      AssetInfo.TextureAsset body, AssetInfo.@Nullable TextureAsset cape, AssetInfo.@Nullable TextureAsset elytra, PlayerSkinType model
   ) {
      return new SkinTextures(body, cape, elytra, model, false);
   }

   public SkinTextures withOverride(SkinTextures.SkinOverride override) {
      return override.equals(SkinTextures.SkinOverride.EMPTY)
         ? this
         : create(
            (AssetInfo.TextureAsset)DataFixUtils.orElse(override.body, this.body),
            (AssetInfo.TextureAsset)DataFixUtils.orElse(override.cape, this.cape),
            (AssetInfo.TextureAsset)DataFixUtils.orElse(override.elytra, this.elytra),
            override.model.orElse(this.model)
         );
   }

   public record SkinOverride(
      Optional<AssetInfo.TextureAssetInfo> body,
      Optional<AssetInfo.TextureAssetInfo> cape,
      Optional<AssetInfo.TextureAssetInfo> elytra,
      Optional<PlayerSkinType> model
   ) {
      public static final SkinTextures.SkinOverride EMPTY = new SkinTextures.SkinOverride(
         Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
      );
      public static final MapCodec<SkinTextures.SkinOverride> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               AssetInfo.TextureAssetInfo.CODEC.optionalFieldOf("texture").forGetter(SkinTextures.SkinOverride::body),
               AssetInfo.TextureAssetInfo.CODEC.optionalFieldOf("cape").forGetter(SkinTextures.SkinOverride::cape),
               AssetInfo.TextureAssetInfo.CODEC.optionalFieldOf("elytra").forGetter(SkinTextures.SkinOverride::elytra),
               PlayerSkinType.CODEC.optionalFieldOf("model").forGetter(SkinTextures.SkinOverride::model)
            )
            .apply(instance, SkinTextures.SkinOverride::create)
      );
      public static final PacketCodec<ByteBuf, SkinTextures.SkinOverride> PACKET_CODEC = PacketCodec.tuple(
         AssetInfo.TextureAssetInfo.PACKET_CODEC.collect(PacketCodecs::optional),
         SkinTextures.SkinOverride::body,
         AssetInfo.TextureAssetInfo.PACKET_CODEC.collect(PacketCodecs::optional),
         SkinTextures.SkinOverride::cape,
         AssetInfo.TextureAssetInfo.PACKET_CODEC.collect(PacketCodecs::optional),
         SkinTextures.SkinOverride::elytra,
         PlayerSkinType.PACKET_CODEC.collect(PacketCodecs::optional),
         SkinTextures.SkinOverride::model,
         SkinTextures.SkinOverride::create
      );

      public static SkinTextures.SkinOverride create(
         Optional<AssetInfo.TextureAssetInfo> texture,
         Optional<AssetInfo.TextureAssetInfo> cape,
         Optional<AssetInfo.TextureAssetInfo> elytra,
         Optional<PlayerSkinType> model
      ) {
         return texture.isEmpty() && cape.isEmpty() && elytra.isEmpty() && model.isEmpty()
            ? EMPTY
            : new SkinTextures.SkinOverride(texture, cape, elytra, model);
      }
   }
}
