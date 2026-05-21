package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

import java.util.Locale;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockRenderLayerGroup}.
 */
public enum BlockRenderLayerGroup {
	OPAQUE(BlockRenderLayer.SOLID, BlockRenderLayer.CUTOUT),
	TRANSLUCENT(BlockRenderLayer.TRANSLUCENT),
	TRIPWIRE(BlockRenderLayer.TRIPWIRE);

	private final String name;
	private final BlockRenderLayer[] layers;

	private BlockRenderLayerGroup(final BlockRenderLayer... layers) {
		this.layers = layers;
		this.name = this.toString().toLowerCase(Locale.ROOT);
	}

	public String getName() {
		return this.name;
	}

	public BlockRenderLayer[] getLayers() {
		return this.layers;
	}

	public Framebuffer getFramebuffer() {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();

		Framebuffer framebuffer = switch (this) {
			case TRANSLUCENT -> minecraftClient.worldRenderer.getTranslucentFramebuffer();
			case TRIPWIRE -> minecraftClient.worldRenderer.getWeatherFramebuffer();
			default -> minecraftClient.getFramebuffer();
		};
		return framebuffer != null ? framebuffer : minecraftClient.getFramebuffer();
	}
}
