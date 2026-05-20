package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;

public interface AssetInfo {
   Identifier id();

   public record SkinAssetInfo(Identifier texturePath, String url) implements AssetInfo.TextureAsset {
      @Override
      public Identifier id() {
         return this.texturePath;
      }
   }

   public interface TextureAsset extends AssetInfo {
      Identifier texturePath();
   }

   public record TextureAssetInfo(Identifier id, Identifier texturePath) implements AssetInfo.TextureAsset {
      public static final Codec<AssetInfo.TextureAssetInfo> CODEC = Identifier.CODEC.xmap(AssetInfo.TextureAssetInfo::new, AssetInfo.TextureAssetInfo::id);
      public static final MapCodec<AssetInfo.TextureAssetInfo> MAP_CODEC = CODEC.fieldOf("asset_id");
      public static final PacketCodec<ByteBuf, AssetInfo.TextureAssetInfo> PACKET_CODEC = Identifier.PACKET_CODEC
         .xmap(AssetInfo.TextureAssetInfo::new, AssetInfo.TextureAssetInfo::id);

      public TextureAssetInfo(Identifier id) {
         this(id, id.withPath(path -> "textures/" + path + ".png"));
      }
   }
}
