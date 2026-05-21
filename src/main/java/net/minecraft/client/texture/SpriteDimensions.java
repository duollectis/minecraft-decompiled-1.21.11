package net.minecraft.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code SpriteDimensions}.
 */
public record SpriteDimensions(int width, int height) {
}
