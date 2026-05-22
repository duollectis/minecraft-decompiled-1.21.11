package net.minecraft.util.crash;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Именованная секция отчёта о сбое: содержит набор пар ключ-значение
 * и фрагмент стека вызовов для локализации места ошибки.
 */
public class CrashReportSection {

	private static final int STACK_FRAME_OFFSET = 3;
	private static final int BLOCK_COORD_MASK = 15;
	private static final int REGION_SHIFT = 9;
	private static final int CHUNK_SHIFT = 5;

	private final String title;
	private final List<Element> elements = Lists.newArrayList();
	private StackTraceElement[] stackTrace = new StackTraceElement[0];

	public CrashReportSection(String title) {
		this.title = title;
	}

	public static String createPositionString(double x, double y, double z) {
		return String.format(Locale.ROOT, "%.2f,%.2f,%.2f", x, y, z);
	}

	public static String createPositionString(HeightLimitView world, double x, double y, double z) {
		return String.format(
				Locale.ROOT,
				"%.2f,%.2f,%.2f - %s",
				x,
				y,
				z,
				createPositionString(world, BlockPos.ofFloored(x, y, z))
		);
	}

	public static String createPositionString(HeightLimitView world, BlockPos pos) {
		return createPositionString(world, pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Формирует строку с координатами блока в трёх масштабах: мировые, секционные и региональные.
	 * Каждый блок оборачивается в try-catch для защиты от вторичных ошибок при формировании отчёта.
	 *
	 * @param world мир для получения границ высоты
	 * @param x     координата X
	 * @param y     координата Y
	 * @param z     координата Z
	 * @return строка с координатами в трёх масштабах
	 */
	public static String createPositionString(HeightLimitView world, int x, int y, int z) {
		StringBuilder builder = new StringBuilder();

		try {
			builder.append(String.format(Locale.ROOT, "World: (%d,%d,%d)", x, y, z));
		}
		catch (Throwable ignored) {
			builder.append("(Error finding world loc)");
		}

		builder.append(", ");

		try {
			int chunkX = ChunkSectionPos.getSectionCoord(x);
			int chunkY = ChunkSectionPos.getSectionCoord(y);
			int chunkZ = ChunkSectionPos.getSectionCoord(z);
			int localX = x & BLOCK_COORD_MASK;
			int localY = y & BLOCK_COORD_MASK;
			int localZ = z & BLOCK_COORD_MASK;
			int chunkStartX = ChunkSectionPos.getBlockCoord(chunkX);
			int worldBottom = world.getBottomY();
			int chunkStartZ = ChunkSectionPos.getBlockCoord(chunkZ);
			int chunkEndX = ChunkSectionPos.getBlockCoord(chunkX + 1) - 1;
			int worldTop = world.getTopYInclusive();
			int chunkEndZ = ChunkSectionPos.getBlockCoord(chunkZ + 1) - 1;
			builder.append(String.format(
					Locale.ROOT,
					"Section: (at %d,%d,%d in %d,%d,%d; chunk contains blocks %d,%d,%d to %d,%d,%d)",
					localX, localY, localZ,
					chunkX, chunkY, chunkZ,
					chunkStartX, worldBottom, chunkStartZ,
					chunkEndX, worldTop, chunkEndZ
			));
		}
		catch (Throwable ignored) {
			builder.append("(Error finding chunk loc)");
		}

		builder.append(", ");

		try {
			int regionX = x >> REGION_SHIFT;
			int regionZ = z >> REGION_SHIFT;
			int regionChunkStartX = regionX << CHUNK_SHIFT;
			int regionChunkStartZ = regionZ << CHUNK_SHIFT;
			int regionChunkEndX = (regionX + 1 << CHUNK_SHIFT) - 1;
			int regionChunkEndZ = (regionZ + 1 << CHUNK_SHIFT) - 1;
			int regionBlockStartX = regionX << REGION_SHIFT;
			int worldBottom = world.getBottomY();
			int regionBlockStartZ = regionZ << REGION_SHIFT;
			int regionBlockEndX = (regionX + 1 << REGION_SHIFT) - 1;
			int worldTop = world.getTopYInclusive();
			int regionBlockEndZ = (regionZ + 1 << REGION_SHIFT) - 1;
			builder.append(String.format(
					Locale.ROOT,
					"Region: (%d,%d; contains chunks %d,%d to %d,%d, blocks %d,%d,%d to %d,%d,%d)",
					regionX, regionZ,
					regionChunkStartX, regionChunkStartZ,
					regionChunkEndX, regionChunkEndZ,
					regionBlockStartX, worldBottom, regionBlockStartZ,
					regionBlockEndX, worldTop, regionBlockEndZ
			));
		}
		catch (Throwable ignored) {
			builder.append("(Error finding world loc)");
		}

		return builder.toString();
	}

	public CrashReportSection add(String name, CrashCallable<String> callable) {
		try {
			add(name, callable.call());
		}
		catch (Throwable throwable) {
			add(name, throwable);
		}

		return this;
	}

	public CrashReportSection add(String name, Object detail) {
		elements.add(new Element(name, detail));
		return this;
	}

	public void add(String name, Throwable throwable) {
		add(name, (Object) throwable);
	}

	/**
	 * Захватывает текущий стек вызовов, пропуская {@code ignoredCallCount} верхних фреймов
	 * плюс служебные фреймы самого метода.
	 *
	 * @param ignoredCallCount количество дополнительных фреймов для пропуска
	 * @return длина захваченного стека
	 */
	public int initStackTrace(int ignoredCallCount) {
		StackTraceElement[] currentTrace = Thread.currentThread().getStackTrace();

		if (currentTrace.length <= 0) {
			return 0;
		}

		stackTrace = new StackTraceElement[currentTrace.length - STACK_FRAME_OFFSET - ignoredCallCount];
		System.arraycopy(currentTrace, STACK_FRAME_OFFSET + ignoredCallCount, stackTrace, 0, stackTrace.length);
		return stackTrace.length;
	}

	/**
	 * Проверяет, совпадает ли верхний фрейм захваченного стека с ожидаемым фреймом из стека причины.
	 * Используется для связывания секций отчёта с конкретными местами в стеке исключения.
	 *
	 * @param prev ожидаемый текущий фрейм
	 * @param next ожидаемый следующий фрейм (или {@code null})
	 * @return {@code true}, если стек совпадает и секция может быть связана
	 */
	public boolean shouldGenerateStackTrace(StackTraceElement prev, StackTraceElement next) {
		if (stackTrace.length == 0 || prev == null) {
			return false;
		}

		StackTraceElement top = stackTrace[0];
		boolean topMatches = top.isNativeMethod() == prev.isNativeMethod()
				&& top.getClassName().equals(prev.getClassName())
				&& top.getFileName().equals(prev.getFileName())
				&& top.getMethodName().equals(prev.getMethodName());

		if (!topMatches) {
			return false;
		}

		if (next != null != stackTrace.length > 1) {
			return false;
		}

		if (next != null && !stackTrace[1].equals(next)) {
			return false;
		}

		stackTrace[0] = prev;
		return true;
	}

	public void trimStackTraceEnd(int callCount) {
		StackTraceElement[] trimmed = new StackTraceElement[stackTrace.length - callCount];
		System.arraycopy(stackTrace, 0, trimmed, 0, trimmed.length);
		stackTrace = trimmed;
	}

	public void addStackTrace(StringBuilder builder) {
		builder.append("-- ").append(title).append(" --\n");
		builder.append("Details:");

		for (Element element : elements) {
			builder.append("\n\t").append(element.getName()).append(": ").append(element.getDetail());
		}

		if (stackTrace != null && stackTrace.length > 0) {
			builder.append("\nStacktrace:");

			for (StackTraceElement frame : stackTrace) {
				builder.append("\n\tat ").append(frame);
			}
		}
	}

	public StackTraceElement[] getStackTrace() {
		return stackTrace;
	}

	public static void addBlockInfo(CrashReportSection element, HeightLimitView world, BlockPos pos, BlockState state) {
		element.add("Block", state::toString);
		addBlockLocation(element, world, pos);
	}

	public static CrashReportSection addBlockLocation(CrashReportSection element, HeightLimitView world, BlockPos pos) {
		return element.add("Block location", () -> createPositionString(world, pos));
	}

	static class Element {

		private final String name;
		private final String detail;

		public Element(String name, @Nullable Object detail) {
			this.name = name;

			if (detail == null) {
				this.detail = "~~NULL~~";
			}
			else if (detail instanceof Throwable throwable) {
				this.detail = "~~ERROR~~ " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
			}
			else {
				this.detail = detail.toString();
			}
		}

		public String getName() {
			return name;
		}

		public String getDetail() {
			return detail;
		}
	}
}
