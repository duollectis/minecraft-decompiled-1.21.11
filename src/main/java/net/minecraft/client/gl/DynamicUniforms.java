package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.*;

import java.nio.ByteBuffer;

/**
 * Хранилище динамических UBO-данных для двух типов блоков:
 * трансформаций объектов ({@link TransformsValue}) и данных чанк-секций ({@link ChunkSectionsValue}).
 * Управляет двумя независимыми {@link DynamicUniformStorage} с начальной ёмкостью 2.
 */
@Environment(EnvType.CLIENT)
public class DynamicUniforms implements AutoCloseable {

	public static final int TRANSFORMS_SIZE =
		new Std140SizeCalculator().putMat4f().putVec4().putVec3().putMat4f().get();
	public static final int CHUNK_SECTIONS_SIZE =
		new Std140SizeCalculator().putMat4f().putFloat().putIVec2().putIVec3().get();

	private static final int DEFAULT_CAPACITY = 2;

	private final DynamicUniformStorage<TransformsValue> transformsStorage =
		new DynamicUniformStorage<>("Dynamic Transforms UBO", TRANSFORMS_SIZE, DEFAULT_CAPACITY);
	private final DynamicUniformStorage<ChunkSectionsValue> chunkSectionsStorage =
		new DynamicUniformStorage<>("Chunk Sections UBO", CHUNK_SECTIONS_SIZE, DEFAULT_CAPACITY);

	public void clear() {
		transformsStorage.clear();
		chunkSectionsStorage.clear();
	}

	@Override
	public void close() {
		transformsStorage.close();
		chunkSectionsStorage.close();
	}

	public GpuBufferSlice write(
		Matrix4fc modelView,
		Vector4fc colorModulator,
		Vector3fc modelOffset,
		Matrix4fc textureMatrix
	) {
		return transformsStorage.write(
			new TransformsValue(
				new Matrix4f(modelView),
				new Vector4f(colorModulator),
				new Vector3f(modelOffset),
				new Matrix4f(textureMatrix)
			)
		);
	}

	public GpuBufferSlice[] writeTransforms(TransformsValue... values) {
		return transformsStorage.writeAll(values);
	}

	public GpuBufferSlice[] writeChunkSections(ChunkSectionsValue... values) {
		return chunkSectionsStorage.writeAll(values);
	}

	/**
	 * Данные UBO для одной чанк-секции: матрица вида, видимость, размеры атласа и координаты.
	 */
	@Environment(EnvType.CLIENT)
	public record ChunkSectionsValue(
		Matrix4fc modelView,
		int x,
		int y,
		int z,
		float visibility,
		int textureAtlasWidth,
		int textureAtlasHeight
	) implements DynamicUniformStorage.Uploadable {

		@Override
		public void write(ByteBuffer buffer) {
			Std140Builder.intoBuffer(buffer)
				.putMat4f(modelView)
				.putFloat(visibility)
				.putIVec2(textureAtlasWidth, textureAtlasHeight)
				.putIVec3(x, y, z);
		}
	}

	/**
	 * Данные UBO для трансформаций объекта: матрица вида, модулятор цвета, смещение и матрица текстуры.
	 */
	@Environment(EnvType.CLIENT)
	public record TransformsValue(
		Matrix4fc modelView,
		Vector4fc colorModulator,
		Vector3fc modelOffset,
		Matrix4fc textureMatrix
	) implements DynamicUniformStorage.Uploadable {

		@Override
		public void write(ByteBuffer buffer) {
			Std140Builder.intoBuffer(buffer)
				.putMat4f(modelView)
				.putVec4(colorModulator)
				.putVec3(modelOffset)
				.putMat4f(textureMatrix);
		}
	}
}
