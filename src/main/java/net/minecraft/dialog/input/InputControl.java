package net.minecraft.dialog.input;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registries;

/**
 * {@code InputControl}.
 */
public interface InputControl {

	MapCodec<InputControl>
			CODEC =
			Registries.INPUT_CONTROL_TYPE.getCodec().dispatchMap(InputControl::getCodec, mapCodec -> mapCodec);

	MapCodec<? extends InputControl> getCodec();
}
