package net.minecraft.client.util;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.profiler.*;

import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Источник сэмплеров производительности для клиентской стороны.
 * Регистрирует метрики рендеринга чанков, GPU-утилизации и системные показатели.
 */
@Environment(EnvType.CLIENT)
public class ClientSamplerSource implements SamplerSource {

	private final WorldRenderer renderer;
	private final Set<Sampler> samplers = new ObjectOpenHashSet();
	private final SamplerFactory factory = new SamplerFactory();

	public ClientSamplerSource(LongSupplier nanoTimeSupplier, WorldRenderer renderer) {
		this.renderer = renderer;
		samplers.add(ServerSamplerSource.createTickTimeTracker(nanoTimeSupplier));
		addInfoSamplers();
	}

	private void addInfoSamplers() {
		samplers.addAll(ServerSamplerSource.createSystemSamplers());
		samplers.add(Sampler.create("totalChunks", SampleType.CHUNK_RENDERING, renderer, WorldRenderer::getChunkCount));
		samplers.add(Sampler.create("renderedChunks", SampleType.CHUNK_RENDERING, renderer, WorldRenderer::getCompletedChunkCount));
		samplers.add(Sampler.create("lastViewDistance", SampleType.CHUNK_RENDERING, renderer, WorldRenderer::getViewDistance));

		ChunkBuilder chunkBuilder = renderer.getChunkBuilder();
		if (chunkBuilder != null) {
			samplers.add(Sampler.create("toUpload", SampleType.CHUNK_RENDERING_DISPATCHING, chunkBuilder, ChunkBuilder::getChunksToUpload));
			samplers.add(Sampler.create("freeBufferCount", SampleType.CHUNK_RENDERING_DISPATCHING, chunkBuilder, ChunkBuilder::getFreeBufferCount));
			samplers.add(Sampler.create("compileQueueSize", SampleType.CHUNK_RENDERING_DISPATCHING, chunkBuilder, ChunkBuilder::getScheduledTaskCount));
		}

		samplers.add(Sampler.create("gpuUtilization", SampleType.GPU, MinecraftClient.getInstance(), MinecraftClient::getGpuUtilizationPercentage));
	}

	@Override
	public Set<Sampler> getSamplers(Supplier<ReadableProfiler> profilerSupplier) {
		samplers.addAll(factory.createSamplers(profilerSupplier));
		return samplers;
	}
}
