package com.mojang.blaze3d.textures;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
/**
 * {@code TextureFormat}.
 */
public enum TextureFormat {
	RGBA8(4),
	RED8(1),
	RED8I(1),
	DEPTH32(4);

	private final int pixelSize;

	private TextureFormat(final int pixelSie) {
		this.pixelSize = pixelSie;
	}

	public int pixelSize() {
		return this.pixelSize;
	}

	public boolean hasColorAspect() {
		return this == RGBA8 || this == RED8;
	}

	public boolean hasDepthAspect() {
		return this == DEPTH32;
	}
}
