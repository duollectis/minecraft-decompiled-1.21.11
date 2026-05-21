package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.*;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
/**
 * {@code DynamicUniforms}.
 */
public class DynamicUniforms implements AutoCloseable {

	public static final int
			TRANSFORMS_SIZE =
			new Std140SizeCalculator().putMat4f().putVec4().putVec3().putMat4f().get();
	public static final int
			CHUNK_SECTIONS_SIZE =
			new Std140SizeCalculator().putMat4f().putFloat().putIVec2().putIVec3().get();
	private static final int DEFAULT_CAPACITY = 2;
	private final DynamicUniformStorage<DynamicUniforms.TransformsValue>
			transformsStorage =
			new DynamicUniformStorage<>(
					"Dynamic Transforms UBO", TRANSFORMS_SIZE, 2
			);
	private final DynamicUniformStorage<DynamicUniforms.ChunkSectionsValue>
			chunkSectionsStorage =
			new DynamicUniformStorage<>(
					"Chunk Sections UBO", CHUNK_SECTIONS_SIZE, 2
			);

	/**
	 * Clear.
	 */
	public void clear() {
		this.transformsStorage.clear();
		this.chunkSectionsStorage.clear();
	}

	@Override
	public void close() {
		this.transformsStorage.close();
		this.chunkSectionsStorage.close();
	}

	public GpuBufferSlice write(
			Matrix4fc modelView,
			Vector4fc colorModulator,
			Vector3fc modelOffset,
			Matrix4fc textureMatrix
	) {
		return this.transformsStorage
				.write(
						new DynamicUniforms.TransformsValue(
								new Matrix4f(modelView),
								new Vector4f(colorModulator),
								new Vector3f(modelOffset),
								new Matrix4f(textureMatrix)
						)
				);
	}

	/**
	 * Записывает transforms.
	 *
	 * @param values values
	 *
	 * @return GpuBufferSlice[] — результат операции
	 */
	public GpuBufferSlice[] writeTransforms(DynamicUniforms.TransformsValue... values) {
		return this.transformsStorage.writeAll(values);
	}

	/**
	 * Записывает chunk sections.
	 *
	 * @param values values
	 *
	 * @return GpuBufferSlice[] — результат операции
	 */
	public GpuBufferSlice[] writeChunkSections(DynamicUniforms.ChunkSectionsValue... values) {
		return this.chunkSectionsStorage.writeAll(values);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ChunkSectionsValue}.
	 */
	public record ChunkSectionsValue(
			Matrix4fc modelView,
			int x,
			int y,
			int z,
			float visibility,
			int textureAtlasWidth,
			int textureAtlasHeight
	)
			implements DynamicUniformStorage.Uploadable {

		@Override
		public void write(ByteBuffer buffer) {
			Std140Builder.intoBuffer(buffer)
			             .putMat4f(this.modelView)
			             .putFloat(this.visibility)
			             .putIVec2(this.textureAtlasWidth, this.textureAtlasHeight)
			             .putIVec3(this.x, this.y, this.z);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TransformsValue}.
	 */
	public record TransformsValue(
			Matrix4fc modelView,
			Vector4fc colorModulator,
			Vector3fc modelOffset,
			Matrix4fc textureMatrix
	)
			implements DynamicUniformStorage.Uploadable {

		@Override
		public void write(ByteBuffer buffer) {
			Std140Builder
					.intoBuffer(buffer)
					.putMat4f(this.modelView)
					.putVec4(this.colorModulator)
					.putVec3(this.modelOffset)
					.putMat4f(this.textureMatrix);
		}
	}
}
