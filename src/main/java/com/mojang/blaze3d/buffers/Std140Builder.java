package com.mojang.blaze3d.buffers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;
import net.minecraft.util.math.MathHelper;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

/**
 * Построитель данных в формате std140 — стандартном layout'е для uniform-блоков OpenGL/Vulkan.
 * Автоматически выравнивает каждое поле по требованиям спецификации std140 перед записью.
 * Используется для заполнения {@link GpuBuffer} данными uniform-переменных.
 *
 * <p>Типичный сценарий использования:
 * <pre>{@code
 *   Std140Builder.onStack(stack, size)
 *       .putFloat(value)
 *       .putVec4(x, y, z, w)
 *       .get();
 * }</pre>
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class Std140Builder {

	private static final int ALIGN_FLOAT = 4;
	private static final int ALIGN_VEC2 = 8;
	private static final int ALIGN_VEC3_VEC4 = 16;
	private static final int VEC2_BYTE_SIZE = 8;
	private static final int VEC3_PADDING = 4;
	private static final int VEC3_TOTAL_SIZE = 16;
	private static final int VEC4_BYTE_SIZE = 16;
	private static final int MAT4_BYTE_SIZE = 64;

	private final ByteBuffer buffer;
	private final int start;

	private Std140Builder(ByteBuffer buffer) {
		this.buffer = buffer;
		start = buffer.position();
	}

	/** Создаёт построитель, записывающий данные в существующий {@link ByteBuffer}. */
	public static Std140Builder intoBuffer(ByteBuffer buffer) {
		return new Std140Builder(buffer);
	}

	/**
	 * Создаёт построитель, выделяя память на стеке LWJGL.
	 *
	 * @param stack стек памяти LWJGL
	 * @param size  требуемый размер в байтах
	 */
	public static Std140Builder onStack(MemoryStack stack, int size) {
		return new Std140Builder(stack.malloc(size));
	}

	/**
	 * Завершает построение и возвращает заполненный буфер.
	 * Вызывает {@link ByteBuffer#flip()}, переводя буфер в режим чтения.
	 */
	public ByteBuffer get() {
		return buffer.flip();
	}

	/**
	 * Выравнивает текущую позицию буфера до кратного {@code alignedSize} значения
	 * относительно начала построителя.
	 */
	public Std140Builder align(int alignedSize) {
		int currentPos = buffer.position();
		buffer.position(start + MathHelper.roundUpToMultiple(currentPos - start, alignedSize));
		return this;
	}

	public Std140Builder putFloat(float value) {
		align(ALIGN_FLOAT);
		buffer.putFloat(value);
		return this;
	}

	public Std140Builder putInt(int value) {
		align(ALIGN_FLOAT);
		buffer.putInt(value);
		return this;
	}

	public Std140Builder putVec2(float x, float y) {
		align(ALIGN_VEC2);
		buffer.putFloat(x);
		buffer.putFloat(y);
		return this;
	}

	public Std140Builder putVec2(Vector2fc vec) {
		align(ALIGN_VEC2);
		vec.get(buffer);
		buffer.position(buffer.position() + VEC2_BYTE_SIZE);
		return this;
	}

	public Std140Builder putIVec2(int x, int y) {
		align(ALIGN_VEC2);
		buffer.putInt(x);
		buffer.putInt(y);
		return this;
	}

	public Std140Builder putIVec2(Vector2ic vec) {
		align(ALIGN_VEC2);
		vec.get(buffer);
		buffer.position(buffer.position() + VEC2_BYTE_SIZE);
		return this;
	}

	public Std140Builder putVec3(float x, float y, float z) {
		align(ALIGN_VEC3_VEC4);
		buffer.putFloat(x);
		buffer.putFloat(y);
		buffer.putFloat(z);
		// std140: vec3 занимает 16 байт — добавляем 4 байта padding
		buffer.position(buffer.position() + VEC3_PADDING);
		return this;
	}

	public Std140Builder putVec3(Vector3fc vec) {
		align(ALIGN_VEC3_VEC4);
		vec.get(buffer);
		buffer.position(buffer.position() + VEC3_TOTAL_SIZE);
		return this;
	}

	public Std140Builder putIVec3(int x, int y, int z) {
		align(ALIGN_VEC3_VEC4);
		buffer.putInt(x);
		buffer.putInt(y);
		buffer.putInt(z);
		buffer.position(buffer.position() + VEC3_PADDING);
		return this;
	}

	public Std140Builder putIVec3(Vector3ic vec) {
		align(ALIGN_VEC3_VEC4);
		vec.get(buffer);
		buffer.position(buffer.position() + VEC3_TOTAL_SIZE);
		return this;
	}

	public Std140Builder putVec4(float x, float y, float z, float w) {
		align(ALIGN_VEC3_VEC4);
		buffer.putFloat(x);
		buffer.putFloat(y);
		buffer.putFloat(z);
		buffer.putFloat(w);
		return this;
	}

	public Std140Builder putVec4(Vector4fc vec) {
		align(ALIGN_VEC3_VEC4);
		vec.get(buffer);
		buffer.position(buffer.position() + VEC4_BYTE_SIZE);
		return this;
	}

	public Std140Builder putIVec4(int x, int y, int z, int w) {
		align(ALIGN_VEC3_VEC4);
		buffer.putInt(x);
		buffer.putInt(y);
		buffer.putInt(z);
		buffer.putInt(w);
		return this;
	}

	public Std140Builder putIVec4(Vector4ic vec) {
		align(ALIGN_VEC3_VEC4);
		vec.get(buffer);
		buffer.position(buffer.position() + VEC4_BYTE_SIZE);
		return this;
	}

	public Std140Builder putMat4f(Matrix4fc matrix) {
		align(ALIGN_VEC3_VEC4);
		matrix.get(buffer);
		buffer.position(buffer.position() + MAT4_BYTE_SIZE);
		return this;
	}
}
