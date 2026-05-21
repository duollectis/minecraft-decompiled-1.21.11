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
 * {@code RawProjectionMatrix}.
 */
public class RawProjectionMatrix implements AutoCloseable {

	private final GpuBuffer buffer;
	private final GpuBufferSlice slice;

	public RawProjectionMatrix(String name) {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		this.buffer =
				gpuDevice.createBuffer(
						() -> "Projection matrix UBO " + name,
						136,
						RenderSystem.PROJECTION_MATRIX_UBO_SIZE
				);
		this.slice = this.buffer.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
	}

	public GpuBufferSlice set(Matrix4f projectionMatrix) {
		MemoryStack memoryStack = MemoryStack.stackPush();

		try {
			ByteBuffer
					byteBuffer =
					Std140Builder
							.onStack(memoryStack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE)
							.putMat4f(projectionMatrix)
							.get();
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), byteBuffer);
		}
		catch (Throwable var6) {
			if (memoryStack != null) {
				try {
					memoryStack.close();
				}
				catch (Throwable var5) {
					var6.addSuppressed(var5);
				}
			}

			throw var6;
		}

		if (memoryStack != null) {
			memoryStack.close();
		}

		return this.slice;
	}

	@Override
	public void close() {
		this.buffer.close();
	}
}
