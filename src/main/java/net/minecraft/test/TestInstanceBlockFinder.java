package net.minecraft.test;

import net.minecraft.util.math.BlockPos;

import java.util.stream.Stream;

@FunctionalInterface
/**
 * {@code TestInstanceBlockFinder}.
 */
public interface TestInstanceBlockFinder {

	Stream<BlockPos> findTestPos();
}
