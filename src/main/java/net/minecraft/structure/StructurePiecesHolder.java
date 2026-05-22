package net.minecraft.structure;

import net.minecraft.util.math.BlockBox;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс для накопления кусков структуры во время генерации.
 * Позволяет добавлять куски и проверять пересечения с существующими.
 */
public interface StructurePiecesHolder {

	void addPiece(StructurePiece piece);

	@Nullable StructurePiece getIntersecting(BlockBox box);
}
