package net.minecraft.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;

import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
/**
 * {@code SectionRenderState}.
 */
public record SectionRenderState(
		GpuTextureView textureView,
		EnumMap<BlockRenderLayer, List<RenderPass.RenderObject<GpuBufferSlice[]>>> drawsPerLayer,
		int maxIndicesRequired,
		GpuBufferSlice[] chunkSectionInfos
) {

	public void renderSection(BlockRenderLayerGroup group, GpuSampler sampler) {
		RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
		GpuBuffer
				gpuBuffer =
				this.maxIndicesRequired == 0 ? null : shapeIndexBuffer.getIndexBuffer(this.maxIndicesRequired);
		VertexFormat.IndexType indexType = this.maxIndicesRequired == 0 ? null : shapeIndexBuffer.getIndexType();
		BlockRenderLayer[] blockRenderLayers = group.getLayers();
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		boolean bl = SharedConstants.HOTKEYS && minecraftClient.wireFrame;
		Framebuffer framebuffer = group.getFramebuffer();

		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         () -> "Section layers for " + group.getName(),
				                                         framebuffer.getColorAttachmentView(),
				                                         OptionalInt.empty(),
				                                         framebuffer.getDepthAttachmentView(),
				                                         OptionalDouble.empty()
		                                         )
		) {
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.bindTexture(
					"Sampler2",
					minecraftClient.gameRenderer.getLightmapTextureManager().getGlTextureView(),
					RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
			);

			for (BlockRenderLayer blockRenderLayer : blockRenderLayers) {
				List<RenderPass.RenderObject<GpuBufferSlice[]>> list = this.drawsPerLayer.get(blockRenderLayer);
				if (!list.isEmpty()) {
					if (blockRenderLayer == BlockRenderLayer.TRANSLUCENT) {
						list = list.reversed();
					}

					renderPass.setPipeline(bl ? RenderPipelines.WIREFRAME : blockRenderLayer.getPipeline());
					renderPass.bindTexture("Sampler0", this.textureView, sampler);
					renderPass.drawMultipleIndexed(
							list,
							gpuBuffer,
							indexType,
							List.of("ChunkSection"),
							this.chunkSectionInfos
					);
				}
			}
		}
	}
}
