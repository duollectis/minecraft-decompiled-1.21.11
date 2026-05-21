package net.minecraft.util.profiling.jfr.sample;

import jdk.jfr.consumer.RecordedEvent;

/**
 * {@code ChunkRegionSample}.
 */
public record ChunkRegionSample(String level, String dimension, int x, int z) {

	/**
	 * From event.
	 *
	 * @param event event
	 *
	 * @return ChunkRegionSample — результат операции
	 */
	public static ChunkRegionSample fromEvent(RecordedEvent event) {
		return new ChunkRegionSample(
				event.getString("level"),
				event.getString("dimension"),
				event.getInt("chunkPosX"),
				event.getInt("chunkPosZ")
		);
	}
}
