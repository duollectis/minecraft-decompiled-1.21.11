package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;

/**
 * Базовый контракт для всех критериев достижений. Управляет жизненным циклом отслеживания
 * условий для конкретного игрока через {@link PlayerAdvancementTracker}.
 */
public interface Criterion<T extends CriterionConditions> {

	void beginTrackingCondition(PlayerAdvancementTracker manager, ConditionsContainer<T> conditions);

	void endTrackingCondition(PlayerAdvancementTracker manager, ConditionsContainer<T> conditions);

	void endTracking(PlayerAdvancementTracker tracker);

	Codec<T> getConditionsCodec();

	default AdvancementCriterion<T> create(T conditions) {
		return new AdvancementCriterion<>(this, conditions);
	}

	/**
	 * Связывает набор условий критерия с конкретным достижением и именем критерия,
	 * позволяя выдать его через трекер при выполнении.
	 */
	record ConditionsContainer<T extends CriterionConditions>(
			T conditions,
			AdvancementEntry advancement,
			String id
	) {

		public void grant(PlayerAdvancementTracker tracker) {
			tracker.grantCriterion(advancement, id);
		}
	}
}
