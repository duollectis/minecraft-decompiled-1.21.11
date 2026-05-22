package net.minecraft.registry;

import net.minecraft.util.Util;

import java.util.*;
import java.util.stream.Stream;

/**
 * Многоуровневый менеджер динамических реестров, организованный по слоям.
 * Каждый слой соответствует одному из типов {@link ServerDynamicRegistryType}
 * и содержит свой {@link DynamicRegistryManager.Immutable}.
 *
 * <p>Слои упорядочены по приоритету: STATIC → WORLDGEN → DIMENSIONS → RELOADABLE.
 * При объединении реестров из нескольких слоёв дубликаты не допускаются.</p>
 *
 * @param <T> тип перечисления слоёв (обычно {@link ServerDynamicRegistryType})
 */
public class CombinedDynamicRegistries<T> {

	private final List<T> types;
	private final List<DynamicRegistryManager.Immutable> registryManagers;
	private final DynamicRegistryManager.Immutable combinedRegistryManager;

	/**
	 * Создаёт менеджер с пустыми реестрами для каждого из указанных типов.
	 *
	 * @param types список типов слоёв в порядке приоритета
	 */
	public CombinedDynamicRegistries(List<T> types) {
		this(
				types,
				Util.make(() -> {
					DynamicRegistryManager.Immutable[] immutables = new DynamicRegistryManager.Immutable[types.size()];
					Arrays.fill(immutables, DynamicRegistryManager.EMPTY);
					return Arrays.asList(immutables);
				})
		);
	}

	private CombinedDynamicRegistries(List<T> types, List<DynamicRegistryManager.Immutable> registryManagers) {
		this.types = List.copyOf(types);
		this.registryManagers = List.copyOf(registryManagers);
		this.combinedRegistryManager = new DynamicRegistryManager.ImmutableImpl(
				toRegistryMap(registryManagers.stream())
		).toImmutable();
	}

	private int getIndex(T type) {
		int index = types.indexOf(type);

		if (index == -1) {
			throw new IllegalStateException("Can't find " + type + " inside " + types);
		}

		return index;
	}

	/**
	 * Возвращает менеджер реестров для конкретного слоя.
	 *
	 * @param type тип слоя
	 * @return иммутабельный менеджер реестров данного слоя
	 */
	public DynamicRegistryManager.Immutable get(T type) {
		return registryManagers.get(getIndex(type));
	}

	/**
	 * Возвращает объединённый менеджер всех слоёв, предшествующих указанному (не включая его).
	 *
	 * @param type тип слоя-границы (не включается в результат)
	 * @return объединённый менеджер предшествующих слоёв
	 */
	public DynamicRegistryManager.Immutable getPrecedingRegistryManagers(T type) {
		return subset(0, getIndex(type));
	}

	/**
	 * Возвращает объединённый менеджер указанного слоя и всех последующих.
	 *
	 * @param type тип начального слоя (включается в результат)
	 * @return объединённый менеджер данного и последующих слоёв
	 */
	public DynamicRegistryManager.Immutable getSucceedingRegistryManagers(T type) {
		return subset(getIndex(type), registryManagers.size());
	}

	private DynamicRegistryManager.Immutable subset(int startIndex, int endIndex) {
		return new DynamicRegistryManager.ImmutableImpl(
				toRegistryMap(registryManagers.subList(startIndex, endIndex).stream())
		).toImmutable();
	}

	/**
	 * Заменяет реестры начиная с указанного слоя на переданные менеджеры.
	 * Оставшиеся слои заполняются пустыми менеджерами.
	 *
	 * @param type             тип слоя, с которого начинается замена
	 * @param registryManagers новые менеджеры реестров
	 * @return новый экземпляр с обновлёнными слоями
	 */
	public CombinedDynamicRegistries<T> with(T type, DynamicRegistryManager.Immutable... registryManagers) {
		return with(type, Arrays.asList(registryManagers));
	}

	/**
	 * Заменяет реестры начиная с указанного слоя на переданный список менеджеров.
	 * Оставшиеся слои заполняются пустыми менеджерами.
	 *
	 * @param type             тип слоя, с которого начинается замена
	 * @param newManagers      новые менеджеры реестров
	 * @return новый экземпляр с обновлёнными слоями
	 * @throws IllegalStateException если передано больше менеджеров, чем доступно слотов
	 */
	public CombinedDynamicRegistries<T> with(T type, List<DynamicRegistryManager.Immutable> newManagers) {
		int startIndex = getIndex(type);

		if (newManagers.size() > registryManagers.size() - startIndex) {
			throw new IllegalStateException("Too many values to replace");
		}

		List<DynamicRegistryManager.Immutable> result = new ArrayList<>();

		for (int index = 0; index < startIndex; index++) {
			result.add(registryManagers.get(index));
		}

		result.addAll(newManagers);

		while (result.size() < registryManagers.size()) {
			result.add(DynamicRegistryManager.EMPTY);
		}

		return new CombinedDynamicRegistries<>(types, result);
	}

	/**
	 * Возвращает объединённый менеджер всех слоёв.
	 *
	 * @return иммутабельный менеджер, объединяющий все слои
	 */
	public DynamicRegistryManager.Immutable getCombinedRegistryManager() {
		return combinedRegistryManager;
	}

	/**
	 * Объединяет реестры из нескольких менеджеров в единую карту.
	 * Дубликаты ключей реестров не допускаются.
	 *
	 * @param managers поток менеджеров для объединения
	 * @return карта ключей реестров к реестрам
	 * @throws IllegalStateException при обнаружении дублирующегося реестра
	 */
	private static Map<RegistryKey<? extends Registry<?>>, Registry<?>> toRegistryMap(
			Stream<? extends DynamicRegistryManager> managers
	) {
		Map<RegistryKey<? extends Registry<?>>, Registry<?>> map = new HashMap<>();

		managers.forEach(manager -> manager.streamAllRegistries().forEach(entry -> {
			if (map.put(entry.key(), entry.value()) != null) {
				throw new IllegalStateException("Duplicated registry " + entry.key());
			}
		}));

		return map;
	}
}
