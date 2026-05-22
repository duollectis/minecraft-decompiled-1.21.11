package net.minecraft.advancement.criterion;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Базовая реализация критерия достижения. Управляет подпиской трекеров прогресса
 * и выполняет проверку условий при срабатывании триггера.
 *
 * @param <T> тип условий данного критерия
 */
public abstract class AbstractCriterion<T extends AbstractCriterion.Conditions> implements Criterion<T> {

	private final Map<PlayerAdvancementTracker, Set<Criterion.ConditionsContainer<T>>> progressions = new HashMap<>();

	@Override
	public final void beginTrackingCondition(
			PlayerAdvancementTracker manager,
			Criterion.ConditionsContainer<T> conditions
	) {
		progressions.computeIfAbsent(manager, key -> new HashSet<>()).add(conditions);
	}

	@Override
	public final void endTrackingCondition(
			PlayerAdvancementTracker manager,
			Criterion.ConditionsContainer<T> conditions
	) {
		Set<Criterion.ConditionsContainer<T>> tracked = progressions.get(manager);
		if (tracked == null) {
			return;
		}

		tracked.remove(conditions);
		if (tracked.isEmpty()) {
			progressions.remove(manager);
		}
	}

	@Override
	public final void endTracking(PlayerAdvancementTracker tracker) {
		progressions.remove(tracker);
	}

	/**
	 * Проверяет все отслеживаемые условия для данного игрока и выдаёт прогресс
	 * тем, которые прошли проверку предиката и условия на игрока.
	 *
	 * @param player    игрок, для которого сработал триггер
	 * @param predicate дополнительная проверка условий критерия
	 */
	protected void trigger(ServerPlayerEntity player, Predicate<T> predicate) {
		PlayerAdvancementTracker tracker = player.getAdvancementTracker();
		Set<Criterion.ConditionsContainer<T>> tracked = progressions.get(tracker);
		if (tracked == null || tracked.isEmpty()) {
			return;
		}

		LootContext playerContext = EntityPredicate.createAdvancementEntityLootContext(player, player);
		List<Criterion.ConditionsContainer<T>> matched = null;

		for (Criterion.ConditionsContainer<T> container : tracked) {
			T conditions = container.conditions();
			if (!predicate.test(conditions)) {
				continue;
			}

			Optional<LootContextPredicate> playerPredicate = conditions.player();
			if (playerPredicate.isPresent() && !playerPredicate.get().test(playerContext)) {
				continue;
			}

			if (matched == null) {
				matched = new ArrayList<>();
			}

			matched.add(container);
		}

		if (matched != null) {
			for (Criterion.ConditionsContainer<T> container : matched) {
				container.grant(tracker);
			}
		}
	}

	/**
	 * Базовый интерфейс условий для критериев, основанных на {@link AbstractCriterion}.
	 * Предоставляет стандартную валидацию предиката игрока.
	 */
	public interface Conditions extends CriterionConditions {

		Optional<LootContextPredicate> player();

		@Override
		default void validate(LootContextPredicateValidator validator) {
			validator.validateEntityPredicate(player(), "player");
		}
	}
}
