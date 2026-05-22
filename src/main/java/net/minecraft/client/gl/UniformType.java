package net.minecraft.client.gl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Тип uniform-буфера в шейдерной программе.
 * Определяет, как именно данные передаются в шейдер: через UBO или texel-буфер.
 */
@Environment(EnvType.CLIENT)
public enum UniformType {
	UNIFORM_BUFFER("ubo"),
	TEXEL_BUFFER("utb");

	final String name;

	UniformType(String name) {
		this.name = name;
	}
}
