package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Критерий: игрок убит стрелой (или несколько сущностей пробиты одной стрелой).
 * Проверяет список жертв, количество уникальных типов сущностей и оружие.
 */
public class KilledByArrowCriterion extends AbstractCriterion<KilledByArrowCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(
			ServerPlayerEntity player,
			Collection<Entity> piercingKilledEntities,
			@Nullable ItemStack weapon
	) {
		List<LootContext> victimContexts = new ArrayList<>();
		Set<EntityType<?>> uniqueTypes = new HashSet<>();

		for (Entity entity : piercingKilledEntities) {
			uniqueTypes.add(entity.getType());
			victimContexts.add(EntityPredicate.createAdvancementEntityLootContext(player, entity));
		}

		trigger(player, conditions -> conditions.matches(victimContexts, uniqueTypes.size(), weapon));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			List<LootContextPredicate> victims,
			NumberRange.IntRange uniqueEntityTypes,
			Optional<ItemPredicate> firedFromWeapon
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.listOf()
								.optionalFieldOf("victims", List.of())
								.forGetter(Conditions::victims),
						NumberRange.IntRange.CODEC
								.optionalFieldOf("unique_entity_types", NumberRange.IntRange.ANY)
								.forGetter(Conditions::uniqueEntityTypes),
						ItemPredicate.CODEC
								.optionalFieldOf("fired_from_weapon")
								.forGetter(Conditions::firedFromWeapon)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> createCrossbow(
				RegistryEntryLookup<Item> itemRegistry, EntityPredicate.Builder... victims
		) {
			return Criteria.KILLED_BY_ARROW.create(new Conditions(
					Optional.empty(),
					EntityPredicate.contextPredicateFromEntityPredicates(victims),
					NumberRange.IntRange.ANY,
					Optional.of(ItemPredicate.Builder.create().items(itemRegistry, Items.CROSSBOW).build())
			));
		}

		public static AdvancementCriterion<Conditions> createCrossbow(
				RegistryEntryLookup<Item> itemRegistry, NumberRange.IntRange uniqueEntityTypeCount
		) {
			return Criteria.KILLED_BY_ARROW.create(new Conditions(
					Optional.empty(),
					List.of(),
					uniqueEntityTypeCount,
					Optional.of(ItemPredicate.Builder.create().items(itemRegistry, Items.CROSSBOW).build())
			));
		}

		/**
		 * Проверяет оружие, список жертв и количество уникальных типов сущностей.
		 * Для каждого предиката жертвы ищет первое совпадение в списке контекстов и удаляет его.
		 */
		public boolean matches(
				Collection<LootContext> victimContexts,
				int uniqueEntityTypeCount,
				@Nullable ItemStack weapon
		) {
			if (firedFromWeapon.isPresent() && (weapon == null || !firedFromWeapon.get().test(weapon))) {
				return false;
			}

			if (!victims.isEmpty()) {
				List<LootContext> remaining = new ArrayList<>(victimContexts);

				for (LootContextPredicate victimPredicate : victims) {
					boolean matched = false;
					Iterator<LootContext> iterator = remaining.iterator();

					while (iterator.hasNext()) {
						if (victimPredicate.test(iterator.next())) {
							iterator.remove();
							matched = true;
							break;
						}
					}

					if (!matched) {
						return false;
					}
				}
			}

			return uniqueEntityTypes.test(uniqueEntityTypeCount);
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicates(victims, "victims");
		}
	}
}
