package net.minecraft.client.particle;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.LayeredCustomCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.BufferAllocator;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@Environment(EnvType.CLIENT)
/**
 * {@code BillboardParticleSubmittable}.
 */
public class BillboardParticleSubmittable implements OrderedRenderCommandQueue.LayeredCustom, Submittable {

	private static final int INITIAL_BUFFER_MAX_LENGTH = 1024;
	private static final int BUFFER_FLOAT_FIELDS = 12;
	private static final int BUFFER_INT_FIELDS = 2;
	private final Map<BillboardParticle.RenderType, BillboardParticleSubmittable.Vertices>
			bufferByType =
			new HashMap<>();
	private int particles;

	public void render(
			BillboardParticle.RenderType renderType,
			float x,
			float y,
			float z,
			float rotationX,
			float rotationY,
			float rotationZ,
			float rotationW,
			float size,
			float minU,
			float maxU,
			float minV,
			float maxV,
			int color,
			int brightness
	) {
		this.bufferByType
				.computeIfAbsent(renderType, renderTypex -> new BillboardParticleSubmittable.Vertices())
				.vertex(
						x,
						y,
						z,
						rotationX,
						rotationY,
						rotationZ,
						rotationW,
						size,
						minU,
						maxU,
						minV,
						maxV,
						color,
						brightness
				);
		this.particles++;
	}

	@Override
	public void onFrameEnd() {
		this.bufferByType.values().forEach(BillboardParticleSubmittable.Vertices::reset);
		this.particles = 0;
	}

	@Override
	public BillboardParticleSubmittable.@Nullable Buffers submit(LayeredCustomCommandRenderer.VerticesCache cache) {
		int i = this.particles * 4;

		Object var13;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(
				i * VertexFormats.POSITION_TEXTURE_COLOR_LIGHT.getVertexSize())
		) {
			BufferBuilder
					bufferBuilder =
					new BufferBuilder(
							bufferAllocator,
							VertexFormat.DrawMode.QUADS,
							VertexFormats.POSITION_TEXTURE_COLOR_LIGHT
					);
			Map<BillboardParticle.RenderType, BillboardParticleSubmittable.Layer> map = new HashMap<>();
			int j = 0;

			for (Entry<BillboardParticle.RenderType, BillboardParticleSubmittable.Vertices> entry : this.bufferByType.entrySet()) {
				entry.getValue()
				     .render(
						     (x, y, z, rotationX, rotationY, rotationZ, rotationW, size, minU, maxU, minV, maxV, color, brightness) -> this.drawFace(
								     bufferBuilder,
								     x,
								     y,
								     z,
								     rotationX,
								     rotationY,
								     rotationZ,
								     rotationW,
								     size,
								     minU,
								     maxU,
								     minV,
								     maxV,
								     color,
								     brightness
						     )
				     );
				if (entry.getValue().nextVertexIndex() > 0) {
					map.put(
							entry.getKey(),
							new BillboardParticleSubmittable.Layer(j, entry.getValue().nextVertexIndex() * 6)
					);
				}

				j += entry.getValue().nextVertexIndex() * 4;
			}

			BuiltBuffer builtBuffer = bufferBuilder.endNullable();
			if (builtBuffer != null) {
				cache.write(builtBuffer.getBuffer());
				RenderSystem
						.getSequentialBuffer(VertexFormat.DrawMode.QUADS)
						.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
				GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
				                                            .write(
						                                            RenderSystem.getModelViewMatrix(),
						                                            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
						                                            new Vector3f(),
						                                            new Matrix4f()
				                                            );
				return new BillboardParticleSubmittable.Buffers(
						builtBuffer.getDrawParameters().indexCount(),
						gpuBufferSlice,
						map
				);
			}

			var13 = null;
		}

		return (BillboardParticleSubmittable.Buffers) var13;
	}

	@Override
	public void render(
			BillboardParticleSubmittable.Buffers buffers,
			LayeredCustomCommandRenderer.VerticesCache cache,
			RenderPass renderPass,
			TextureManager manager,
			boolean translucent
	) {
		RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
		renderPass.setVertexBuffer(0, cache.get());
		renderPass.setIndexBuffer(shapeIndexBuffer.getIndexBuffer(buffers.indexCount), shapeIndexBuffer.getIndexType());
		renderPass.setUniform("DynamicTransforms", buffers.dynamicTransforms);

		for (Entry<BillboardParticle.RenderType, BillboardParticleSubmittable.Layer> entry : buffers.layers.entrySet()) {
			if (translucent == entry.getKey().translucent()) {
				renderPass.setPipeline(entry.getKey().pipeline());
				AbstractTexture abstractTexture = manager.getTexture(entry.getKey().textureAtlasLocation());
				renderPass.bindTexture("Sampler0", abstractTexture.getGlTextureView(), abstractTexture.getSampler());
				renderPass.drawIndexed(entry.getValue().vertexOffset, 0, entry.getValue().indexCount, 1);
			}
		}
	}

	protected void drawFace(
			VertexConsumer vertexConsumer,
			float x,
			float y,
			float z,
			float rotationX,
			float rotationY,
			float rotationZ,
			float rotationW,
			float size,
			float minU,
			float maxU,
			float minV,
			float maxV,
			int color,
			int brightness
	) {
		Quaternionf quaternionf = new Quaternionf(rotationX, rotationY, rotationZ, rotationW);
		this.renderVertex(vertexConsumer, quaternionf, x, y, z, 1.0F, -1.0F, size, maxU, maxV, color, brightness);
		this.renderVertex(vertexConsumer, quaternionf, x, y, z, 1.0F, 1.0F, size, maxU, minV, color, brightness);
		this.renderVertex(vertexConsumer, quaternionf, x, y, z, -1.0F, 1.0F, size, minU, minV, color, brightness);
		this.renderVertex(vertexConsumer, quaternionf, x, y, z, -1.0F, -1.0F, size, minU, maxV, color, brightness);
	}

	private void renderVertex(
			VertexConsumer vertexConsumer,
			Quaternionf rotation,
			float x,
			float y,
			float z,
			float localX,
			float localY,
			float size,
			float maxU,
			float maxV,
			int color,
			int brightness
	) {
		Vector3f vector3f = new Vector3f(localX, localY, 0.0F).rotate(rotation).mul(size).add(x, y, z);
		vertexConsumer
				.vertex(vector3f.x(), vector3f.y(), vector3f.z())
				.texture(maxU, maxV)
				.color(color)
				.light(brightness);
	}

	@Override
	public void submit(OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState) {
		if (this.particles > 0) {
			orderedRenderCommandQueue.submitCustom(this);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Buffers}.
	 */
	public record Buffers(
			int indexCount,
			GpuBufferSlice dynamicTransforms,
			Map<BillboardParticle.RenderType, BillboardParticleSubmittable.Layer> layers
	) {
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code Consumer}.
	 */
	public interface Consumer {

		void consume(
				float x,
				float y,
				float z,
				float rotationX,
				float rotationY,
				float rotationZ,
				float rotationW,
				float size,
				float minU,
				float maxU,
				float minV,
				float maxV,
				int color,
				int brightness
		);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Layer}.
	 */
	public record Layer(int vertexOffset, int indexCount) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Vertices}.
	 */
	static class Vertices {

		private int maxVertices = 1024;
		private float[] floatData = new float[12288];
		private int[] intData = new int[2048];
		private int nextVertexIndex;

		public void vertex(
				float x,
				float y,
				float z,
				float rotationX,
				float rotationY,
				float rotationZ,
				float rotationW,
				float size,
				float minU,
				float maxU,
				float minV,
				float maxV,
				int color,
				int brightness
		) {
			if (this.nextVertexIndex >= this.maxVertices) {
				this.increaseCapacity();
			}

			int i = this.nextVertexIndex * 12;
			this.floatData[i++] = x;
			this.floatData[i++] = y;
			this.floatData[i++] = z;
			this.floatData[i++] = rotationX;
			this.floatData[i++] = rotationY;
			this.floatData[i++] = rotationZ;
			this.floatData[i++] = rotationW;
			this.floatData[i++] = size;
			this.floatData[i++] = minU;
			this.floatData[i++] = maxU;
			this.floatData[i++] = minV;
			this.floatData[i] = maxV;
			i = this.nextVertexIndex * 2;
			this.intData[i++] = color;
			this.intData[i] = brightness;
			this.nextVertexIndex++;
		}

		/**
		 * Render.
		 *
		 * @param vertexConsumer vertex consumer
		 */
		public void render(BillboardParticleSubmittable.Consumer vertexConsumer) {
			for (int i = 0; i < this.nextVertexIndex; i++) {
				int j = i * 12;
				int k = i * 2;
				vertexConsumer.consume(
						this.floatData[j++],
						this.floatData[j++],
						this.floatData[j++],
						this.floatData[j++],
						this.floatData[j++],
						this.floatData[j++],
						this.floatData[j++],
						this.floatData[j++],
						this.floatData[j++],
						this.floatData[j++],
						this.floatData[j++],
						this.floatData[j],
						this.intData[k++],
						this.intData[k]
				);
			}
		}

		/**
		 * Reset.
		 */
		public void reset() {
			this.nextVertexIndex = 0;
		}

		private void increaseCapacity() {
			this.maxVertices *= 2;
			this.floatData = Arrays.copyOf(this.floatData, this.maxVertices * 12);
			this.intData = Arrays.copyOf(this.intData, this.maxVertices * 2);
		}

		/**
		 * Next vertex index.
		 *
		 * @return int — результат операции
		 */
		public int nextVertexIndex() {
			return this.nextVertexIndex;
		}
	}
}
