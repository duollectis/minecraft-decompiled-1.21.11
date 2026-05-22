package net.minecraft.datafixer.fix;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.List;
import java.util.Optional;

/**
 * Мигрирует данные рецептов в печах, доменных печах и коптильнях:
 * заменяет пронумерованные поля {@code RecipeLocation0..N} и {@code RecipeAmount0..N}
 * на единый список {@code RecipesUsed} с парами (рецепт, количество).
 */
public class FurnaceRecipesFix extends DataFix {

	public FurnaceRecipesFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return updateBlockEntities(getOutputSchema().getTypeRaw(TypeReferences.RECIPE));
	}

	private <R> TypeRewriteRule updateBlockEntities(Type<R> recipeType) {
		Type<Pair<Either<Pair<List<Pair<R, Integer>>, Dynamic<?>>, Unit>, Dynamic<?>>> recipesUsedType = DSL.and(
			DSL.optional(DSL.field(
				"RecipesUsed",
				DSL.and(DSL.compoundList(recipeType, DSL.intType()), DSL.remainderType())
			)),
			DSL.remainderType()
		);

		OpticFinder<?> furnaceFinder = DSL.namedChoice(
			"minecraft:furnace",
			getInputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, "minecraft:furnace")
		);
		OpticFinder<?> blastFurnaceFinder = DSL.namedChoice(
			"minecraft:blast_furnace",
			getInputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, "minecraft:blast_furnace")
		);
		OpticFinder<?> smokerFinder = DSL.namedChoice(
			"minecraft:smoker",
			getInputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, "minecraft:smoker")
		);

		Type<?> outputFurnaceType = getOutputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, "minecraft:furnace");
		Type<?> outputBlastFurnaceType = getOutputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, "minecraft:blast_furnace");
		Type<?> outputSmokerType = getOutputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, "minecraft:smoker");

		Type<?> inputBlockEntityType = getInputSchema().getType(TypeReferences.BLOCK_ENTITY);
		Type<?> outputBlockEntityType = getOutputSchema().getType(TypeReferences.BLOCK_ENTITY);

		return fixTypeEverywhereTyped(
			"FurnaceRecipesFix",
			inputBlockEntityType,
			outputBlockEntityType,
			blockEntity -> blockEntity
				.updateTyped(furnaceFinder, outputFurnaceType, furnace ->
					updateBlockEntityData(recipeType, recipesUsedType, furnace)
				)
				.updateTyped(blastFurnaceFinder, outputBlastFurnaceType, blastFurnace ->
					updateBlockEntityData(recipeType, recipesUsedType, blastFurnace)
				)
				.updateTyped(smokerFinder, outputSmokerType, smoker ->
					updateBlockEntityData(recipeType, recipesUsedType, smoker)
				)
		);
	}

	private <R> Typed<?> updateBlockEntityData(
		Type<R> recipeType,
		Type<Pair<Either<Pair<List<Pair<R, Integer>>, Dynamic<?>>, Unit>, Dynamic<?>>> recipesUsedType,
		Typed<?> smelter
	) {
		Dynamic<?> data = (Dynamic<?>) smelter.getOrCreate(DSL.remainderFinder());
		int recipeCount = data.get("RecipesUsedSize").asInt(0);
		data = data.remove("RecipesUsedSize");

		List<Pair<R, Integer>> recipes = Lists.newArrayList();

		for (int index = 0; index < recipeCount; index++) {
			String locationKey = "RecipeLocation" + index;
			String amountKey = "RecipeAmount" + index;

			Optional<? extends Dynamic<?>> locationDynamic = data.get(locationKey).result();
			int amount = data.get(amountKey).asInt(0);

			if (amount > 0) {
				locationDynamic.ifPresent(location -> {
					Optional<? extends Pair<R, ? extends Dynamic<?>>> parsed = recipeType.read(location).result();
					parsed.ifPresent(pair -> recipes.add(Pair.of(pair.getFirst(), amount)));
				});
			}

			data = data.remove(locationKey).remove(amountKey);
		}

		return smelter.set(
			DSL.remainderFinder(),
			recipesUsedType,
			Pair.of(Either.left(Pair.of(recipes, data.emptyMap())), data)
		);
	}
}
