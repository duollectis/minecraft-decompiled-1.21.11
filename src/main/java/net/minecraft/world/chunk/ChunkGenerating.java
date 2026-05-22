package net.minecraft.world.chunk;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.Blender;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

/**
 * Набор статических методов-задач генерации чанка, реализующих каждый шаг
 * пайплайна {@link ChunkGenerationSteps}. Каждый метод соответствует одному
 * {@link ChunkStatus} и принимает единый набор параметров через {@link ChunkGenerationContext}.
 */
public class ChunkGenerating {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static boolean isLightOn(Chunk chunk) {
		return chunk.getStatus().isAtLeast(ChunkStatus.LIGHT) && chunk.isLightOn();
	}

	static CompletableFuture<Chunk> noop(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		return CompletableFuture.completedFuture(chunk);
	}

	static CompletableFuture<Chunk> generateStructures(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		ServerWorld world = context.world();
		if (world.getServer().getSaveProperties().getGeneratorOptions().shouldGenerateStructures()) {
			context.generator().setStructureStarts(
					world.getRegistryManager(),
					world.getChunkManager().getStructurePlacementCalculator(),
					world.getStructureAccessor(),
					chunk,
					context.structureManager(),
					world.getRegistryKey()
			);
		}

		world.cacheStructures(chunk);
		return CompletableFuture.completedFuture(chunk);
	}

	static CompletableFuture<Chunk> loadStructures(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		context.world().cacheStructures(chunk);
		return CompletableFuture.completedFuture(chunk);
	}

	static CompletableFuture<Chunk> generateStructureReferences(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		ServerWorld world = context.world();
		ChunkRegion region = new ChunkRegion(world, chunks, step, chunk);
		context.generator().addStructureReferences(region, world.getStructureAccessor().forRegion(region), chunk);
		return CompletableFuture.completedFuture(chunk);
	}

	static CompletableFuture<Chunk> populateBiomes(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		ServerWorld world = context.world();
		ChunkRegion region = new ChunkRegion(world, chunks, step, chunk);
		return context.generator().populateBiomes(
				world.getChunkManager().getNoiseConfig(),
				Blender.getBlender(region),
				world.getStructureAccessor().forRegion(region),
				chunk
		);
	}

	static CompletableFuture<Chunk> populateNoise(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		ServerWorld world = context.world();
		ChunkRegion region = new ChunkRegion(world, chunks, step, chunk);
		return context.generator()
				.populateNoise(
						Blender.getBlender(region),
						world.getChunkManager().getNoiseConfig(),
						world.getStructureAccessor().forRegion(region),
						chunk
				)
				.thenApply(populated -> {
					if (populated instanceof ProtoChunk protoChunk) {
						BelowZeroRetrogen retrogen = protoChunk.getBelowZeroRetrogen();
						if (retrogen != null) {
							BelowZeroRetrogen.replaceOldBedrock(protoChunk);
							if (retrogen.hasMissingBedrock()) {
								retrogen.fillColumnsWithAirIfMissingBedrock(protoChunk);
							}
						}
					}

					return (Chunk) populated;
				});
	}

	static CompletableFuture<Chunk> buildSurface(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		ServerWorld world = context.world();
		ChunkRegion region = new ChunkRegion(world, chunks, step, chunk);
		context.generator().buildSurface(
				region,
				world.getStructureAccessor().forRegion(region),
				world.getChunkManager().getNoiseConfig(),
				chunk
		);
		return CompletableFuture.completedFuture(chunk);
	}

	static CompletableFuture<Chunk> carve(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		ServerWorld world = context.world();
		ChunkRegion region = new ChunkRegion(world, chunks, step, chunk);
		if (chunk instanceof ProtoChunk protoChunk) {
			Blender.createCarvingMasks(region, protoChunk);
		}

		context.generator().carve(
				region,
				world.getSeed(),
				world.getChunkManager().getNoiseConfig(),
				world.getBiomeAccess(),
				world.getStructureAccessor().forRegion(region),
				chunk
		);
		return CompletableFuture.completedFuture(chunk);
	}

	static CompletableFuture<Chunk> generateFeatures(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		ServerWorld world = context.world();
		Heightmap.populateHeightmaps(
				chunk,
				EnumSet.of(
						Heightmap.Type.MOTION_BLOCKING,
						Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
						Heightmap.Type.OCEAN_FLOOR,
						Heightmap.Type.WORLD_SURFACE
				)
		);

		ChunkRegion region = new ChunkRegion(world, chunks, step, chunk);
		if (!SharedConstants.DISABLE_FEATURES) {
			context.generator().generateFeatures(region, chunk, world.getStructureAccessor().forRegion(region));
		}

		Blender.tickLeavesAndFluids(region, chunk);
		return CompletableFuture.completedFuture(chunk);
	}

	static CompletableFuture<Chunk> initializeLight(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		ServerLightingProvider lightingProvider = context.lightingProvider();
		chunk.refreshSurfaceY();
		((ProtoChunk) chunk).setLightingProvider(lightingProvider);
		return lightingProvider.initializeLight(chunk, isLightOn(chunk));
	}

	static CompletableFuture<Chunk> light(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		return context.lightingProvider().light(chunk, isLightOn(chunk));
	}

	static CompletableFuture<Chunk> generateEntities(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		if (!chunk.hasBelowZeroRetrogen()) {
			context.generator().populateEntities(new ChunkRegion(context.world(), chunks, step, chunk));
		}

		return CompletableFuture.completedFuture(chunk);
	}

	/**
	 * Финализирует генерацию: конвертирует {@link ProtoChunk} в {@link WorldChunk},
	 * загружает сущности и блок-энтити, регистрирует тик-планировщики.
	 * Выполняется в главном потоке через {@link ChunkGenerationContext#mainThreadExecutor()}.
	 */
	static CompletableFuture<Chunk> convertToFullChunk(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		ChunkPos chunkPos = chunk.getPos();
		AbstractChunkHolder holder = chunks.get(chunkPos.x, chunkPos.z);

		return CompletableFuture.supplyAsync(
				() -> {
					ProtoChunk protoChunk = (ProtoChunk) chunk;
					ServerWorld world = context.world();
					WorldChunk worldChunk;

					if (protoChunk instanceof WrapperProtoChunk wrapper) {
						worldChunk = wrapper.getWrappedChunk();
					} else {
						worldChunk = new WorldChunk(world, protoChunk, loadedChunk -> {
							try (ErrorReporter.Logging logging = new ErrorReporter.Logging(
									chunk.getErrorReporterContext(), LOGGER)
							) {
								addEntities(
										world,
										NbtReadView.createList(
												logging,
												world.getRegistryManager(),
												protoChunk.getEntities()
										)
								);
							}
						});
						holder.replaceWith(new WrapperProtoChunk(worldChunk, false));
					}

					worldChunk.setLevelTypeProvider(holder::getLevelType);
					worldChunk.loadEntities();
					worldChunk.setLoadedToWorld(true);
					worldChunk.updateAllBlockEntities();
					worldChunk.addChunkTickSchedulers(world);
					worldChunk.setUnsavedListener(context.unsavedListener());
					return worldChunk;
				},
				context.mainThreadExecutor()
		);
	}

	private static void addEntities(ServerWorld world, ReadView.ListReadView entities) {
		if (!entities.isEmpty()) {
			world.addEntities(EntityType.streamFromData(entities, world, SpawnReason.LOAD));
		}
	}
}
