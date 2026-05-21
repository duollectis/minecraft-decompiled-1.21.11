package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

/**
 * Вычисляет оптимальный размер батча чанков для отправки клиенту.
 * <p>Использует скользящее среднее времени обработки одного чанка (в наносекундах),
 * чтобы динамически подстраивать количество чанков в батче под производительность клиента.
 * Новые замеры зажимаются в диапазон [avg/3, avg*3] для защиты от выбросов.
 */
@Environment(EnvType.CLIENT)
public class ChunkBatchSizeCalculator {

	private static final int MAX_SAMPLE_SIZE = 49;
	private static final double CLAMP_FACTOR = 3.0;
	private static final double TARGET_NANOS_PER_TICK = 7_000_000.0;
	private static final double INITIAL_NANOS_PER_CHUNK = 2_000_000.0;

	private double averageNanosPerChunk = INITIAL_NANOS_PER_CHUNK;
	private int sampleSize = 1;
	private volatile long startTime = Util.getMeasuringTimeNano();

	/**
	 * Фиксирует момент начала отправки батча чанков.
	 */
	public void onStartChunkSend() {
		startTime = Util.getMeasuringTimeNano();
	}

	/**
	 * Обновляет скользящее среднее на основе времени обработки завершённого батча.
	 *
	 * @param batchSize количество чанков в завершённом батче
	 */
	public void onChunkSent(int batchSize) {
		if (batchSize <= 0) {
			return;
		}

		double elapsed = Util.getMeasuringTimeNano() - startTime;
		double nanosPerChunk = elapsed / batchSize;
		double clamped = MathHelper.clamp(
				nanosPerChunk,
				averageNanosPerChunk / CLAMP_FACTOR,
				averageNanosPerChunk * CLAMP_FACTOR
		);
		averageNanosPerChunk = (averageNanosPerChunk * sampleSize + clamped) / (sampleSize + 1);
		sampleSize = Math.min(MAX_SAMPLE_SIZE, sampleSize + 1);
	}

	/**
	 * Возвращает желаемое количество чанков для отправки за один тик.
	 *
	 * @return целевое количество чанков в батче
	 */
	public float getDesiredChunksPerTick() {
		return (float) (TARGET_NANOS_PER_TICK / averageNanosPerChunk);
	}
}
