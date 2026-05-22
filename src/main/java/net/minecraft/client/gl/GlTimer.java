package net.minecraft.client.gl;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.OptionalLong;

/**
 * GPU-таймер для измерения времени выполнения команд на видеокарте.
 * Использует паттерн Singleton через {@link InstanceHolder}.
 * Профилирование начинается через {@link #beginProfile()} и завершается через {@link #endProfile()}.
 */
@Environment(EnvType.CLIENT)
public class GlTimer {

	private @Nullable CommandEncoder encoder;
	private @Nullable GpuQuery query;

	public static GlTimer getInstance() {
		return InstanceHolder.INSTANCE;
	}

	public boolean isRunning() {
		return query != null;
	}

	public void beginProfile() {
		RenderSystem.assertOnRenderThread();

		if (query != null) {
			throw new IllegalStateException("Current profile not ended");
		}

		encoder = RenderSystem.getDevice().createCommandEncoder();
		query = encoder.timerQueryBegin();
	}

	/**
	 * Завершает текущий профиль и возвращает объект запроса для получения результата.
	 * Вызов до {@link #beginProfile()} приводит к {@link IllegalStateException}.
	 */
	public Query endProfile() {
		RenderSystem.assertOnRenderThread();

		if (query == null || encoder == null) {
			throw new IllegalStateException("endProfile called before beginProfile");
		}

		encoder.timerQueryEnd(query);
		Query result = new Query(query);
		query = null;
		encoder = null;

		return result;
	}

	@Environment(EnvType.CLIENT)
	static class InstanceHolder {

		static final GlTimer INSTANCE = new GlTimer();

		private InstanceHolder() {
		}
	}

	/**
	 * Результат GPU-запроса таймера. Хранит состояние: не готов (0), закрыт (-1) или
	 * содержит реальное значение в наносекундах.
	 */
	@Environment(EnvType.CLIENT)
	public static class Query {

		private static final long MISSING = 0L;
		private static final long CLOSED = -1L;

		private final GpuQuery query;
		private long result = MISSING;

		Query(GpuQuery query) {
			this.query = query;
		}

		public void close() {
			RenderSystem.assertOnRenderThread();

			if (result != MISSING) {
				return;
			}

			result = CLOSED;
			query.close();
		}

		public boolean isResultAvailable() {
			RenderSystem.assertOnRenderThread();

			if (result != MISSING) {
				return true;
			}

			OptionalLong queryValue = query.getValue();

			if (queryValue.isPresent()) {
				result = queryValue.getAsLong();
				query.close();
				return true;
			}

			return false;
		}

		public long queryResult() {
			RenderSystem.assertOnRenderThread();

			if (result == MISSING) {
				OptionalLong queryValue = query.getValue();

				if (queryValue.isPresent()) {
					result = queryValue.getAsLong();
					query.close();
				}
			}

			return result;
		}
	}
}
