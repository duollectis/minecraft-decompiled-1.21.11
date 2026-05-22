package net.minecraft.structure.pool.alias;

import com.google.common.collect.ImmutableMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Функциональный интерфейс для разрешения псевдонимов пулов структур.
 * Принимает ключ пула и возвращает реальный ключ, на который он отображён.
 * Если псевдоним не зарегистрирован, возвращает исходный ключ без изменений.
 */
@FunctionalInterface
public interface StructurePoolAliasLookup {

	StructurePoolAliasLookup EMPTY = pool -> pool;

	RegistryKey<StructurePool> lookup(RegistryKey<StructurePool> pool);

	/**
	 * Создаёт таблицу разрешения псевдонимов из списка привязок.
	 * Генератор случайных чисел инициализируется детерминированно на основе
	 * {@code seed} и позиции {@code pos}, чтобы выбор псевдонима был воспроизводимым.
	 *
	 * @param bindings список привязок псевдонимов
	 * @param pos позиция структуры для детерминированного сида
	 * @param seed базовый сид мира
	 * @return таблица разрешения псевдонимов
	 */
	static StructurePoolAliasLookup create(List<StructurePoolAliasBinding> bindings, BlockPos pos, long seed) {
		if (bindings.isEmpty()) {
			return EMPTY;
		}

		Random random = Random.create(seed).nextSplitter().split(pos);
		ImmutableMap.Builder<RegistryKey<StructurePool>, RegistryKey<StructurePool>> builder = ImmutableMap.builder();
		bindings.forEach(binding -> binding.forEach(random, builder::put));
		Map<RegistryKey<StructurePool>, RegistryKey<StructurePool>> aliasMap = builder.build();

		return alias -> Objects.requireNonNull(
			aliasMap.getOrDefault(alias, alias),
			() -> "alias " + alias.getValue() + " was mapped to null value"
		);
	}
}
