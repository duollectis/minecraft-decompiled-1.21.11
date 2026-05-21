package net.minecraft.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
/**
 * {@code ProjectionMatrix2}.
 */
public class ProjectionMatrix2 implements AutoCloseable {

	private final GpuBuffer buffer;
	private final GpuBufferSlice slice;
	private final float nearZ;
	private final float farZ;
	private final boolean invertY;
	private float width;
	private float height;

	public ProjectionMatrix2(String name, float nearZ, float farZ, boolean invertY) {
		this.nearZ = nearZ;
		this.farZ = farZ;
		this.invertY = invertY;
		GpuDevice gpuDevice = RenderSystem.getDevice();
		this.buffer =
				gpuDevice.createBuffer(
						() -> "Projection matrix UBO " + name,
						136,
						RenderSystem.PROJECTION_MATRIX_UBO_SIZE
				);
		this.slice = this.buffer.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
	}

	public GpuBufferSlice set(float width, float height) {
		if (this.width != width || this.height != height) {
			Matrix4f matrix4f = this.getMatrix(width, height);
			MemoryStack memoryStack = MemoryStack.stackPush();

			try {
				ByteBuffer
						byteBuffer =
						Std140Builder
								.onStack(memoryStack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE)
								.putMat4f(matrix4f)
								.get();
				RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), byteBuffer);
			}
			catch (Throwable var8) {
				if (memoryStack != null) {
					try {
						memoryStack.close();
					}
					catch (Throwable var7) {
						var8.addSuppressed(var7);
					}
				}

				throw var8;
			}

			if (memoryStack != null) {
				memoryStack.close();
			}

			this.width = width;
			this.height = height;
		}

		return this.slice;
	}

	private Matrix4f getMatrix(float width, float height) {
		return new Matrix4f().setOrtho(
				0.0F,
				width,
				this.invertY ? height : 0.0F,
				this.invertY ? 0.0F : height,
				this.nearZ,
				this.farZ
		);
	}

	@Override
	public void close() {
		this.buffer.close();
	}
}
