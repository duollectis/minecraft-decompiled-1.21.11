package net.minecraft.client.render;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code RenderSetup}.
 */
public final class RenderSetup {

	final RenderPipeline pipeline;
	final Map<String, RenderSetup.TextureSpec> textures;
	final TextureTransform textureTransform;
	final OutputTarget outputTarget;
	final RenderSetup.OutlineMode outlineMode;
	final boolean useLightmap;
	final boolean useOverlay;
	final boolean hasCrumbling;
	final boolean translucent;
	final int expectedBufferSize;
	final LayeringTransform layeringTransform;

	RenderSetup(
			RenderPipeline pipeline,
			Map<String, RenderSetup.TextureSpec> textures,
			boolean useLightmap,
			boolean useOverlay,
			LayeringTransform layeringTransform,
			OutputTarget outputTarget,
			TextureTransform textureTransform,
			RenderSetup.OutlineMode outlineMode,
			boolean hasCrumbling,
			boolean translucent,
			int expectedBufferSize
	) {
		this.pipeline = pipeline;
		this.textures = textures;
		this.outputTarget = outputTarget;
		this.textureTransform = textureTransform;
		this.useLightmap = useLightmap;
		this.useOverlay = useOverlay;
		this.outlineMode = outlineMode;
		this.layeringTransform = layeringTransform;
		this.hasCrumbling = hasCrumbling;
		this.translucent = translucent;
		this.expectedBufferSize = expectedBufferSize;
	}

	@Override
	public String toString() {
		return "RenderSetup[layeringTransform="
				+ this.layeringTransform
				+ ", textureTransform="
				+ this.textureTransform
				+ ", textures="
				+ this.textures
				+ ", outlineProperty="
				+ this.outlineMode
				+ ", useLightmap="
				+ this.useLightmap
				+ ", useOverlay="
				+ this.useOverlay
				+ "]";
	}

	public static RenderSetup.Builder builder(RenderPipeline renderPipeline) {
		return new RenderSetup.Builder(renderPipeline);
	}

	public Map<String, RenderSetup.Texture> resolveTextures() {
		if (this.textures.isEmpty() && !this.useOverlay && !this.useLightmap) {
			return Collections.emptyMap();
		}
		else {
			Map<String, RenderSetup.Texture> map = new HashMap<>();
			if (this.useOverlay) {
				map.put(
						"Sampler1",
						new RenderSetup.Texture(
								MinecraftClient.getInstance().gameRenderer.getOverlayTexture().getTextureView(),
								RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
						)
				);
			}

			if (this.useLightmap) {
				map.put(
						"Sampler2",
						new RenderSetup.Texture(
								MinecraftClient.getInstance().gameRenderer
										.getLightmapTextureManager()
										.getGlTextureView(),
								RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
						)
				);
			}

			TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();

			for (Entry<String, RenderSetup.TextureSpec> entry : this.textures.entrySet()) {
				AbstractTexture abstractTexture = textureManager.getTexture(entry.getValue().location);
				GpuSampler gpuSampler = entry.getValue().sampler().get();
				map.put(
						entry.getKey(),
						new RenderSetup.Texture(
								abstractTexture.getGlTextureView(),
								gpuSampler != null ? gpuSampler : abstractTexture.getSampler()
						)
				);
			}

			return map;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private final RenderPipeline pipeline;
		private boolean useLightmap = false;
		private boolean useOverlay = false;
		private LayeringTransform layeringTransform = LayeringTransform.NO_LAYERING;
		private OutputTarget outputTarget = OutputTarget.MAIN_TARGET;
		private TextureTransform textureTransform = TextureTransform.DEFAULT_TEXTURING;
		private boolean hasCrumbling = false;
		private boolean translucent = false;
		private int expectedBufferSize = 1536;
		private RenderSetup.OutlineMode outlineMode = RenderSetup.OutlineMode.NONE;
		private final Map<String, RenderSetup.TextureSpec> textures = new HashMap<>();

		Builder(RenderPipeline pipeline) {
			this.pipeline = pipeline;
		}

		public RenderSetup.Builder texture(String name, Identifier id) {
			this.textures.put(name, new RenderSetup.TextureSpec(id, () -> null));
			return this;
		}

		public RenderSetup.Builder texture(String name, Identifier id, @Nullable Supplier<GpuSampler> samplerSupplier) {
			this.textures.put(
					name,
					new RenderSetup.TextureSpec(
							id,
							Suppliers.memoize(() -> samplerSupplier == null ? null : samplerSupplier.get())
					)
			);
			return this;
		}

		public RenderSetup.Builder useLightmap() {
			this.useLightmap = true;
			return this;
		}

		public RenderSetup.Builder useOverlay() {
			this.useOverlay = true;
			return this;
		}

		public RenderSetup.Builder crumbling() {
			this.hasCrumbling = true;
			return this;
		}

		public RenderSetup.Builder translucent() {
			this.translucent = true;
			return this;
		}

		public RenderSetup.Builder expectedBufferSize(int expectedBufferSize) {
			this.expectedBufferSize = expectedBufferSize;
			return this;
		}

		public RenderSetup.Builder layeringTransform(LayeringTransform layeringTransform) {
			this.layeringTransform = layeringTransform;
			return this;
		}

		public RenderSetup.Builder outputTarget(OutputTarget outputTarget) {
			this.outputTarget = outputTarget;
			return this;
		}

		public RenderSetup.Builder textureTransform(TextureTransform textureTransform) {
			this.textureTransform = textureTransform;
			return this;
		}

		public RenderSetup.Builder outlineMode(RenderSetup.OutlineMode outlineMode) {
			this.outlineMode = outlineMode;
			return this;
		}

		public RenderSetup build() {
			return new RenderSetup(
					this.pipeline,
					this.textures,
					this.useLightmap,
					this.useOverlay,
					this.layeringTransform,
					this.outputTarget,
					this.textureTransform,
					this.outlineMode,
					this.hasCrumbling,
					this.translucent,
					this.expectedBufferSize
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code OutlineMode}.
	 */
	public static enum OutlineMode {
		NONE("none"),
		IS_OUTLINE("is_outline"),
		AFFECTS_OUTLINE("affects_outline");

		private final String name;

		private OutlineMode(final String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Texture}.
	 */
	public record Texture(GpuTextureView textureView, GpuSampler sampler) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TextureSpec}.
	 */
	record TextureSpec(Identifier location, Supplier<@Nullable GpuSampler> sampler) {
	}
}
