package net.minecraft.block.entity;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

/**
 * Контракт для блок-сущностей, которые умеют отображать ограничивающий бокс структуры в режиме отладки.
 * Реализуется {@link StructureBlockBlockEntity} и {@link TestInstanceBlockEntity}.
 */
public interface StructureBoxRendering {

	RenderMode getRenderMode();

	StructureBox getStructureBox();

	enum RenderMode {
		NONE,
		BOX,
		BOX_AND_INVISIBLE_BLOCKS;
	}

	record StructureBox(BlockPos localPos, Vec3i size) {

		/**
		 * Создаёт бокс по двум угловым точкам, автоматически нормализуя координаты
		 * так, чтобы {@code localPos} всегда указывал на минимальный угол.
		 */
		public static StructureBox create(
				int minX,
				int minY,
				int minZ,
				int maxX,
				int maxY,
				int maxZ
		) {
			int normalizedMinX = Math.min(minX, maxX);
			int normalizedMinY = Math.min(minY, maxY);
			int normalizedMinZ = Math.min(minZ, maxZ);

			return new StructureBox(
					new BlockPos(normalizedMinX, normalizedMinY, normalizedMinZ),
					new Vec3i(
							Math.max(minX, maxX) - normalizedMinX,
							Math.max(minY, maxY) - normalizedMinY,
							Math.max(minZ, maxZ) - normalizedMinZ
					)
			);
		}
	}
}
