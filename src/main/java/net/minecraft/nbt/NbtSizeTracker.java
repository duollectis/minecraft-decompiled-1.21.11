package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;

/**
 * Отслеживает объём выделенной памяти и глубину вложенности при десериализации NBT.
 * <p>
 * Защищает от атак типа «бомба NBT»: злонамеренно сконструированных данных,
 * которые при разборе потребляют чрезмерно много памяти или вызывают переполнение стека.
 * При превышении лимитов бросает {@link NbtSizeValidationException}.
 */
public class NbtSizeTracker {

	/** Максимальный размер NBT-данных в сетевом пакете (2 МБ). */
	public static final int PACKET_MAX_SIZE = 2097152;
	/** Максимальный размер NBT-данных при чтении уровня (100 МБ). */
	public static final int LEVEL_MAX_SIZE = 104857600;
	private static final int DEFAULT_MAX_DEPTH = 512;

	private final long maxBytes;
	private long allocatedBytes;
	private final int maxDepth;
	private int depth;

	public NbtSizeTracker(long maxBytes, int maxDepth) {
		this.maxBytes = maxBytes;
		this.maxDepth = maxDepth;
	}

	/**
	 * Создаёт трекер с указанным лимитом байт и глубиной по умолчанию ({@value #DEFAULT_MAX_DEPTH}).
	 *
	 * @param maxBytes максимально допустимый объём данных в байтах
	 * @return новый трекер
	 */
	public static NbtSizeTracker of(long maxBytes) {
		return new NbtSizeTracker(maxBytes, DEFAULT_MAX_DEPTH);
	}

	/**
	 * Создаёт трекер для сетевых пакетов с лимитом {@value #PACKET_MAX_SIZE} байт.
	 *
	 * @return новый трекер для пакетов
	 */
	public static NbtSizeTracker forPacket() {
		return new NbtSizeTracker(PACKET_MAX_SIZE, DEFAULT_MAX_DEPTH);
	}

	/**
	 * Создаёт трекер для чтения данных уровня с лимитом {@value #LEVEL_MAX_SIZE} байт.
	 *
	 * @return новый трекер для уровня
	 */
	public static NbtSizeTracker forLevel() {
		return new NbtSizeTracker(LEVEL_MAX_SIZE, DEFAULT_MAX_DEPTH);
	}

	/**
	 * Создаёт трекер без ограничений по размеру (используется для доверенных данных).
	 *
	 * @return трекер с лимитом {@link Long#MAX_VALUE}
	 */
	public static NbtSizeTracker ofUnlimitedBytes() {
		return new NbtSizeTracker(Long.MAX_VALUE, DEFAULT_MAX_DEPTH);
	}

	/**
	 * Учитывает {@code multiplier * bytes} байт выделенной памяти.
	 * Удобно для массивов: {@code add(4L, arrayLength)} вместо {@code add(4L * arrayLength)}.
	 *
	 * @param multiplier множитель (например, размер одного элемента)
	 * @param bytes      количество элементов
	 */
	public void add(long multiplier, long bytes) {
		add(multiplier * bytes);
	}

	/**
	 * Учитывает {@code bytes} байт выделенной памяти.
	 * Бросает исключение при отрицательном значении или превышении лимита.
	 *
	 * @param bytes количество байт для учёта
	 * @throws NbtSizeValidationException если суммарный объём превысит {@code maxBytes}
	 */
	public void add(long bytes) {
		if (bytes < 0L) {
			throw new IllegalArgumentException("Tried to account NBT tag with negative size: " + bytes);
		}

		if (allocatedBytes + bytes > maxBytes) {
			throw new NbtSizeValidationException(
				"Tried to read NBT tag that was too big; tried to allocate: "
					+ allocatedBytes + " + " + bytes
					+ " bytes where max allowed: " + maxBytes
			);
		}

		allocatedBytes += bytes;
	}

	/**
	 * Увеличивает счётчик глубины вложенности при входе в составной тег.
	 *
	 * @throws NbtSizeValidationException если глубина превысит {@code maxDepth}
	 */
	public void pushStack() {
		if (depth >= maxDepth) {
			throw new NbtSizeValidationException(
				"Tried to read NBT tag with too high complexity, depth > " + maxDepth
			);
		}

		depth++;
	}

	/**
	 * Уменьшает счётчик глубины вложенности при выходе из составного тега.
	 *
	 * @throws NbtSizeValidationException если попытка выйти ниже корневого уровня
	 */
	public void popStack() {
		if (depth <= 0) {
			throw new NbtSizeValidationException("NBT-Accounter tried to pop stack-depth at top-level");
		}

		depth--;
	}

	@VisibleForTesting
	public long getAllocatedBytes() {
		return allocatedBytes;
	}

	@VisibleForTesting
	public int getDepth() {
		return depth;
	}
}
