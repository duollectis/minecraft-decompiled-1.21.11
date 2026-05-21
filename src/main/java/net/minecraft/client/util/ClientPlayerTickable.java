package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code ClientPlayerTickable}.
 */
public interface ClientPlayerTickable {

	void tick();
}
