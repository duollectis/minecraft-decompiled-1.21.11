package net.minecraft.dialog.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registries;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;

/**
 * {@code DialogBody}.
 */
public interface DialogBody {

	Codec<DialogBody>
			CODEC =
			Registries.DIALOG_BODY_TYPE.getCodec().dispatch(DialogBody::getTypeCodec, mapCodec -> mapCodec);

	Codec<List<DialogBody>> LIST_CODEC = Codecs.listOrSingle(CODEC);

	MapCodec<? extends DialogBody> getTypeCodec();
}
