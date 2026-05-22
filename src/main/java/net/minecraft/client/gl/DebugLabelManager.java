package net.minecraft.client.gl;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.util.StringHelper;
import org.lwjgl.opengl.EXTDebugLabel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Менеджер отладочных меток OpenGL-объектов.
 * Позволяет назначать читаемые имена буферам, текстурам и шейдерам
 * для отображения в графических отладчиках (RenderDoc, NSight и т.д.).
 * Предоставляет три реализации: KHR_debug, EXT_debug_label и no-op.
 */
@Environment(EnvType.CLIENT)
public abstract class DebugLabelManager {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Создаёт оптимальную реализацию менеджера меток.
	 * Если отладка отключена или расширения недоступны — возвращает no-op реализацию.
	 *
	 * @param capabilities    доступные возможности OpenGL
	 * @param debugEnabled    включён ли режим отладки
	 * @param usedCapabilities множество для регистрации использованного расширения
	 * @return активная реализация менеджера меток
	 */
	public static DebugLabelManager create(
		GLCapabilities capabilities,
		boolean debugEnabled,
		Set<String> usedCapabilities
	) {
		if (debugEnabled) {
			if (capabilities.GL_KHR_debug && GlBackend.allowGlKhrDebug) {
				usedCapabilities.add("GL_KHR_debug");
				return new KHRDebugLabelManager();
			}

			if (capabilities.GL_EXT_debug_label && GlBackend.allowExtDebugLabel) {
				usedCapabilities.add("GL_EXT_debug_label");
				return new EXTDebugLabelManager();
			}

			LOGGER.warn("Debug labels unavailable: neither KHR_debug nor EXT_debug_label are supported");
		}

		return new NoOpDebugLabelManager();
	}

	public void labelGlGpuBuffer(GlGpuBuffer buffer) {
	}

	public void labelGlTexture(GlTexture texture) {
	}

	public void labelCompiledShader(CompiledShader shader) {
	}

	public void labelShaderProgram(ShaderProgram program) {
	}

	public void labelAllocatedBuffer(VertexBufferManager.AllocatedBuffer buffer) {
	}

	public void pushDebugGroup(Supplier<String> labelGetter) {
	}

	public void popDebugGroup() {
	}

	public boolean isUsable() {
		return false;
	}

	// -------------------------------------------------------------------------
	// EXT_debug_label реализация
	// -------------------------------------------------------------------------

	@Environment(EnvType.CLIENT)
	static class EXTDebugLabelManager extends DebugLabelManager {

		// GL_BUFFER_OBJECT_EXT = 37201, GL_TEXTURE = 5890, GL_SHADER_OBJECT_EXT = 35656
		// GL_PROGRAM_OBJECT_EXT = 35648, GL_VERTEX_ARRAY_OBJECT_EXT = 32884
		private static final int MAX_LABEL_LENGTH = 256;

		@Override
		public void labelGlGpuBuffer(GlGpuBuffer buffer) {
			Supplier<String> supplier = buffer.debugLabelSupplier;
			if (supplier == null) {
				return;
			}

			EXTDebugLabel.glLabelObjectEXT(37201, buffer.id, StringHelper.truncate(supplier.get(), MAX_LABEL_LENGTH, true));
		}

		@Override
		public void labelGlTexture(GlTexture texture) {
			EXTDebugLabel.glLabelObjectEXT(5890, texture.glId, StringHelper.truncate(texture.getLabel(), MAX_LABEL_LENGTH, true));
		}

		@Override
		public void labelCompiledShader(CompiledShader shader) {
			EXTDebugLabel.glLabelObjectEXT(
				35656,
				shader.getHandle(),
				StringHelper.truncate(shader.getDebugLabel(), MAX_LABEL_LENGTH, true)
			);
		}

		@Override
		public void labelShaderProgram(ShaderProgram program) {
			EXTDebugLabel.glLabelObjectEXT(
				35648,
				program.getGlRef(),
				StringHelper.truncate(program.getDebugLabel(), MAX_LABEL_LENGTH, true)
			);
		}

		@Override
		public void labelAllocatedBuffer(VertexBufferManager.AllocatedBuffer buffer) {
			EXTDebugLabel.glLabelObjectEXT(
				32884,
				buffer.glId,
				StringHelper.truncate(buffer.vertexFormat.toString(), MAX_LABEL_LENGTH, true)
			);
		}

		@Override
		public boolean isUsable() {
			return true;
		}
	}

	// -------------------------------------------------------------------------
	// KHR_debug реализация (поддерживает группы и динамическую длину метки)
	// -------------------------------------------------------------------------

	@Environment(EnvType.CLIENT)
	static class KHRDebugLabelManager extends DebugLabelManager {

		// GL_MAX_LABEL_LENGTH = 33512
		private final int maxLabelLength = GL11.glGetInteger(33512);

		// GL_BUFFER = 33504, GL_TEXTURE = 5890, GL_SHADER = 33505
		// GL_PROGRAM = 33506, GL_VERTEX_ARRAY = 32884, GL_DEBUG_SOURCE_APPLICATION = 33354

		@Override
		public void labelGlGpuBuffer(GlGpuBuffer buffer) {
			Supplier<String> supplier = buffer.debugLabelSupplier;
			if (supplier == null) {
				return;
			}

			KHRDebug.glObjectLabel(33504, buffer.id, StringHelper.truncate(supplier.get(), maxLabelLength, true));
		}

		@Override
		public void labelGlTexture(GlTexture texture) {
			KHRDebug.glObjectLabel(5890, texture.glId, StringHelper.truncate(texture.getLabel(), maxLabelLength, true));
		}

		@Override
		public void labelCompiledShader(CompiledShader shader) {
			KHRDebug.glObjectLabel(
				33505,
				shader.getHandle(),
				StringHelper.truncate(shader.getDebugLabel(), maxLabelLength, true)
			);
		}

		@Override
		public void labelShaderProgram(ShaderProgram program) {
			KHRDebug.glObjectLabel(
				33506,
				program.getGlRef(),
				StringHelper.truncate(program.getDebugLabel(), maxLabelLength, true)
			);
		}

		@Override
		public void labelAllocatedBuffer(VertexBufferManager.AllocatedBuffer buffer) {
			KHRDebug.glObjectLabel(
				32884,
				buffer.glId,
				StringHelper.truncate(buffer.vertexFormat.toString(), maxLabelLength, true)
			);
		}

		@Override
		public void pushDebugGroup(Supplier<String> labelGetter) {
			KHRDebug.glPushDebugGroup(33354, 0, labelGetter.get());
		}

		@Override
		public void popDebugGroup() {
			KHRDebug.glPopDebugGroup();
		}

		@Override
		public boolean isUsable() {
			return true;
		}
	}

	// -------------------------------------------------------------------------
	// No-op реализация (когда отладка отключена)
	// -------------------------------------------------------------------------

	@Environment(EnvType.CLIENT)
	static class NoOpDebugLabelManager extends DebugLabelManager {
	}
}
