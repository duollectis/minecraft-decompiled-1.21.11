package net.minecraft.client.gl;

import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.GL32C;

import java.util.OptionalLong;

/**
 * Реализация GPU-запроса таймера через OpenGL query objects.
 * Использует {@code GL_QUERY_RESULT_AVAILABLE} для неблокирующей проверки готовности
 * и {@code ARBTimerQuery} для получения 64-битного результата в наносекундах.
 */
@Environment(EnvType.CLIENT)
public class GlTimerQuery implements GpuQuery {

	private static final int GL_QUERY_RESULT_AVAILABLE = 34919;
	private static final int GL_QUERY_RESULT = 34918;

	private final int id;
	private boolean closed;
	private OptionalLong value = OptionalLong.empty();

	GlTimerQuery(int id) {
		this.id = id;
	}

	@Override
	public OptionalLong getValue() {
		RenderSystem.assertOnRenderThread();

		if (closed) {
			throw new IllegalStateException("GlTimerQuery is closed");
		}

		if (value.isPresent()) {
			return value;
		}

		if (GL32C.glGetQueryObjecti(id, GL_QUERY_RESULT_AVAILABLE) != 1) {
			return OptionalLong.empty();
		}

		value = OptionalLong.of(ARBTimerQuery.glGetQueryObjecti64(id, GL_QUERY_RESULT));

		return value;
	}

	@Override
	public void close() {
		RenderSystem.assertOnRenderThread();

		if (closed) {
			return;
		}

		closed = true;
		GL32C.glDeleteQueries(id);
	}
}
