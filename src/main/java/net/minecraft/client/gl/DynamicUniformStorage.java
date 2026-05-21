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

@Environment(EnvType.CLIENT)
/**
 * {@code DynamicUniformStorage}.
 */
public class DynamicUniformStorage<T extends DynamicUniformStorage.Uploadable> implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final List<MappableRingBuffer> oldBuffers = new ArrayList<>();
	private final int blockSize;
	private MappableRingBuffer buffer;
	private int size;
	private int capacity;
	private @Nullable T lastWrittenValue;
	private final String name;

	public DynamicUniformStorage(String name, int blockSize, int capacity) {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		this.blockSize = MathHelper.roundUpToMultiple(blockSize, gpuDevice.getUniformOffsetAlignment());
		this.capacity = MathHelper.smallestEncompassingPowerOfTwo(capacity);
		this.size = 0;
		this.buffer = new MappableRingBuffer(() -> name + " x" + this.blockSize, 130, this.blockSize * this.capacity);
		this.name = name;
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.size = 0;
		this.lastWrittenValue = null;
		this.buffer.rotate();
		if (!this.oldBuffers.isEmpty()) {
			for (MappableRingBuffer mappableRingBuffer : this.oldBuffers) {
				mappableRingBuffer.close();
			}

			this.oldBuffers.clear();
		}
	}

	private void growBuffer(int capacity) {
		this.capacity = capacity;
		this.size = 0;
		this.lastWrittenValue = null;
		this.oldBuffers.add(this.buffer);
		this.buffer =
				new MappableRingBuffer(() -> this.name + " x" + this.blockSize, 130, this.blockSize * this.capacity);
	}

	/**
	 * Write.
	 *
	 * @param value value
	 *
	 * @return GpuBufferSlice — результат операции
	 */
	public GpuBufferSlice write(T value) {
		if (this.lastWrittenValue != null && this.lastWrittenValue.equals(value)) {
			return this.buffer.getBlocking().slice((this.size - 1) * this.blockSize, this.blockSize);
		}
		else {
			if (this.size >= this.capacity) {
				int i = this.capacity * 2;
				LOGGER.info(
						"Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.",
						new Object[]{this.name, this.capacity, i}
				);
				this.growBuffer(i);
			}

			int i = this.size * this.blockSize;

			try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice()
			                                                   .createCommandEncoder()
			                                                   .mapBuffer(
					                                                   this.buffer
							                                                   .getBlocking()
							                                                   .slice(i, this.blockSize), false, true
			                                                   )
			) {
				value.write(mappedView.data());
			}

			this.size++;
			this.lastWrittenValue = value;
			return this.buffer.getBlocking().slice(i, this.blockSize);
		}
	}

	/**
	 * Записывает all.
	 *
	 * @param values values
	 *
	 * @return GpuBufferSlice[] — результат операции
	 */
	public GpuBufferSlice[] writeAll(T[] values) {
		if (values.length == 0) {
			return new GpuBufferSlice[0];
		}
		else {
			if (this.size + values.length > this.capacity) {
				int i = MathHelper.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, values.length));
				LOGGER.info(
						"Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.",
						new Object[]{this.name, this.capacity, i}
				);
				this.growBuffer(i);
			}

			int i = this.size * this.blockSize;
			GpuBufferSlice[] gpuBufferSlices = new GpuBufferSlice[values.length];

			try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice()
			                                                   .createCommandEncoder()
			                                                   .mapBuffer(
					                                                   this.buffer
							                                                   .getBlocking()
							                                                   .slice(
									                                                   i,
									                                                   values.length * this.blockSize
							                                                   ),
					                                                   false,
					                                                   true
			                                                   )
			) {
				ByteBuffer byteBuffer = mappedView.data();

				for (int j = 0; j < values.length; j++) {
					T uploadable = values[j];
					gpuBufferSlices[j] = this.buffer.getBlocking().slice(i + j * this.blockSize, this.blockSize);
					byteBuffer.position(j * this.blockSize);
					uploadable.write(byteBuffer);
				}
			}

			this.size += values.length;
			this.lastWrittenValue = values[values.length - 1];
			return gpuBufferSlices;
		}
	}

	@Override
	public void close() {
		for (MappableRingBuffer mappableRingBuffer : this.oldBuffers) {
			mappableRingBuffer.close();
		}

		this.buffer.close();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Uploadable}.
	 */
	public interface Uploadable {

		void write(ByteBuffer buffer);
	}
}
