package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
@Environment(EnvType.CLIENT)
/**
 * {@code SimpleModel}.
 */
public interface SimpleModel {

	String name();
}
