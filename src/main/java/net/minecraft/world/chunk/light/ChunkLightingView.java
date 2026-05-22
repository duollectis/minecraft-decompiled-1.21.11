package net.minecraft.world.chunk.light;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkNibbleArray;
import org.jspecify.annotations.Nullable;

/**
 * Представление освещения для отдельного чанка.
 * Расширяет {@link LightingView} методами получения данных о свете на уровне секций и блоков.
 * Содержит заглушку {@link Empty} для измерений без освещения.
 */
public interface ChunkLightingView extends LightingView {

	@Nullable ChunkNibbleArray getLightSection(ChunkSectionPos pos);

	int getLightLevel(BlockPos pos);

	/**
	 * Заглушка-реализация для измерений без освещения (например, Nether без неба).
	 */
	enum Empty implements ChunkLightingView {
		INSTANCE;

		@Override
		public @Nullable ChunkNibbleArray getLightSection(ChunkSectionPos pos) {
			return null;
		}

		@Override
		public int getLightLevel(BlockPos pos) {
			return 0;
		}

		@Override
		public void checkBlock(BlockPos pos) {
		}

		@Override
		public boolean hasUpdates() {
			return false;
		}

		@Override
		public int doLightUpdates() {
			return 0;
		}

		@Override
		public void setSectionStatus(ChunkSectionPos pos, boolean notReady) {
		}

		@Override
		public void setColumnEnabled(ChunkPos pos, boolean retainData) {
		}

		@Override
		public void propagateLight(ChunkPos chunkPos) {
		}
	}
}
