package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
/**
 * {@code Baker}.
 */
public interface Baker {

	BakedSimpleModel getModel(Identifier id);

	BlockModelPart getMissingBlockPart();

	ErrorCollectingSpriteGetter getSpriteGetter();

	Baker.VertexInterner getVertexInterner();

	<T> T compute(Baker.ResolvableCacheKey<T> key);

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code ResolvableCacheKey}.
	 */
	public interface ResolvableCacheKey<T> {

		T compute(Baker baker);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code VertexInterner}.
	 */
	public interface VertexInterner {

		default Vector3fc intern(float f, float g, float h) {
			return this.internVector(new Vector3f(f, g, h));
		}

		Vector3fc internVector(Vector3fc vector3fc);
	}
}
