package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Контекст запекания модели: предоставляет доступ к уже запечённым простым моделям,
 * спрайтам, интернеру вершин и кэшу вычислений для избежания повторного запекания.
 */
@Environment(EnvType.CLIENT)
public interface Baker {

	BakedSimpleModel getModel(Identifier id);

	BlockModelPart getMissingBlockPart();

	ErrorCollectingSpriteGetter getSpriteGetter();

	Baker.VertexInterner getVertexInterner();

	<T> T compute(Baker.ResolvableCacheKey<T> key);

	/**
	 * Ключ кэша для вычислений, зависящих от {@link Baker}.
	 * Позволяет избежать повторного запекания одной и той же модели в рамках одного прохода.
	 */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	interface ResolvableCacheKey<T> {

		T compute(Baker baker);
	}

	/**
	 * Интернер вершин: дедуплицирует {@link Vector3fc} объекты для экономии памяти
	 * при запекании геометрии с повторяющимися позициями вершин.
	 */
	@Environment(EnvType.CLIENT)
	interface VertexInterner {

		default Vector3fc intern(float x, float y, float z) {
			return internVector(new Vector3f(x, y, z));
		}

		Vector3fc internVector(Vector3fc vector);
	}
}
