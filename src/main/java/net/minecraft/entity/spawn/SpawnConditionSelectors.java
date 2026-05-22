package net.minecraft.entity.spawn;

import com.mojang.serialization.Codec;
import net.minecraft.entity.VariantSelectorProvider;

import java.util.List;

/**
 * Контейнер списка селекторов условий спауна для одного варианта сущности.
 * Каждый {@link VariantSelectorProvider.Selector} хранит опциональное условие и приоритет.
 * При выборе варианта система перебирает все селекторы и выбирает подходящий
 * с наивысшим приоритетом через {@link VariantSelectorProvider#select}.
 */
public record SpawnConditionSelectors(
	List<VariantSelectorProvider.Selector<SpawnContext, SpawnCondition>> selectors
) {

	public static final SpawnConditionSelectors EMPTY = new SpawnConditionSelectors(List.of());

	public static final Codec<SpawnConditionSelectors> CODEC = VariantSelectorProvider.Selector
		.createCodec(SpawnCondition.CODEC)
		.listOf()
		.xmap(SpawnConditionSelectors::new, SpawnConditionSelectors::selectors);

	/**
	 * Создаёт селекторы с единственным условием и заданным приоритетом.
	 *
	 * @param condition условие спауна
	 * @param priority приоритет выбора варианта
	 * @return обёртка с одним селектором
	 */
	public static SpawnConditionSelectors createSingle(SpawnCondition condition, int priority) {
		return new SpawnConditionSelectors(VariantSelectorProvider.createSingle(condition, priority));
	}

	/**
	 * Создаёт резервный (fallback) селектор без условия с заданным приоритетом.
	 * Используется как вариант по умолчанию, когда ни одно условное правило не подошло.
	 *
	 * @param priority приоритет резервного варианта
	 * @return обёртка с одним безусловным селектором
	 */
	public static SpawnConditionSelectors createFallback(int priority) {
		return new SpawnConditionSelectors(VariantSelectorProvider.createFallback(priority));
	}
}
