package net.minecraft.structure;

import net.minecraft.util.math.BlockBox;
import org.jspecify.annotations.Nullable;

public interface StructurePiecesHolder {
   void addPiece(StructurePiece piece);

   @Nullable StructurePiece getIntersecting(BlockBox box);
}
