package net.minecraft.text;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.Codecs;

public class NbtDataSourceTypes {
   private static final Codecs.IdMapper<String, MapCodec<? extends NbtDataSource>> ID_MAPPER = new Codecs.IdMapper<>();
   public static final MapCodec<NbtDataSource> CODEC = TextCodecs.dispatchingCodec(ID_MAPPER, NbtDataSource::getCodec, "source");

   static {
      ID_MAPPER.put("entity", EntityNbtDataSource.CODEC);
      ID_MAPPER.put("block", BlockNbtDataSource.CODEC);
      ID_MAPPER.put("storage", StorageNbtDataSource.CODEC);
   }
}
