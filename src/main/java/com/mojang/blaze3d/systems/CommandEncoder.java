package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
/**
 * {@code CommandEncoder}.
 */
public interface CommandEncoder {

	RenderPass createRenderPass(Supplier<String> labelGetter, GpuTextureView colorAttachment, OptionalInt clearColor);

	RenderPass createRenderPass(
			Supplier<String> labelGetter,
			GpuTextureView colorAttachment,
			OptionalInt clearColor,
			@Nullable GpuTextureView depthAttachment,
			OptionalDouble clearDepth
	);

	void clearColorTexture(GpuTexture texture, int color);

	void clearColorAndDepthTextures(GpuTexture colorAttachment, int color, GpuTexture depthAttachment, double depth);

	void clearColorAndDepthTextures(
			GpuTexture colorAttachment,
			int color,
			GpuTexture depthAttachment,
			double depth,
			int scissorX,
			int scissorY,
			int scissorWidth,
			int scissorHeight
	);

	void clearDepthTexture(GpuTexture texture, double depth);

	void writeToBuffer(GpuBufferSlice slice, ByteBuffer source);

	GpuBuffer.MappedView mapBuffer(GpuBuffer buffer, boolean read, boolean write);

	GpuBuffer.MappedView mapBuffer(GpuBufferSlice slice, boolean read, boolean write);

	void copyToBuffer(GpuBufferSlice from, GpuBufferSlice to);

	void writeToTexture(GpuTexture target, NativeImage source);

	void writeToTexture(
			GpuTexture target,
			NativeImage source,
			int mipLevel,
			int depth,
			int offsetX,
			int offsetY,
			int width,
			int height,
			int skipPixels,
			int skipRows
	);

	void writeToTexture(
			GpuTexture target,
			ByteBuffer buf,
			NativeImage.Format format,
			int mipLevel,
			int depth,
			int offsetX,
			int offsetY,
			int width,
			int height
	);

	void copyTextureToBuffer(
			GpuTexture source,
			GpuBuffer target,
			long offset,
			Runnable dataUploadedCallback,
			int mipLevel
	);

	void copyTextureToBuffer(
			GpuTexture source,
			GpuBuffer target,
			long offset,
			Runnable dataUploadedCallback,
			int mipLevel,
			int intoX,
			int intoY,
			int width,
			int height
	);

	void copyTextureToTexture(
			GpuTexture source,
			GpuTexture target,
			int mipLevel,
			int intoX,
			int intoY,
			int sourceX,
			int sourceY,
			int width,
			int height
	);

	void presentTexture(GpuTextureView texture);

	GpuFence createFence();

	GpuQuery timerQueryBegin();

	void timerQueryEnd(GpuQuery gpuQuery);
}
