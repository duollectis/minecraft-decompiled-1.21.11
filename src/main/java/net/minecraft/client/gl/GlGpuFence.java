package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Реализация GPU-барьера синхронизации через OpenGL fence sync (GL_SYNC_GPU_COMMANDS_COMPLETE).
 * Используется для ожидания завершения GPU-операций перед чтением результатов на CPU.
 */
@Environment(EnvType.CLIENT)
public class GlGpuFence implements GpuFence {

	// GL_SYNC_GPU_COMMANDS_COMPLETE = 37143
	private static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 37143;
	// GL_TIMEOUT_EXPIRED = 37147, GL_WAIT_FAILED = 37149
	private static final int GL_TIMEOUT_EXPIRED = 37147;
	private static final int GL_WAIT_FAILED = 37149;

	private long handle = GlStateManager._glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

	@Override
	public void close() {
		if (handle == 0L) {
			return;
		}

		GlStateManager._glDeleteSync(handle);
		handle = 0L;
	}

	@Override
	public boolean awaitCompletion(long timeoutNanos) {
		if (handle == 0L) {
			return true;
		}

		int result = GlStateManager._glClientWaitSync(handle, 0, timeoutNanos);

		if (result == GL_TIMEOUT_EXPIRED) {
			return false;
		}

		if (result == GL_WAIT_FAILED) {
			throw new IllegalStateException("Failed to complete GPU fence: " + GlStateManager._getError());
		}

		return true;
	}
}
