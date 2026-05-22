package net.minecraft.predicate.block;

import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * Предикат для проверки состояния блока с возможностью фильтрации по отдельным свойствам.
 * Создаётся через {@link #forBlock(Block)} и настраивается цепочкой вызовов {@link #with}.
 */
public class BlockStatePredicate implements Predicate<BlockState> {

	public static final Predicate<BlockState> ANY = state -> true;

	private final StateManager<Block, BlockState> manager;
	private final Map<Property<?>, Predicate<Object>> propertyTests = Maps.newHashMap();

	private BlockStatePredicate(StateManager<Block, BlockState> manager) {
		this.manager = manager;
	}

	public static BlockStatePredicate forBlock(Block block) {
		return new BlockStatePredicate(block.getStateManager());
	}

	public boolean test(@Nullable BlockState blockState) {
		if (blockState == null || !blockState.getBlock().equals(manager.getOwner())) {
			return false;
		}

		if (propertyTests.isEmpty()) {
			return true;
		}

		for (Entry<Property<?>, Predicate<Object>> entry : propertyTests.entrySet()) {
			if (!testProperty(blockState, entry.getKey(), entry.getValue())) {
				return false;
			}
		}

		return true;
	}

	protected <T extends Comparable<T>> boolean testProperty(
			BlockState blockState,
			Property<T> property,
			Predicate<Object> predicate
	) {
		T value = blockState.get(property);
		return predicate.test(value);
	}

	/**
	 * Добавляет условие проверки конкретного свойства блока.
	 *
	 * @param property  свойство, которое должно принадлежать менеджеру состояний этого блока
	 * @param predicate предикат для проверки значения свойства
	 * @return {@code this} для цепочки вызовов
	 * @throws IllegalArgumentException если свойство не принадлежит данному блоку
	 */
	public <V extends Comparable<V>> BlockStatePredicate with(Property<V> property, Predicate<Object> predicate) {
		if (!manager.getProperties().contains(property)) {
			throw new IllegalArgumentException(manager + " cannot support property " + property);
		}

		propertyTests.put(property, predicate);
		return this;
	}
}
