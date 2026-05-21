package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code CursorMovement}.
 */
public enum CursorMovement {
	ABSOLUTE,
	RELATIVE,
	END;
}
