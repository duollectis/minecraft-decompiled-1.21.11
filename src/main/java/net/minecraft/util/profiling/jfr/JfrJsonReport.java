package net.minecraft.util.profiling.jfr;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.math.Quantiles;
import net.minecraft.util.profiling.jfr.sample.*;
import net.minecraft.world.chunk.ChunkStatus;

import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * {@code JfrJsonReport}.
 */
public class JfrJsonReport {

	private static final String BYTES_PER_SECOND = "bytesPerSecond";
	private static final String COUNT = "count";
	private static final String DURATION_NANOS_TOTAL = "durationNanosTotal";
	private static final String TOTAL_BYTES = "totalBytes";
	private static final String COUNT_PER_SECOND = "countPerSecond";
	final Gson
			gson =
			new GsonBuilder().setPrettyPrinting().setLongSerializationPolicy(LongSerializationPolicy.DEFAULT).create();

	private static void addPacketData(PacketSample packet, JsonObject json) {
		json.addProperty("protocolId", packet.protocolId());
		json.addProperty("packetId", packet.packetId());
	}

	private static void addChunkData(ChunkRegionSample chunk, JsonObject json) {
		json.addProperty("level", chunk.level());
		json.addProperty("dimension", chunk.dimension());
		json.addProperty("x", chunk.x());
		json.addProperty("z", chunk.z());
	}

	public String toString(JfrProfile profile) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("startedEpoch", profile.startTime().toEpochMilli());
		jsonObject.addProperty("endedEpoch", profile.endTime().toEpochMilli());
		jsonObject.addProperty("durationMs", profile.duration().toMillis());
		Duration duration = profile.worldGenDuration();
		if (duration != null) {
			jsonObject.addProperty("worldGenDurationMs", duration.toMillis());
		}

		jsonObject.add("heap", this.collectHeapSection(profile.gcHeapSummaryStatistics()));
		jsonObject.add("cpuPercent", this.collectCpuPercentSection(profile.cpuLoadSamples()));
		jsonObject.add("network", this.collectNetworkSection(profile));
		jsonObject.add("fileIO", this.collectFileIoSection(profile));
		jsonObject.add("fps", this.collectClientFpsSection(profile.fps()));
		jsonObject.add("serverTick", this.collectServerTickSection(profile.serverTickTimeSamples()));
		jsonObject.add("threadAllocation", this.collectThreadAllocationSection(profile.threadAllocationMap()));
		jsonObject.add("chunkGen", this.collectChunkGenSection(profile.getChunkGenerationSampleStatistics()));
		jsonObject.add("structureGen", this.collectStructureGenSection(profile.structureGenerationSamples()));
		return this.gson.toJson(jsonObject);
	}

	private JsonElement collectHeapSection(GcHeapSummarySample.Statistics statistics) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("allocationRateBytesPerSecond", statistics.allocatedBytesPerSecond());
		jsonObject.addProperty("gcCount", statistics.count());
		jsonObject.addProperty("gcOverHeadPercent", statistics.getGcDurationRatio());
		jsonObject.addProperty("gcTotalDurationMs", statistics.gcDuration().toMillis());
		return jsonObject;
	}

	private JsonElement collectStructureGenSection(List<StructureGenerationSample> structureGenerationSamples) {
		JsonObject jsonObject = new JsonObject();
		Optional<LongRunningSampleStatistics<StructureGenerationSample>>
				optional =
				LongRunningSampleStatistics.fromSamples(structureGenerationSamples);
		if (optional.isEmpty()) {
			return jsonObject;
		}
		else {
			LongRunningSampleStatistics<StructureGenerationSample> longRunningSampleStatistics = optional.get();
			JsonArray jsonArray = new JsonArray();
			jsonObject.add("structure", jsonArray);
			structureGenerationSamples.stream()
			                          .collect(Collectors.groupingBy(StructureGenerationSample::structureName))
			                          .forEach(
					                          (name, samples) -> {
						                          Optional<LongRunningSampleStatistics<StructureGenerationSample>>
								                          optionalx =
								                          LongRunningSampleStatistics.fromSamples(
										                          (List<StructureGenerationSample>) samples
								                          );
						                          if (!optionalx.isEmpty()) {
							                          LongRunningSampleStatistics<StructureGenerationSample>
									                          longRunningSampleStatistics2 =
									                          optionalx.get();
							                          JsonObject jsonObject2 = new JsonObject();
							                          jsonArray.add(jsonObject2);
							                          jsonObject2.addProperty("name", name);
							                          jsonObject2.addProperty(
									                          "count",
									                          longRunningSampleStatistics2.count()
							                          );
							                          jsonObject2.addProperty(
									                          "durationNanosTotal",
									                          longRunningSampleStatistics2.totalDuration().toNanos()
							                          );
							                          jsonObject2.addProperty(
									                          "durationNanosAvg",
									                          longRunningSampleStatistics2.totalDuration().toNanos()
											                          / longRunningSampleStatistics2.count()
							                          );
							                          JsonObject
									                          jsonObject3 =
									                          Util.make(
											                          new JsonObject(),
											                          json -> jsonObject2.add(
													                          "durationNanosPercentiles",
													                          json
											                          )
									                          );
							                          longRunningSampleStatistics2
									                          .quantiles()
									                          .forEach((quantile, value) -> jsonObject3.addProperty(
											                          "p" + quantile,
											                          value
									                          ));
							                          Function<StructureGenerationSample, JsonElement>
									                          function =
									                          sample -> {
										                          JsonObject jsonObjectxx = new JsonObject();
										                          jsonObjectxx.addProperty(
												                          "durationNanos",
												                          sample.duration().toNanos()
										                          );
										                          jsonObjectxx.addProperty(
												                          "chunkPosX",
												                          sample.chunkPos().x
										                          );
										                          jsonObjectxx.addProperty(
												                          "chunkPosZ",
												                          sample.chunkPos().z
										                          );
										                          jsonObjectxx.addProperty(
												                          "structureName",
												                          sample.structureName()
										                          );
										                          jsonObjectxx.addProperty("level", sample.level());
										                          jsonObjectxx.addProperty("success", sample.success());
										                          return jsonObjectxx;
									                          };
							                          jsonObject.add(
									                          "fastest",
									                          function.apply(longRunningSampleStatistics.fastestSample())
							                          );
							                          jsonObject.add(
									                          "slowest",
									                          function.apply(longRunningSampleStatistics.slowestSample())
							                          );
							                          jsonObject.add(
									                          "secondSlowest",
									                          (JsonElement) (
											                          longRunningSampleStatistics.secondSlowestSample()
													                          != null
											                          ? function.apply(longRunningSampleStatistics.secondSlowestSample())
											                          : JsonNull.INSTANCE
									                          )
							                          );
						                          }
					                          }
			                          );
			return jsonObject;
		}
	}

	private JsonElement collectChunkGenSection(List<Pair<ChunkStatus, LongRunningSampleStatistics<ChunkGenerationSample>>> statistics) {
		JsonObject jsonObject = new JsonObject();
		if (statistics.isEmpty()) {
			return jsonObject;
		}
		else {
			jsonObject.addProperty(
					"durationNanosTotal",
					statistics
							.stream()
							.mapToDouble(pairx -> ((LongRunningSampleStatistics) pairx.getSecond())
									.totalDuration()
									.toNanos())
							.sum()
			);
			JsonArray jsonArray = Util.make(new JsonArray(), json -> jsonObject.add("status", json));

			for (Pair<ChunkStatus, LongRunningSampleStatistics<ChunkGenerationSample>> pair : statistics) {
				LongRunningSampleStatistics<ChunkGenerationSample>
						longRunningSampleStatistics =
						(LongRunningSampleStatistics<ChunkGenerationSample>) pair.getSecond();
				JsonObject jsonObject2 = Util.make(new JsonObject(), jsonArray::add);
				jsonObject2.addProperty("state", ((ChunkStatus) pair.getFirst()).toString());
				jsonObject2.addProperty("count", longRunningSampleStatistics.count());
				jsonObject2.addProperty("durationNanosTotal", longRunningSampleStatistics.totalDuration().toNanos());
				jsonObject2.addProperty(
						"durationNanosAvg",
						longRunningSampleStatistics.totalDuration().toNanos() / longRunningSampleStatistics.count()
				);
				JsonObject
						jsonObject3 =
						Util.make(new JsonObject(), json -> jsonObject2.add("durationNanosPercentiles", json));
				longRunningSampleStatistics
						.quantiles()
						.forEach((quantile, value) -> jsonObject3.addProperty("p" + quantile, value));
				Function<ChunkGenerationSample, JsonElement> function = sample -> {
					JsonObject jsonObjectx = new JsonObject();
					jsonObjectx.addProperty("durationNanos", sample.duration().toNanos());
					jsonObjectx.addProperty("level", sample.worldKey());
					jsonObjectx.addProperty("chunkPosX", sample.chunkPos().x);
					jsonObjectx.addProperty("chunkPosZ", sample.chunkPos().z);
					jsonObjectx.addProperty("worldPosX", sample.centerPos().x());
					jsonObjectx.addProperty("worldPosZ", sample.centerPos().z());
					return jsonObjectx;
				};
				jsonObject2.add("fastest", function.apply(longRunningSampleStatistics.fastestSample()));
				jsonObject2.add("slowest", function.apply(longRunningSampleStatistics.slowestSample()));
				jsonObject2.add(
						"secondSlowest",
						(JsonElement) (longRunningSampleStatistics.secondSlowestSample() != null
						               ? function.apply(longRunningSampleStatistics.secondSlowestSample())
						               : JsonNull.INSTANCE
						)
				);
			}

			return jsonObject;
		}
	}

	private JsonElement collectThreadAllocationSection(ThreadAllocationStatisticsSample.AllocationMap statistics) {
		JsonArray jsonArray = new JsonArray();
		statistics.allocations().forEach((threadName, allocation) -> jsonArray.add(Util.make(
				new JsonObject(), json -> {
					json.addProperty("thread", threadName);
					json.addProperty("bytesPerSecond", allocation);
				}
		)));
		return jsonArray;
	}

	private JsonElement collectServerTickSection(List<ServerTickTimeSample> samples) {
		if (samples.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		else {
			JsonObject jsonObject = new JsonObject();
			double[]
					ds =
					samples.stream().mapToDouble(sample -> sample.averageTickMs().toNanos() / 1000000.0).toArray();
			DoubleSummaryStatistics doubleSummaryStatistics = DoubleStream.of(ds).summaryStatistics();
			jsonObject.addProperty("minMs", doubleSummaryStatistics.getMin());
			jsonObject.addProperty("averageMs", doubleSummaryStatistics.getAverage());
			jsonObject.addProperty("maxMs", doubleSummaryStatistics.getMax());
			Map<Integer, Double> map = Quantiles.create(ds);
			map.forEach((quantile, value) -> jsonObject.addProperty("p" + quantile, value));
			return jsonObject;
		}
	}

	private JsonElement collectClientFpsSection(List<ClientFpsSample> samples) {
		if (samples.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		else {
			JsonObject jsonObject = new JsonObject();
			int[] is = samples.stream().mapToInt(ClientFpsSample::fps).toArray();
			IntSummaryStatistics intSummaryStatistics = IntStream.of(is).summaryStatistics();
			jsonObject.addProperty("minFPS", intSummaryStatistics.getMin());
			jsonObject.addProperty("averageFPS", intSummaryStatistics.getAverage());
			jsonObject.addProperty("maxFPS", intSummaryStatistics.getMax());
			Map<Integer, Double> map = Quantiles.create(is);
			map.forEach((quantile, value) -> jsonObject.addProperty("p" + quantile, value));
			return jsonObject;
		}
	}

	private JsonElement collectFileIoSection(JfrProfile profile) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.add("write", this.collectFileIoSection(profile.fileWriteStatistics()));
		jsonObject.add("read", this.collectFileIoSection(profile.fileReadStatistics()));
		jsonObject.add("chunksRead", this.collectPacketSection(profile.readChunks(), JfrJsonReport::addChunkData));
		jsonObject.add(
				"chunksWritten",
				this.collectPacketSection(profile.writtenChunks(), JfrJsonReport::addChunkData)
		);
		return jsonObject;
	}

	private JsonElement collectFileIoSection(FileIoSample.Statistics statistics) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("totalBytes", statistics.totalBytes());
		jsonObject.addProperty("count", statistics.count());
		jsonObject.addProperty("bytesPerSecond", statistics.bytesPerSecond());
		jsonObject.addProperty("countPerSecond", statistics.countPerSecond());
		JsonArray jsonArray = new JsonArray();
		jsonObject.add("topContributors", jsonArray);
		statistics.topContributors().forEach(pair -> {
			JsonObject jsonObjectx = new JsonObject();
			jsonArray.add(jsonObjectx);
			jsonObjectx.addProperty("path", (String) pair.getFirst());
			jsonObjectx.addProperty("totalBytes", (Number) pair.getSecond());
		});
		return jsonObject;
	}

	private JsonElement collectNetworkSection(JfrProfile profile) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.add("sent", this.collectPacketSection(profile.packetSentStatistics(), JfrJsonReport::addPacketData));
		jsonObject.add(
				"received",
				this.collectPacketSection(profile.packetReadStatistics(), JfrJsonReport::addPacketData)
		);
		return jsonObject;
	}

	private <T> JsonElement collectPacketSection(
			NetworkIoStatistics<T> statistics,
			BiConsumer<T, JsonObject> callback
	) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("totalBytes", statistics.getTotalSize());
		jsonObject.addProperty("count", statistics.getTotalCount());
		jsonObject.addProperty("bytesPerSecond", statistics.getBytesPerSecond());
		jsonObject.addProperty("countPerSecond", statistics.getCountPerSecond());
		JsonArray jsonArray = new JsonArray();
		jsonObject.add("topContributors", jsonArray);
		statistics.getTopContributors().forEach(topContributor -> {
			JsonObject jsonObjectx = new JsonObject();
			jsonArray.add(jsonObjectx);
			T object = (T) topContributor.getFirst();
			NetworkIoStatistics.PacketStatistics
					packetStatistics =
					(NetworkIoStatistics.PacketStatistics) topContributor.getSecond();
			callback.accept(object, jsonObjectx);
			jsonObjectx.addProperty("totalBytes", packetStatistics.totalSize());
			jsonObjectx.addProperty("count", packetStatistics.totalCount());
			jsonObjectx.addProperty("averageSize", packetStatistics.getAverageSize());
		});
		return jsonObject;
	}

	private JsonElement collectCpuPercentSection(List<CpuLoadSample> samples) {
		JsonObject jsonObject = new JsonObject();
		BiFunction<List<CpuLoadSample>, ToDoubleFunction<CpuLoadSample>, JsonObject>
				biFunction =
				(samplesx, valueGetter) -> {
					JsonObject jsonObjectx = new JsonObject();
					DoubleSummaryStatistics
							doubleSummaryStatistics =
							samplesx.stream().mapToDouble(valueGetter).summaryStatistics();
					jsonObjectx.addProperty("min", doubleSummaryStatistics.getMin());
					jsonObjectx.addProperty("average", doubleSummaryStatistics.getAverage());
					jsonObjectx.addProperty("max", doubleSummaryStatistics.getMax());
					return jsonObjectx;
				};
		jsonObject.add("jvm", (JsonElement) biFunction.apply(samples, CpuLoadSample::jvm));
		jsonObject.add("userJvm", (JsonElement) biFunction.apply(samples, CpuLoadSample::userJvm));
		jsonObject.add("system", (JsonElement) biFunction.apply(samples, CpuLoadSample::system));
		return jsonObject;
	}
}
