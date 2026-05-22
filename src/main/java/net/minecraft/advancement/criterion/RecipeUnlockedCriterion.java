package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий выполняется, когда игрок разблокирует рецепт крафта.
 */
public class RecipeUnlockedCriterion extends AbstractCriterion<RecipeUnlockedCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, RecipeEntry<?> recipe) {
		trigger(player, conditions -> conditions.matches(recipe));
	}

	public static AdvancementCriterion<RecipeUnlockedCriterion.Conditions> create(RegistryKey<Recipe<?>> registryKey) {
		return Criteria.RECIPE_UNLOCKED.create(new Conditions(Optional.empty(), registryKey));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			RegistryKey<Recipe<?>> recipe
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						Recipe.KEY_CODEC
								.fieldOf("recipe")
								.forGetter(Conditions::recipe)
				).apply(instance, Conditions::new)
		);

		public boolean matches(RecipeEntry<?> recipe) {
			return this.recipe == recipe.id();
		}
	}
}
