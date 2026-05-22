package net.minecraft.structure;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.FeatureConfig;

/**
 * Генератор кусков структуры. Получает контекст генерации и заполняет
 * {@link StructurePiecesCollector} набором {@link StructurePiece}, из которых
 * будет собрана структура в мире.
 */
@FunctionalInterface
public interface StructurePiecesGenerator<C extends FeatureConfig> {

	void generatePieces(StructurePiecesCollector collector, StructurePiecesGenerator.Context<C> context);

	/**
	 * Контекст генерации кусков структуры. Содержит конфигурацию, генератор чанков,
	 * менеджер шаблонов, позицию чанка, ограничения высоты, генератор случайных чисел
	 * и зерно мира.
	 */
	public record Context<C extends FeatureConfig>(
			C config,
			ChunkGenerator chunkGenerator,
			StructureTemplateManager structureTemplateManager,
			ChunkPos chunkPos,
			HeightLimitView world,
			ChunkRandom random,
			long seed
	) {
	}
}
