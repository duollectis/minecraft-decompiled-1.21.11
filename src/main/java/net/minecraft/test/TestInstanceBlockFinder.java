package net.minecraft.test;

import net.minecraft.util.math.BlockPos;

import java.util.stream.Stream;

/**
 * Функциональный интерфейс для поиска позиций тестовых блоков в мире.
 */
@FunctionalInterface
public interface TestInstanceBlockFinder {

	Stream<BlockPos> findTestPos();
}
