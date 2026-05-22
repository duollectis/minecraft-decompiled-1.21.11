package net.minecraft.world.poi;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Тип точки интереса (POI), определяющий набор блоков, которые её образуют,
 * максимальное количество билетов (слотов для сущностей) и радиус поиска.
 */
public record PointOfInterestType(Set<BlockState> blockStates, int ticketCount, int searchDistance) {

	public static final Predicate<RegistryEntry<PointOfInterestType>> NONE = type -> false;

	/**
	 * Создаёт тип POI, защищая переданный набор блоков от внешних изменений.
	 */
	public PointOfInterestType(Set<BlockState> blockStates, int ticketCount, int searchDistance) {
		blockStates = Set.copyOf(blockStates);
		this.blockStates = blockStates;
		this.ticketCount = ticketCount;
		this.searchDistance = searchDistance;
	}

	public boolean contains(BlockState state) {
		return blockStates.contains(state);
	}
}
