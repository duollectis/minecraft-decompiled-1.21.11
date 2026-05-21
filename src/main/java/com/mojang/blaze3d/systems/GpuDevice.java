package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.ShaderSourceGetter;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
/**
 * {@code GpuDevice}.
 */
public interface GpuDevice {

	CommandEncoder createCommandEncoder();

	GpuSampler createSampler(
			AddressMode addressModeU,
			AddressMode addressModeV,
			FilterMode minFilterMode,
			FilterMode magFilterMode,
			int maxAnisotropy,
			OptionalDouble maxLevelOfDetail
	);

	GpuTexture createTexture(
			@Nullable Supplier<String> labelGetter,
			@GpuTexture.Usage int usage,
			TextureFormat format,
			int width,
			int height,
			int depthOrLayers,
			int mipLevels
	);

	GpuTexture createTexture(
			@Nullable String label,
			@GpuTexture.Usage int usage,
			TextureFormat format,
			int width,
			int height,
			int depthOrLayers,
			int mipLevels
	);

	GpuTextureView createTextureView(GpuTexture texture);

	GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels);

	GpuBuffer createBuffer(@Nullable Supplier<String> labelGetter, @GpuBuffer.Usage int usage, long size);

	GpuBuffer createBuffer(@Nullable Supplier<String> labelGetter, @GpuBuffer.Usage int usage, ByteBuffer data);

	String getImplementationInformation();

	List<String> getLastDebugMessages();

	boolean isDebuggingEnabled();

	String getVendor();

	String getBackendName();

	String getVersion();

	String getRenderer();

	int getMaxTextureSize();

	int getUniformOffsetAlignment();

	default CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline) {
		return this.precompilePipeline(pipeline, null);
	}

	CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline, @Nullable ShaderSourceGetter sourceGetter);

	void clearPipelineCache();

	List<String> getEnabledExtensions();

	int getMaxSupportedAnisotropy();

	void close();
}
