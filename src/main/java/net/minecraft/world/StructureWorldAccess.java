package net.minecraft.world;

import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * {@code StructureWorldAccess}.
 */
public interface StructureWorldAccess extends ServerWorldAccess {

	long getSeed();

	default boolean isValidForSetBlock(BlockPos pos) {
		return true;
	}

	default void setCurrentlyGeneratingStructureName(@Nullable Supplier<String> structureName) {
	}
}
