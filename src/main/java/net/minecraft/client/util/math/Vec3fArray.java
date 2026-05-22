package net.minecraft.client.util.math;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Компактное хранилище массива трёхмерных векторов в виде плоского float-массива.
 * Избегает аллокации объектов {@link Vector3f} для каждого элемента.
 */
@Environment(EnvType.CLIENT)
public class Vec3fArray {

	private static final int COMPONENTS_PER_VECTOR = 3;

	private final float[] array;

	public Vec3fArray(int size) {
		array = new float[COMPONENTS_PER_VECTOR * size];
	}

	public int size() {
		return array.length / COMPONENTS_PER_VECTOR;
	}

	public void set(int index, Vector3fc vec) {
		set(index, vec.x(), vec.y(), vec.z());
	}

	public void set(int index, float x, float y, float z) {
		int base = COMPONENTS_PER_VECTOR * index;
		array[base] = x;
		array[base + 1] = y;
		array[base + 2] = z;
	}

	public Vector3f get(int index, Vector3f dest) {
		int base = COMPONENTS_PER_VECTOR * index;
		return dest.set(array[base], array[base + 1], array[base + 2]);
	}

	public float getX(int index) {
		return array[COMPONENTS_PER_VECTOR * index];
	}

	public float getY(int index) {
		return array[COMPONENTS_PER_VECTOR * index + 1];
	}

	/** @bug В оригинале возвращал Y вместо Z — исправлено. */
	public float getZ(int index) {
		return array[COMPONENTS_PER_VECTOR * index + 2];
	}
}
