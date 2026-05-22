package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Критерий выполняется, когда игрок создаёт предмет по рецепту.
 * Поддерживает проверку конкретных ингредиентов, использованных при крафте.
 */
public class RecipeCraftedCriterion extends AbstractCriterion<RecipeCraftedCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, RegistryKey<Recipe<?>> recipeKey, List<ItemStack> ingredients) {
		trigger(player, conditions -> conditions.matches(recipeKey, ingredients));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			RegistryKey<Recipe<?>> recipeId,
			List<ItemPredicate> ingredients
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						Recipe.KEY_CODEC
								.fieldOf("recipe_id")
								.forGetter(Conditions::recipeId),
						ItemPredicate.CODEC
								.listOf()
								.optionalFieldOf("ingredients", List.of())
								.forGetter(Conditions::ingredients)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(
				RegistryKey<Recipe<?>> recipeKey,
				List<ItemPredicate.Builder> ingredients
		) {
			return Criteria.RECIPE_CRAFTED.create(new Conditions(
					Optional.empty(),
					recipeKey,
					ingredients.stream().map(ItemPredicate.Builder::build).toList()
			));
		}

		public static AdvancementCriterion<Conditions> create(RegistryKey<Recipe<?>> recipeKey) {
			return Criteria.RECIPE_CRAFTED.create(new Conditions(
					Optional.empty(),
					recipeKey,
					List.of()
			));
		}

		public static AdvancementCriterion<Conditions> createCrafterRecipeCrafted(RegistryKey<Recipe<?>> recipeKey) {
			return Criteria.CRAFTER_RECIPE_CRAFTED.create(new Conditions(
					Optional.empty(),
					recipeKey,
					List.of()
			));
		}

		/**
		 * Проверяет, что рецепт совпадает и все требуемые предикаты ингредиентов
		 * находят соответствие среди фактически использованных ингредиентов.
		 * Каждый предикат потребляет один ингредиент — повторное использование невозможно.
		 */
		boolean matches(RegistryKey<Recipe<?>> recipeKey, List<ItemStack> ingredients) {
			if (recipeKey != recipeId) {
				return false;
			}

			List<ItemStack> remaining = new ArrayList<>(ingredients);

			for (ItemPredicate predicate : this.ingredients) {
				boolean matched = false;
				Iterator<ItemStack> iterator = remaining.iterator();

				while (iterator.hasNext()) {
					if (predicate.test(iterator.next())) {
						iterator.remove();
						matched = true;
						break;
					}
				}

				if (!matched) {
					return false;
				}
			}

			return true;
		}
	}
}
