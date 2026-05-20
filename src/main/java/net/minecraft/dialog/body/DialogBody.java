package net.minecraft.dialog.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.registry.Registries;
import net.minecraft.util.dynamic.Codecs;

public interface DialogBody {
   Codec<DialogBody> CODEC = Registries.DIALOG_BODY_TYPE.getCodec().dispatch(DialogBody::getTypeCodec, mapCodec -> mapCodec);
   Codec<List<DialogBody>> LIST_CODEC = Codecs.listOrSingle(CODEC);

   MapCodec<? extends DialogBody> getTypeCodec();
}
