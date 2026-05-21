package net.minecraft.client.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code TextureSetup}.
 */
public record TextureSetup(
		@Nullable GpuTextureView texure0,
		@Nullable GpuTextureView texure1,
		@Nullable GpuTextureView texure2,
		@Nullable GpuSampler sampler0,
		@Nullable GpuSampler sampler1,
		@Nullable GpuSampler sampler2
) {

	private static final TextureSetup EMPTY = new TextureSetup(null, null, null, null, null, null);
	private static int shuffleSeed;

	public static TextureSetup of(GpuTextureView texture, GpuSampler sampler) {
		return new TextureSetup(texture, null, null, sampler, null, null);
	}

	public static TextureSetup withLightmap(GpuTextureView texture, GpuSampler sampler) {
		return new TextureSetup(
				texture,
				null,
				MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().getGlTextureView(),
				sampler,
				null,
				RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
		);
	}

	public static TextureSetup of(
			GpuTextureView texture0,
			GpuSampler sampler0,
			GpuTextureView texture1,
			GpuSampler sampler1
	) {
		return new TextureSetup(texture0, texture1, null, sampler0, sampler1, null);
	}

	public static TextureSetup empty() {
		return EMPTY;
	}

	public int getSortKey() {
		return SharedConstants.SHUFFLE_UI_RENDERING_ORDER ? this.hashCode() * (shuffleSeed + 1) : this.hashCode();
	}

	public static void shuffleRenderingOrder() {
		shuffleSeed = Math.round(100000.0F * (float) Math.random());
	}
}
