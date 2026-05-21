package net.minecraft.util.profiling.jfr;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.profiling.jfr.sample.*;
import net.minecraft.world.chunk.ChunkStatus;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code JfrProfile}.
 */
public record JfrProfile(
		Instant startTime,
		Instant endTime,
		Duration duration,
		@Nullable Duration worldGenDuration,
		List<ClientFpsSample> fps,
		List<ServerTickTimeSample> serverTickTimeSamples,
		List<CpuLoadSample> cpuLoadSamples,
		GcHeapSummarySample.Statistics gcHeapSummaryStatistics,
		ThreadAllocationStatisticsSample.AllocationMap threadAllocationMap,
		NetworkIoStatistics<PacketSample> packetReadStatistics,
		NetworkIoStatistics<PacketSample> packetSentStatistics,
		NetworkIoStatistics<ChunkRegionSample> writtenChunks,
		NetworkIoStatistics<ChunkRegionSample> readChunks,
		FileIoSample.Statistics fileWriteStatistics,
		FileIoSample.Statistics fileReadStatistics,
		List<ChunkGenerationSample> chunkGenerationSamples,
		List<StructureGenerationSample> structureGenerationSamples
) {

	public List<Pair<ChunkStatus, LongRunningSampleStatistics<ChunkGenerationSample>>> getChunkGenerationSampleStatistics() {
		Map<ChunkStatus, List<ChunkGenerationSample>> map = this.chunkGenerationSamples
				.stream()
				.collect(Collectors.groupingBy(ChunkGenerationSample::chunkStatus));
		return map.entrySet()
		          .stream()
		          .flatMap(entry -> LongRunningSampleStatistics.<ChunkGenerationSample>fromSamples(entry.getValue())
		                                                       .map(stats -> Pair.of(entry.getKey(), stats))
		                                                       .stream()
		          )
		          .sorted(
				          Comparator
						          .<Pair<ChunkStatus, LongRunningSampleStatistics<ChunkGenerationSample>>, Duration>comparing(
								          pair -> pair.getSecond().totalDuration()
						          )
						          .reversed()
		          )
		          .toList();
	}

	/**
	 * To json.
	 *
	 * @return String — результат операции
	 */
	public String toJson() {
		return new JfrJsonReport().toString(this);
	}
}
