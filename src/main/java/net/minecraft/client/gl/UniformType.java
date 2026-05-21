package net.minecraft.client.gl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code UniformType}.
 */
public enum UniformType {
	UNIFORM_BUFFER("ubo"),
	TEXEL_BUFFER("utb");

	final String name;

	private UniformType(final String name) {
		this.name = name;
	}
}
