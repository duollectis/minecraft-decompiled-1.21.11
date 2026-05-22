package net.minecraft.world.chunk;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.concurrent.Executor;

/**
 * Контекст генерации чанка, передаваемый в каждый шаг пайплайна {@link ChunkGenerationSteps}.
 * Содержит все зависимости, необходимые для выполнения задач генерации.
 */
public record ChunkGenerationContext(
		ServerWorld world,
		ChunkGenerator generator,
		StructureTemplateManager structureManager,
		ServerLightingProvider lightingProvider,
		Executor mainThreadExecutor,
		WorldChunk.UnsavedListener unsavedListener
) {
}
