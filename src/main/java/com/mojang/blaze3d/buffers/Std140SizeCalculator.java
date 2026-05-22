package com.mojang.blaze3d.buffers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;
import net.minecraft.util.math.MathHelper;

/**
 * Калькулятор размера буфера в формате std140.
 * Позволяет вычислить необходимый размер памяти для uniform-блока
 * до его фактического выделения, зеркально повторяя вызовы {@link Std140Builder}.
 *
 * <p>Пример использования:
 * <pre>{@code
 *   int size = new Std140SizeCalculator()
 *       .putFloat()
 *       .putVec4()
 *       .get();
 * }</pre>
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class Std140SizeCalculator {

	private static final int ALIGN_FLOAT = 4;
	private static final int ALIGN_VEC2 = 8;
	private static final int ALIGN_VEC3_VEC4 = 16;
	private static final int SIZE_FLOAT_INT = 4;
	private static final int SIZE_VEC2 = 8;
	private static final int SIZE_VEC3 = 16;
	private static final int SIZE_VEC4 = 16;
	private static final int SIZE_MAT4 = 64;

	private int size;

	/** Возвращает вычисленный размер буфера в байтах. */
	public int get() {
		return size;
	}

	/**
	 * Выравнивает текущий размер до кратного {@code alignedSize} значения.
	 */
	public Std140SizeCalculator align(int alignedSize) {
		size = MathHelper.roundUpToMultiple(size, alignedSize);
		return this;
	}

	public Std140SizeCalculator putFloat() {
		align(ALIGN_FLOAT);
		size += SIZE_FLOAT_INT;
		return this;
	}

	public Std140SizeCalculator putInt() {
		align(ALIGN_FLOAT);
		size += SIZE_FLOAT_INT;
		return this;
	}

	public Std140SizeCalculator putVec2() {
		align(ALIGN_VEC2);
		size += SIZE_VEC2;
		return this;
	}

	public Std140SizeCalculator putIVec2() {
		align(ALIGN_VEC2);
		size += SIZE_VEC2;
		return this;
	}

	public Std140SizeCalculator putVec3() {
		align(ALIGN_VEC3_VEC4);
		size += SIZE_VEC3;
		return this;
	}

	public Std140SizeCalculator putIVec3() {
		align(ALIGN_VEC3_VEC4);
		size += SIZE_VEC3;
		return this;
	}

	public Std140SizeCalculator putVec4() {
		align(ALIGN_VEC3_VEC4);
		size += SIZE_VEC4;
		return this;
	}

	public Std140SizeCalculator putIVec4() {
		align(ALIGN_VEC3_VEC4);
		size += SIZE_VEC4;
		return this;
	}

	public Std140SizeCalculator putMat4f() {
		align(ALIGN_VEC3_VEC4);
		size += SIZE_MAT4;
		return this;
	}
}
