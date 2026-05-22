package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Кольцевой буфер для динамических UBO-данных одного типа {@code T}.
 * Автоматически расширяется при нехватке ёмкости в течение одного кадра.
 * Старые буферы накапливаются в {@code oldBuffers} и освобождаются при вызове {@link #clear()}.
 */
@Environment(EnvType.CLIENT)
public class DynamicUniformStorage<T extends DynamicUniformStorage.Uploadable> implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final List<MappableRingBuffer> oldBuffers = new ArrayList<>();
	private final int blockSize;
	private final String name;
	private MappableRingBuffer buffer;
	private int size;
	private int capacity;
	private @Nullable T lastWrittenValue;

	public DynamicUniformStorage(String name, int blockSize, int capacity) {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		this.blockSize = MathHelper.roundUpToMultiple(blockSize, gpuDevice.getUniformOffsetAlignment());
		this.capacity = MathHelper.smallestEncompassingPowerOfTwo(capacity);
		this.size = 0;
		this.name = name;
		this.buffer = new MappableRingBuffer(
			() -> name + " x" + this.blockSize,
			130,
			this.blockSize * this.capacity
		);
	}

	public void clear() {
		size = 0;
		lastWrittenValue = null;
		buffer.rotate();

		if (oldBuffers.isEmpty()) {
			return;
		}

		for (MappableRingBuffer old : oldBuffers) {
			old.close();
		}

		oldBuffers.clear();
	}

	/**
	 * Записывает одно значение в буфер. Если значение совпадает с последним записанным,
	 * возвращает срез на уже существующие данные без повторной записи (дедупликация).
	 * При переполнении ёмкости буфер удваивается.
	 */
	public GpuBufferSlice write(T value) {
		if (lastWrittenValue != null && lastWrittenValue.equals(value)) {
			return buffer.getBlocking().slice((size - 1) * blockSize, blockSize);
		}

		if (size >= capacity) {
			int newCapacity = capacity * 2;
			LOGGER.info(
				"Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.",
				new Object[]{name, capacity, newCapacity}
			);
			growBuffer(newCapacity);
		}

		int offset = size * blockSize;

		try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice()
			.createCommandEncoder()
			.mapBuffer(buffer.getBlocking().slice(offset, blockSize), false, true)
		) {
			value.write(mappedView.data());
		}

		size++;
		lastWrittenValue = value;
		return buffer.getBlocking().slice(offset, blockSize);
	}

	/**
	 * Записывает массив значений в буфер одной операцией маппинга.
	 * При переполнении ёмкости буфер расширяется до ближайшей степени двойки.
	 */
	public GpuBufferSlice[] writeAll(T[] values) {
		if (values.length == 0) {
			return new GpuBufferSlice[0];
		}

		if (size + values.length > capacity) {
			int newCapacity = MathHelper.smallestEncompassingPowerOfTwo(Math.max(capacity + 1, values.length));
			LOGGER.info(
				"Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.",
				new Object[]{name, capacity, newCapacity}
			);
			growBuffer(newCapacity);
		}

		int baseOffset = size * blockSize;
		GpuBufferSlice[] slices = new GpuBufferSlice[values.length];

		try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice()
			.createCommandEncoder()
			.mapBuffer(buffer.getBlocking().slice(baseOffset, values.length * blockSize), false, true)
		) {
			ByteBuffer data = mappedView.data();

			for (int index = 0; index < values.length; index++) {
				slices[index] = buffer.getBlocking().slice(baseOffset + index * blockSize, blockSize);
				data.position(index * blockSize);
				values[index].write(data);
			}
		}

		size += values.length;
		lastWrittenValue = values[values.length - 1];
		return slices;
	}

	@Override
	public void close() {
		for (MappableRingBuffer old : oldBuffers) {
			old.close();
		}

		buffer.close();
	}

	private void growBuffer(int newCapacity) {
		capacity = newCapacity;
		size = 0;
		lastWrittenValue = null;
		oldBuffers.add(buffer);
		buffer = new MappableRingBuffer(
			() -> name + " x" + blockSize,
			130,
			blockSize * capacity
		);
	}

	/**
	 * Контракт для объектов, которые умеют сериализовать себя в {@link ByteBuffer} по стандарту STD140.
	 */
	@Environment(EnvType.CLIENT)
	public interface Uploadable {

		void write(ByteBuffer buffer);
	}
}
