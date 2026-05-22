package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Абстрактный менеджер OpenGL-буферов и фреймбуферов.
 * Предоставляет два варианта реализации: через ARB Direct State Access (без bind/unbind)
 * и через стандартный bind-based API (для совместимости).
 */
@Environment(EnvType.CLIENT)
public abstract class BufferManager {

	/**
	 * Создаёт оптимальную реализацию менеджера буферов в зависимости от доступных расширений GPU.
	 * Предпочитает ARB_direct_state_access для минимизации смены состояний OpenGL.
	 *
	 * @param capabilities    доступные возможности OpenGL
	 * @param usedCapabilities множество, в которое добавляется имя использованного расширения
	 * @param deviceInfo      информация об устройстве (для проверки workaround-флагов)
	 * @return экземпляр {@link ARBBufferManager} или {@link DefaultBufferManager}
	 */
	public static BufferManager create(
		GLCapabilities capabilities,
		Set<String> usedCapabilities,
		GpuDeviceInfo deviceInfo
	) {
		if (capabilities.GL_ARB_direct_state_access
			&& GlBackend.allowGlArbDirectAccess
			&& !deviceInfo.shouldDisableArbDirectAccess()
		) {
			usedCapabilities.add("GL_ARB_direct_state_access");
			return new ARBBufferManager();
		}

		return new DefaultBufferManager();
	}

	abstract int createBuffer();

	abstract void setBufferData(int buffer, long size, @GpuBuffer.Usage int usage);

	abstract void setBufferData(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage);

	abstract void setBufferSubData(int buffer, long offset, ByteBuffer data, @GpuBuffer.Usage int usage);

	abstract void setBufferStorage(int buffer, long size, @GpuBuffer.Usage int usage);

	abstract void setBufferStorage(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage);

	abstract @Nullable ByteBuffer mapBufferRange(
		int buffer,
		long offset,
		long length,
		int access,
		@GpuBuffer.Usage int usage
	);

	abstract void unmapBuffer(int buffer, @GpuBuffer.Usage int usage);

	public abstract int createFramebuffer();

	public abstract void setupFramebuffer(
		int framebuffer,
		int colorAttachment,
		int depthAttachment,
		int mipLevel,
		int bindTarget
	);

	abstract void setupBlitFramebuffer(
		int readFramebuffer,
		int writeFramebuffer,
		int srcX0,
		int srcY0,
		int srcX1,
		int srcY1,
		int dstX0,
		int dstY0,
		int dstX1,
		int dstY1,
		int mask,
		int filter
	);

	abstract void flushMappedBufferRange(int buffer, long offset, long length, @GpuBuffer.Usage int usage);

	abstract void copyBufferSubData(int fromBuffer, int toBuffer, long readOffset, long writeOffset, long size);

	// -------------------------------------------------------------------------
	// ARB Direct State Access реализация
	// -------------------------------------------------------------------------

	@Environment(EnvType.CLIENT)
	static class ARBBufferManager extends BufferManager {

		@Override
		int createBuffer() {
			GlStateManager.incrementTrackedBuffers();
			return ARBDirectStateAccess.glCreateBuffers();
		}

		@Override
		void setBufferData(int buffer, long size, @GpuBuffer.Usage int usage) {
			ARBDirectStateAccess.glNamedBufferData(buffer, size, GlConst.bufferUsageToGlEnum(usage));
		}

		@Override
		void setBufferData(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage) {
			ARBDirectStateAccess.glNamedBufferData(buffer, data, GlConst.bufferUsageToGlEnum(usage));
		}

		@Override
		void setBufferSubData(int buffer, long offset, ByteBuffer data, @GpuBuffer.Usage int usage) {
			ARBDirectStateAccess.glNamedBufferSubData(buffer, offset, data);
		}

		@Override
		void setBufferStorage(int buffer, long size, @GpuBuffer.Usage int usage) {
			ARBDirectStateAccess.glNamedBufferStorage(buffer, size, GlConst.bufferUsageToGlFlag(usage));
		}

		@Override
		void setBufferStorage(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage) {
			ARBDirectStateAccess.glNamedBufferStorage(buffer, data, GlConst.bufferUsageToGlFlag(usage));
		}

		@Override
		@Nullable ByteBuffer mapBufferRange(
			int buffer,
			long offset,
			long length,
			int access,
			@GpuBuffer.Usage int usage
		) {
			return ARBDirectStateAccess.glMapNamedBufferRange(buffer, offset, length, access);
		}

		@Override
		void unmapBuffer(int buffer, int usage) {
			ARBDirectStateAccess.glUnmapNamedBuffer(buffer);
		}

		@Override
		public int createFramebuffer() {
			return ARBDirectStateAccess.glCreateFramebuffers();
		}

		@Override
		public void setupFramebuffer(
			int framebuffer,
			int colorAttachment,
			int depthAttachment,
			int mipLevel,
			@GpuBuffer.Usage int bindTarget
		) {
			// GL_COLOR_ATTACHMENT0 = 36064, GL_DEPTH_ATTACHMENT = 36096
			ARBDirectStateAccess.glNamedFramebufferTexture(framebuffer, 36064, colorAttachment, mipLevel);
			ARBDirectStateAccess.glNamedFramebufferTexture(framebuffer, 36096, depthAttachment, mipLevel);
			if (bindTarget != 0) {
				GlStateManager._glBindFramebuffer(bindTarget, framebuffer);
			}
		}

		@Override
		public void setupBlitFramebuffer(
			int readFramebuffer,
			int writeFramebuffer,
			int srcX0,
			int srcY0,
			int srcX1,
			int srcY1,
			int dstX0,
			int dstY0,
			int dstX1,
			int dstY1,
			int mask,
			int filter
		) {
			ARBDirectStateAccess.glBlitNamedFramebuffer(
				readFramebuffer,
				writeFramebuffer,
				srcX0,
				srcY0,
				srcX1,
				srcY1,
				dstX0,
				dstY0,
				dstX1,
				dstY1,
				mask,
				filter
			);
		}

		@Override
		void flushMappedBufferRange(int buffer, long offset, long length, @GpuBuffer.Usage int usage) {
			ARBDirectStateAccess.glFlushMappedNamedBufferRange(buffer, offset, length);
		}

		@Override
		void copyBufferSubData(int fromBuffer, int toBuffer, long readOffset, long writeOffset, long size) {
			ARBDirectStateAccess.glCopyNamedBufferSubData(fromBuffer, toBuffer, readOffset, writeOffset, size);
		}
	}

	// -------------------------------------------------------------------------
	// Стандартная bind-based реализация
	// -------------------------------------------------------------------------

	@Environment(EnvType.CLIENT)
	static class DefaultBufferManager extends BufferManager {

		/**
		 * Определяет GL-таргет буфера по флагам usage.
		 * Биты: 32 = VERTEX (GL_ARRAY_BUFFER), 64 = INDEX (GL_ELEMENT_ARRAY_BUFFER),
		 * 128 = UNIFORM (GL_UNIFORM_BUFFER), иначе GL_COPY_WRITE_BUFFER.
		 */
		private int getTarget(@GpuBuffer.Usage int usage) {
			if ((usage & 32) != 0) {
				return 34962; // GL_ARRAY_BUFFER
			}

			if ((usage & 64) != 0) {
				return 34963; // GL_ELEMENT_ARRAY_BUFFER
			}

			return (usage & 128) != 0 ? 35345 : 36663; // GL_UNIFORM_BUFFER : GL_COPY_WRITE_BUFFER
		}

		@Override
		int createBuffer() {
			return GlStateManager._glGenBuffers();
		}

		@Override
		void setBufferData(int buffer, long size, @GpuBuffer.Usage int usage) {
			int target = getTarget(usage);
			GlStateManager._glBindBuffer(target, buffer);
			GlStateManager._glBufferData(target, size, GlConst.bufferUsageToGlEnum(usage));
			GlStateManager._glBindBuffer(target, 0);
		}

		@Override
		void setBufferData(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage) {
			int target = getTarget(usage);
			GlStateManager._glBindBuffer(target, buffer);
			GlStateManager._glBufferData(target, data, GlConst.bufferUsageToGlEnum(usage));
			GlStateManager._glBindBuffer(target, 0);
		}

		@Override
		void setBufferSubData(int buffer, long offset, ByteBuffer data, @GpuBuffer.Usage int usage) {
			int target = getTarget(usage);
			GlStateManager._glBindBuffer(target, buffer);
			GlStateManager._glBufferSubData(target, offset, data);
			GlStateManager._glBindBuffer(target, 0);
		}

		@Override
		void setBufferStorage(int buffer, long size, @GpuBuffer.Usage int usage) {
			int target = getTarget(usage);
			GlStateManager._glBindBuffer(target, buffer);
			ARBBufferStorage.glBufferStorage(target, size, GlConst.bufferUsageToGlFlag(usage));
			GlStateManager._glBindBuffer(target, 0);
		}

		@Override
		void setBufferStorage(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage) {
			int target = getTarget(usage);
			GlStateManager._glBindBuffer(target, buffer);
			ARBBufferStorage.glBufferStorage(target, data, GlConst.bufferUsageToGlFlag(usage));
			GlStateManager._glBindBuffer(target, 0);
		}

		@Override
		@Nullable ByteBuffer mapBufferRange(
			int buffer,
			long offset,
			long length,
			int access,
			@GpuBuffer.Usage int usage
		) {
			int target = getTarget(usage);
			GlStateManager._glBindBuffer(target, buffer);
			ByteBuffer mapped = GlStateManager._glMapBufferRange(target, offset, length, access);
			GlStateManager._glBindBuffer(target, 0);
			return mapped;
		}

		@Override
		void unmapBuffer(int buffer, @GpuBuffer.Usage int usage) {
			int target = getTarget(usage);
			GlStateManager._glBindBuffer(target, buffer);
			GlStateManager._glUnmapBuffer(target);
			GlStateManager._glBindBuffer(target, 0);
		}

		@Override
		void flushMappedBufferRange(int buffer, long offset, long length, @GpuBuffer.Usage int usage) {
			int target = getTarget(usage);
			GlStateManager._glBindBuffer(target, buffer);
			GL30.glFlushMappedBufferRange(target, offset, length);
			GlStateManager._glBindBuffer(target, 0);
		}

		@Override
		void copyBufferSubData(int fromBuffer, int toBuffer, long readOffset, long writeOffset, long size) {
			// GL_COPY_READ_BUFFER = 36662, GL_COPY_WRITE_BUFFER = 36663
			GlStateManager._glBindBuffer(36662, fromBuffer);
			GlStateManager._glBindBuffer(36663, toBuffer);
			GL31.glCopyBufferSubData(GlConst.GL_COPY_READ_BUFFER, GlConst.GL_COPY_WRITE_BUFFER, readOffset, writeOffset, size);
			GlStateManager._glBindBuffer(36662, 0);
			GlStateManager._glBindBuffer(36663, 0);
		}

		@Override
		public int createFramebuffer() {
			return GlStateManager.glGenFramebuffers();
		}

		@Override
		public void setupFramebuffer(
			int framebuffer,
			int colorAttachment,
			int depthAttachment,
			int mipLevel,
			int bindTarget
		) {
			// Если bindTarget == 0, временно биндим GL_FRAMEBUFFER (36160) и восстанавливаем
			int target = bindTarget == 0 ? 36160 : bindTarget;
			int previousFramebuffer = GlStateManager.getFrameBuffer(target);
			GlStateManager._glBindFramebuffer(target, framebuffer);
			// GL_COLOR_ATTACHMENT0 = 36064, GL_DEPTH_ATTACHMENT = 36096, GL_TEXTURE_2D = 3553
			GlStateManager._glFramebufferTexture2D(target, 36064, 3553, colorAttachment, mipLevel);
			GlStateManager._glFramebufferTexture2D(target, 36096, 3553, depthAttachment, mipLevel);
			if (bindTarget == 0) {
				GlStateManager._glBindFramebuffer(target, previousFramebuffer);
			}
		}

		@Override
		public void setupBlitFramebuffer(
			int readFramebuffer,
			int writeFramebuffer,
			int srcX0,
			int srcY0,
			int srcX1,
			int srcY1,
			int dstX0,
			int dstY0,
			int dstX1,
			int dstY1,
			int mask,
			int filter
		) {
			// GL_READ_FRAMEBUFFER = 36008, GL_DRAW_FRAMEBUFFER = 36009
			int previousRead = GlStateManager.getFrameBuffer(36008);
			int previousDraw = GlStateManager.getFrameBuffer(36009);
			GlStateManager._glBindFramebuffer(36008, readFramebuffer);
			GlStateManager._glBindFramebuffer(36009, writeFramebuffer);
			GlStateManager._glBlitFrameBuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
			GlStateManager._glBindFramebuffer(36008, previousRead);
			GlStateManager._glBindFramebuffer(36009, previousDraw);
		}
	}
}
