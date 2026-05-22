package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Запись отладочного HUD: использование памяти JVM и скорость аллокации.
 */
@Environment(EnvType.CLIENT)
public class MemoryDebugHudEntry implements DebugHudEntry {

	private static final Identifier SECTION_ID = Identifier.ofVanilla("memory");
	private final AllocationRateCalculator allocationRateCalculator = new AllocationRateCalculator();

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		long maxMemory = Runtime.getRuntime().maxMemory();
		long totalMemory = Runtime.getRuntime().totalMemory();
		long freeMemory = Runtime.getRuntime().freeMemory();
		long usedMemory = totalMemory - freeMemory;

		lines.addLinesToSection(
			SECTION_ID,
			List.of(
				String.format(Locale.ROOT, "Mem: %2d%% %03d/%03dMB", usedMemory * 100L / maxMemory, toMegabytes(usedMemory), toMegabytes(maxMemory)),
				String.format(Locale.ROOT, "Allocation rate: %03dMB/s", toMegabytes(allocationRateCalculator.get(usedMemory))),
				String.format(Locale.ROOT, "Allocated: %2d%% %03dMB", totalMemory * 100L / maxMemory, toMegabytes(totalMemory))
			)
		);
	}

	private static long toMegabytes(long bytes) {
		return bytes / 1024L / 1024L;
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}

	/**
	 * Вычисляет скорость аллокации памяти в байтах/с с интервалом обновления {@value #INTERVAL_MS} мс.
	 * Учитывает сборки мусора: если между замерами прошла GC, результат не обновляется.
	 */
	@Environment(EnvType.CLIENT)
	static class AllocationRateCalculator {

		private static final int INTERVAL_MS = 500;
		private static final List<GarbageCollectorMXBean> GARBAGE_COLLECTORS =
			ManagementFactory.getGarbageCollectorMXBeans();

		private long lastCalculated = 0L;
		private long lastAllocatedBytes = -1L;
		private long lastCollectionCount = -1L;
		private long allocationRate = 0L;

		long get(long allocatedBytes) {
			long now = System.currentTimeMillis();

			if (now - lastCalculated < INTERVAL_MS) {
				return allocationRate;
			}

			long currentCollectionCount = getCollectionCount();
			if (lastCalculated != 0L && currentCollectionCount == lastCollectionCount) {
				double secondsRatio = (double) TimeUnit.SECONDS.toMillis(1L) / (now - lastCalculated);
				long allocatedDelta = allocatedBytes - lastAllocatedBytes;
				allocationRate = Math.round(allocatedDelta * secondsRatio);
			}

			lastCalculated = now;
			lastAllocatedBytes = allocatedBytes;
			lastCollectionCount = currentCollectionCount;
			return allocationRate;
		}

		private static long getCollectionCount() {
			long total = 0L;

			for (GarbageCollectorMXBean gc : GARBAGE_COLLECTORS) {
				total += gc.getCollectionCount();
			}

			return total;
		}
	}
}
