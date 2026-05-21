package net.minecraft.util.profiling.jfr.sample;

import jdk.jfr.consumer.RecordedEvent;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.chunk.ChunkStatus;

import java.time.Duration;

/**
 * {@code ChunkGenerationSample}.
 */
public record ChunkGenerationSample(
		Duration duration,
		ChunkPos chunkPos,
		ColumnPos centerPos,
		ChunkStatus chunkStatus,
		String worldKey
)
		implements LongRunningSample {

	public static ChunkGenerationSample fromEvent(RecordedEvent event) {
		return new ChunkGenerationSample(
				event.getDuration(),
				new ChunkPos(event.getInt("chunkPosX"), event.getInt("chunkPosX")),
				new ColumnPos(event.getInt("worldPosX"), event.getInt("worldPosZ")),
				ChunkStatus.byId(event.getString("status")),
				event.getString("level")
		);
	}
}
