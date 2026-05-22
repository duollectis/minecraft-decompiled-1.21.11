package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Кольцевой буфер из трёх GPU-буферов с синхронизацией через fence-объекты.
 * Позволяет CPU записывать данные в текущий буфер, пока GPU читает предыдущий,
 * избегая блокировок при потоковой передаче данных (например, UBO для пост-эффектов).
 */
@Environment(EnvType.CLIENT)
public class MappableRingBuffer implements AutoCloseable {

	private static final int BUFFER_COUNT = 3;

	// Флаги доступа: MAP_READ=1, MAP_WRITE=2
	private static final int GL_MAP_READ_BIT = 1;
	private static final int GL_MAP_WRITE_BIT = 2;

	private final GpuBuffer[] buffers = new GpuBuffer[BUFFER_COUNT];
	private final @Nullable GpuFence[] fences = new GpuFence[BUFFER_COUNT];
	private final int size;
	private int current = 0;

	public MappableRingBuffer(Supplier<String> nameSupplier, @GpuBuffer.Usage int usage, int size) {
		if ((usage & GL_MAP_READ_BIT) == 0 && (usage & GL_MAP_WRITE_BIT) == 0) {
			throw new IllegalArgumentException(
				"MappableRingBuffer requires at least one of USAGE_MAP_READ or USAGE_MAP_WRITE"
			);
		}

		GpuDevice gpuDevice = RenderSystem.getDevice();

		for (int index = 0; index < BUFFER_COUNT; index++) {
			int capturedIndex = index;
			buffers[index] = gpuDevice.createBuffer(() -> nameSupplier.get() + " #" + capturedIndex, usage, size);
			fences[index] = null;
		}

		this.size = size;
	}

	public int size() {
		return size;
	}

	/**
	 * Возвращает текущий буфер, блокируя CPU до завершения GPU-операций над ним.
	 * Fence закрывается после ожидания, чтобы не блокировать повторно.
	 */
	public GpuBuffer getBlocking() {
		GpuFence fence = fences[current];

		if (fence != null) {
			fence.awaitCompletion(Long.MAX_VALUE);
			fence.close();
			fences[current] = null;
		}

		return buffers[current];
	}

	/**
	 * Переключает кольцевой буфер на следующий слот.
	 * Создаёт fence для текущего слота, чтобы следующий вызов {@link #getBlocking()} мог дождаться GPU.
	 */
	public void rotate() {
		if (fences[current] != null) {
			fences[current].close();
		}

		fences[current] = RenderSystem.getDevice().createCommandEncoder().createFence();
		current = (current + 1) % BUFFER_COUNT;
	}

	@Override
	public void close() {
		for (int index = 0; index < BUFFER_COUNT; index++) {
			buffers[index].close();

			if (fences[index] != null) {
				fences[index].close();
			}
		}
	}
}
