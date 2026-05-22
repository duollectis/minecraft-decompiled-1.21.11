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

/**
 * Реализует накопление и отправку billboard-частиц на GPU.
 * Буферизует данные всех частиц по типу рендера, затем за один проход
 * строит единый {@link BuiltBuffer} и отправляет его через {@link OrderedRenderCommandQueue}.
 */
@Environment(EnvType.CLIENT)
public class BillboardParticleSubmittable implements OrderedRenderCommandQueue.LayeredCustom, Submittable {

	private static final int INITIAL_BUFFER_MAX_LENGTH = 1024;
	private static final int BUFFER_FLOAT_FIELDS = 12;
	private static final int BUFFER_INT_FIELDS = 2;

	private final Map<BillboardParticle.RenderType, BillboardParticleSubmittable.Vertices> bufferByType = new HashMap<>();
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
		bufferByType
				.computeIfAbsent(renderType, key -> new BillboardParticleSubmittable.Vertices())
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
		particles++;
	}

	@Override
	public void onFrameEnd() {
		bufferByType.values().forEach(BillboardParticleSubmittable.Vertices::reset);
		particles = 0;
	}

	/**
	 * Собирает все накопленные вершины в единый GPU-буфер и возвращает дескриптор слоёв.
	 * Возвращает {@code null}, если частиц нет (буфер пуст).
	 */
	@Override
	public BillboardParticleSubmittable.@Nullable Buffers submit(LayeredCustomCommandRenderer.VerticesCache cache) {
		int vertexCount = particles * 4;

		try (BufferAllocator allocator = BufferAllocator.fixedSized(
				vertexCount * VertexFormats.POSITION_TEXTURE_COLOR_LIGHT.getVertexSize())
		) {
			BufferBuilder bufferBuilder = new BufferBuilder(
					allocator,
					VertexFormat.DrawMode.QUADS,
					VertexFormats.POSITION_TEXTURE_COLOR_LIGHT
			);
			Map<BillboardParticle.RenderType, BillboardParticleSubmittable.Layer> layers = new HashMap<>();
			int vertexOffset = 0;

			for (Entry<BillboardParticle.RenderType, BillboardParticleSubmittable.Vertices> entry : bufferByType.entrySet()) {
				entry.getValue()
						.render(
								(x, y, z, rotationX, rotationY, rotationZ, rotationW, size, minU, maxU, minV, maxV, color, brightness)
										-> drawFace(
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
					layers.put(
							entry.getKey(),
							new BillboardParticleSubmittable.Layer(vertexOffset, entry.getValue().nextVertexIndex() * 6)
					);
				}

				vertexOffset += entry.getValue().nextVertexIndex() * 4;
			}

			BuiltBuffer builtBuffer = bufferBuilder.endNullable();

			if (builtBuffer == null) {
				return null;
			}

			cache.write(builtBuffer.getBuffer());
			RenderSystem
					.getSequentialBuffer(VertexFormat.DrawMode.QUADS)
					.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
			GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
					.write(
							RenderSystem.getModelViewMatrix(),
							new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
							new Vector3f(),
							new Matrix4f()
					);

			return new BillboardParticleSubmittable.Buffers(
					builtBuffer.getDrawParameters().indexCount(),
					dynamicTransforms,
					layers
			);
		}
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
				AbstractTexture texture = manager.getTexture(entry.getKey().textureAtlasLocation());
				renderPass.bindTexture("Sampler0", texture.getGlTextureView(), texture.getSampler());
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
		Quaternionf rotation = new Quaternionf(rotationX, rotationY, rotationZ, rotationW);
		renderVertex(vertexConsumer, rotation, x, y, z, 1.0F, -1.0F, size, maxU, maxV, color, brightness);
		renderVertex(vertexConsumer, rotation, x, y, z, 1.0F, 1.0F, size, maxU, minV, color, brightness);
		renderVertex(vertexConsumer, rotation, x, y, z, -1.0F, 1.0F, size, minU, minV, color, brightness);
		renderVertex(vertexConsumer, rotation, x, y, z, -1.0F, -1.0F, size, minU, maxV, color, brightness);
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
			float u,
			float v,
			int color,
			int brightness
	) {
		Vector3f pos = new Vector3f(localX, localY, 0.0F).rotate(rotation).mul(size).add(x, y, z);
		vertexConsumer
				.vertex(pos.x(), pos.y(), pos.z())
				.texture(u, v)
				.color(color)
				.light(brightness);
	}

	@Override
	public void submit(OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState) {
		if (particles > 0) {
			queue.submitCustom(this);
		}
	}

	/**
	 * Дескриптор готового GPU-буфера: количество индексов, uniform-данные камеры и карта слоёв по типу рендера.
	 */
	@Environment(EnvType.CLIENT)
	public record Buffers(
			int indexCount,
			GpuBufferSlice dynamicTransforms,
			Map<BillboardParticle.RenderType, BillboardParticleSubmittable.Layer> layers
	) {
	}

	/**
	 * Функциональный интерфейс для потребления данных одной billboard-вершины.
	 */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
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

	/** Смещение и количество индексов одного слоя в общем GPU-буфере. */
	@Environment(EnvType.CLIENT)
	public record Layer(int vertexOffset, int indexCount) {
	}

	/**
	 * Внутренний буфер вершин одного типа рендера.
	 * Хранит данные в двух параллельных массивах (float и int) и удваивает ёмкость при переполнении.
	 */
	@Environment(EnvType.CLIENT)
	static class Vertices {

		private int maxVertices = INITIAL_BUFFER_MAX_LENGTH;
		private float[] floatData = new float[INITIAL_BUFFER_MAX_LENGTH * BUFFER_FLOAT_FIELDS];
		private int[] intData = new int[INITIAL_BUFFER_MAX_LENGTH * BUFFER_INT_FIELDS];
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
			if (nextVertexIndex >= maxVertices) {
				increaseCapacity();
			}

			int floatIndex = nextVertexIndex * BUFFER_FLOAT_FIELDS;
			floatData[floatIndex++] = x;
			floatData[floatIndex++] = y;
			floatData[floatIndex++] = z;
			floatData[floatIndex++] = rotationX;
			floatData[floatIndex++] = rotationY;
			floatData[floatIndex++] = rotationZ;
			floatData[floatIndex++] = rotationW;
			floatData[floatIndex++] = size;
			floatData[floatIndex++] = minU;
			floatData[floatIndex++] = maxU;
			floatData[floatIndex++] = minV;
			floatData[floatIndex] = maxV;

			int intIndex = nextVertexIndex * BUFFER_INT_FIELDS;
			intData[intIndex++] = color;
			intData[intIndex] = brightness;

			nextVertexIndex++;
		}

		public void render(BillboardParticleSubmittable.Consumer consumer) {
			for (int i = 0; i < nextVertexIndex; i++) {
				int floatIndex = i * BUFFER_FLOAT_FIELDS;
				int intIndex = i * BUFFER_INT_FIELDS;
				consumer.consume(
						floatData[floatIndex++],
						floatData[floatIndex++],
						floatData[floatIndex++],
						floatData[floatIndex++],
						floatData[floatIndex++],
						floatData[floatIndex++],
						floatData[floatIndex++],
						floatData[floatIndex++],
						floatData[floatIndex++],
						floatData[floatIndex++],
						floatData[floatIndex++],
						floatData[floatIndex],
						intData[intIndex++],
						intData[intIndex]
				);
			}
		}

		public void reset() {
			nextVertexIndex = 0;
		}

		public int nextVertexIndex() {
			return nextVertexIndex;
		}

		private void increaseCapacity() {
			maxVertices *= 2;
			floatData = Arrays.copyOf(floatData, maxVertices * BUFFER_FLOAT_FIELDS);
			intData = Arrays.copyOf(intData, maxVertices * BUFFER_INT_FIELDS);
		}
	}
}
