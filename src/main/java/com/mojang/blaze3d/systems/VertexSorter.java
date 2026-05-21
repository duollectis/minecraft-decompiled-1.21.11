package com.mojang.blaze3d.systems;

import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.Vec3fArray;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
/**
 * {@code VertexSorter}.
 */
public interface VertexSorter {

	VertexSorter BY_DISTANCE = byDistance(0.0F, 0.0F, 0.0F);

	VertexSorter BY_Z = of(vec -> -vec.z());

	static VertexSorter byDistance(float originX, float originY, float originZ) {
		return byDistance(new Vector3f(originX, originY, originZ));
	}

	static VertexSorter byDistance(Vector3fc origin) {
		return of(origin::distanceSquared);
	}

	static VertexSorter of(VertexSorter.SortKeyMapper mapper) {
		return vectors -> {
			Vector3f vector3f = new Vector3f();
			float[] fs = new float[vectors.size()];
			int[] is = new int[vectors.size()];

			for (int i = 0; i < vectors.size(); is[i] = i++) {
				fs[i] = mapper.apply(vectors.get(i, vector3f));
			}

			IntArrays.mergeSort(is, (a, b) -> Floats.compare(fs[b], fs[a]));
			return is;
		};
	}

	int[] sort(Vec3fArray vectors);

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code SortKeyMapper}.
	 */
	public interface SortKeyMapper {

		float apply(Vector3f vec);
	}
}
