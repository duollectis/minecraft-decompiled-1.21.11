package net.minecraft.entity.spawn;

import com.mojang.serialization.Codec;
import net.minecraft.entity.VariantSelectorProvider;

import java.util.List;

/**
 * {@code SpawnConditionSelectors}.
 */
public record SpawnConditionSelectors(List<VariantSelectorProvider.Selector<SpawnContext, SpawnCondition>> selectors) {

	public static final SpawnConditionSelectors EMPTY = new SpawnConditionSelectors(List.of());
	public static final Codec<SpawnConditionSelectors>
			CODEC =
			VariantSelectorProvider.Selector.createCodec(SpawnCondition.CODEC)
			                                .listOf()
			                                .xmap(SpawnConditionSelectors::new, SpawnConditionSelectors::selectors);

	/**
	 * Создаёт single.
	 *
	 * @param condition condition
	 * @param priority priority
	 *
	 * @return SpawnConditionSelectors — результат операции
	 */
	public static SpawnConditionSelectors createSingle(SpawnCondition condition, int priority) {
		return new SpawnConditionSelectors(VariantSelectorProvider.createSingle(condition, priority));
	}

	/**
	 * Создаёт fallback.
	 *
	 * @param priority priority
	 *
	 * @return SpawnConditionSelectors — результат операции
	 */
	public static SpawnConditionSelectors createFallback(int priority) {
		return new SpawnConditionSelectors(VariantSelectorProvider.createFallback(priority));
	}
}
