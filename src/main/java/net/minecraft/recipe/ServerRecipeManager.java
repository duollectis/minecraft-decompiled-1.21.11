package net.minecraft.recipe;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.recipe.v1.FabricServerRecipeManager;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * {@code ServerRecipeManager}.
 */
public class ServerRecipeManager extends SinglePreparationResourceReloader<PreparedRecipes> implements RecipeManager, FabricServerRecipeManager {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Map<RegistryKey<RecipePropertySet>, ServerRecipeManager.SoleIngredientGetter>
			SOLE_INGREDIENT_GETTERS =
			Map.of(
					RecipePropertySet.SMITHING_ADDITION,
					recipe -> recipe instanceof SmithingRecipe smithingRecipe ? smithingRecipe.addition()
					                                                          : Optional.empty(),
					RecipePropertySet.SMITHING_BASE,
					recipe -> recipe instanceof SmithingRecipe smithingRecipe ? Optional.of(smithingRecipe.base())
					                                                          : Optional.empty(),
					RecipePropertySet.SMITHING_TEMPLATE,
					recipe -> recipe instanceof SmithingRecipe smithingRecipe ? smithingRecipe.template()
					                                                          : Optional.empty(),
					RecipePropertySet.FURNACE_INPUT,
					cookingIngredientGetter(RecipeType.SMELTING),
					RecipePropertySet.BLAST_FURNACE_INPUT,
					cookingIngredientGetter(RecipeType.BLASTING),
					RecipePropertySet.SMOKER_INPUT,
					cookingIngredientGetter(RecipeType.SMOKING),
					RecipePropertySet.CAMPFIRE_INPUT,
					cookingIngredientGetter(RecipeType.CAMPFIRE_COOKING)
			);
	private static final ResourceFinder FINDER = ResourceFinder.json(RegistryKeys.RECIPE);
	private final RegistryWrapper.WrapperLookup registries;
	private PreparedRecipes preparedRecipes = PreparedRecipes.EMPTY;
	private Map<RegistryKey<RecipePropertySet>, RecipePropertySet> propertySets = Map.of();
	private CuttingRecipeDisplay.Grouping<StonecuttingRecipe>
			stonecutterRecipes =
			CuttingRecipeDisplay.Grouping.empty();
	private List<ServerRecipeManager.ServerRecipe> recipes = List.of();
	private Map<RegistryKey<Recipe<?>>, List<ServerRecipeManager.ServerRecipe>> recipesByKey = Map.of();

	public ServerRecipeManager(RegistryWrapper.WrapperLookup registries) {
		this.registries = registries;
	}

	protected PreparedRecipes prepare(ResourceManager resourceManager, Profiler profiler) {
		SortedMap<Identifier, Recipe<?>> sortedMap = new TreeMap<>();
		JsonDataLoader.load(resourceManager, FINDER, this.registries.getOps(JsonOps.INSTANCE), Recipe.CODEC, sortedMap);
		List<RecipeEntry<?>> list = new ArrayList<>(sortedMap.size());
		sortedMap.forEach((id, recipe) -> {
			RegistryKey<Recipe<?>> registryKey = RegistryKey.of(RegistryKeys.RECIPE, id);
			RecipeEntry<?> recipeEntry = new RecipeEntry<>(registryKey, recipe);
			list.add(recipeEntry);
		});
		return PreparedRecipes.of(list);
	}

	protected void apply(PreparedRecipes preparedRecipes, ResourceManager resourceManager, Profiler profiler) {
		this.preparedRecipes = preparedRecipes;
		LOGGER.info("Loaded {} recipes", preparedRecipes.recipes().size());
	}

	public void initialize(FeatureSet features) {
		List<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> list = new ArrayList<>();
		List<ServerRecipeManager.PropertySetBuilder> list2 = SOLE_INGREDIENT_GETTERS.entrySet()
		                                                                            .stream()
		                                                                            .map(entry -> new ServerRecipeManager.PropertySetBuilder(
				                                                                            entry.getKey(),
				                                                                            entry.getValue()
		                                                                            ))
		                                                                            .toList();
		this.preparedRecipes
				.recipes()
				.forEach(
						recipe -> {
							Recipe<?> recipe2 = recipe.value();
							if (!recipe2.isIgnoredInRecipeBook() && recipe2.getIngredientPlacement().hasNoPlacement()) {
								LOGGER.warn(
										"Recipe {} can't be placed due to empty ingredients and will be ignored",
										recipe.id().getValue()
								);
							}
							else {
								list2.forEach(builder -> builder.accept(recipe2));
								if (recipe2 instanceof StonecuttingRecipe stonecuttingRecipe
										&& isEnabled(features, stonecuttingRecipe.ingredient())
										&& stonecuttingRecipe.createResultDisplay().isEnabled(features)) {
									list.add(
											new CuttingRecipeDisplay.GroupEntry<>(
													stonecuttingRecipe.ingredient(),
													new CuttingRecipeDisplay<>(
															stonecuttingRecipe.createResultDisplay(),
															Optional.of((RecipeEntry<StonecuttingRecipe>) recipe)
													)
											)
									);
								}
							}
						}
				);
		this.propertySets =
				list2
						.stream()
						.collect(Collectors.toUnmodifiableMap(
								builder -> builder.propertySetKey,
								builder -> builder.build(features)
						));
		this.stonecutterRecipes = new CuttingRecipeDisplay.Grouping<>(list);
		this.recipes = collectServerRecipes(this.preparedRecipes.recipes(), features);
		this.recipesByKey =
				this.recipes
						.stream()
						.collect(Collectors.groupingBy(
								recipe -> recipe.parent.id(),
								IdentityHashMap::new,
								Collectors.toList()
						));
	}

	static List<Ingredient> filterIngredients(FeatureSet features, List<Ingredient> ingredients) {
		ingredients.removeIf(ingredient -> !isEnabled(features, ingredient));
		return ingredients;
	}

	private static boolean isEnabled(FeatureSet features, Ingredient ingredient) {
		return ingredient.getMatchingItems().allMatch(entry -> entry.value().isEnabled(features));
	}

	public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeEntry<T>> getFirstMatch(
			RecipeType<T> type, I input, World world, @Nullable RegistryKey<Recipe<?>> recipe
	) {
		RecipeEntry<T> recipeEntry = recipe != null ? this.get(type, recipe) : null;
		return this.getFirstMatch(type, input, world, recipeEntry);
	}

	public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeEntry<T>> getFirstMatch(
			RecipeType<T> type, I input, World world, @Nullable RecipeEntry<T> recipe
	) {
		return recipe != null && recipe.value().matches(input, world) ? Optional.of(recipe)
		                                                              : this.getFirstMatch(type, input, world);
	}

	public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeEntry<T>> getFirstMatch(
			RecipeType<T> type,
			I input,
			World world
	) {
		return this.preparedRecipes.find(type, input, world).findFirst();
	}

	public Optional<RecipeEntry<?>> get(RegistryKey<Recipe<?>> key) {
		return Optional.ofNullable(this.preparedRecipes.get(key));
	}

	private <T extends Recipe<?>> @Nullable RecipeEntry<T> get(RecipeType<T> type, RegistryKey<Recipe<?>> key) {
		RecipeEntry<?> recipeEntry = this.preparedRecipes.get(key);
		return (RecipeEntry<T>) (recipeEntry != null && recipeEntry.value().getType().equals(type) ? recipeEntry : null
		);
	}

	public Map<RegistryKey<RecipePropertySet>, RecipePropertySet> getPropertySets() {
		return this.propertySets;
	}

	public CuttingRecipeDisplay.Grouping<StonecuttingRecipe> getStonecutterRecipeForSync() {
		return this.stonecutterRecipes;
	}

	@Override
	public RecipePropertySet getPropertySet(RegistryKey<RecipePropertySet> key) {
		return this.propertySets.getOrDefault(key, RecipePropertySet.EMPTY);
	}

	@Override
	public CuttingRecipeDisplay.Grouping<StonecuttingRecipe> getStonecutterRecipes() {
		return this.stonecutterRecipes;
	}

	public Collection<RecipeEntry<?>> values() {
		return this.preparedRecipes.recipes();
	}

	public ServerRecipeManager.@Nullable ServerRecipe get(NetworkRecipeId id) {
		int i = id.index();
		return i >= 0 && i < this.recipes.size() ? this.recipes.get(i) : null;
	}

	public void forEachRecipeDisplay(RegistryKey<Recipe<?>> key, Consumer<RecipeDisplayEntry> action) {
		List<ServerRecipeManager.ServerRecipe> list = this.recipesByKey.get(key);
		if (list != null) {
			list.forEach(recipe -> action.accept(recipe.display));
		}
	}

	@VisibleForTesting
	protected static RecipeEntry<?> deserialize(
			RegistryKey<Recipe<?>> key,
			JsonObject json,
			RegistryWrapper.WrapperLookup registries
	) {
		Recipe<?>
				recipe =
				(Recipe<?>) Recipe.CODEC
						.parse(registries.getOps(JsonOps.INSTANCE), json)
						.getOrThrow(JsonParseException::new);
		return new RecipeEntry<>(key, recipe);
	}

	public static <I extends RecipeInput, T extends Recipe<I>> ServerRecipeManager.MatchGetter<I, T> createCachedMatchGetter(
			RecipeType<T> type
	) {
		return new ServerRecipeManager.MatchGetter<I, T>() {
			private @Nullable RegistryKey<Recipe<?>> id;

			@Override
			public Optional<RecipeEntry<T>> getFirstMatch(I input, ServerWorld world) {
				ServerRecipeManager serverRecipeManager = world.getRecipeManager();
				Optional<RecipeEntry<T>> optional = serverRecipeManager.getFirstMatch(type, input, world, this.id);
				if (optional.isPresent()) {
					RecipeEntry<T> recipeEntry = optional.get();
					this.id = recipeEntry.id();
					return Optional.of(recipeEntry);
				}
				else {
					return Optional.empty();
				}
			}
		};
	}

	private static List<ServerRecipeManager.ServerRecipe> collectServerRecipes(
			Iterable<RecipeEntry<?>> recipes,
			FeatureSet enabledFeatures
	) {
		List<ServerRecipeManager.ServerRecipe> list = new ArrayList<>();
		Object2IntMap<String> object2IntMap = new Object2IntOpenHashMap();

		for (RecipeEntry<?> recipeEntry : recipes) {
			Recipe<?> recipe = recipeEntry.value();
			OptionalInt optionalInt;
			if (recipe.getGroup().isEmpty()) {
				optionalInt = OptionalInt.empty();
			}
			else {
				optionalInt =
						OptionalInt.of(object2IntMap.computeIfAbsent(recipe.getGroup(), group -> object2IntMap.size()));
			}

			Optional<List<Ingredient>> optional;
			if (recipe.isIgnoredInRecipeBook()) {
				optional = Optional.empty();
			}
			else {
				optional = Optional.of(recipe.getIngredientPlacement().getIngredients());
			}

			for (RecipeDisplay recipeDisplay : recipe.getDisplays()) {
				if (recipeDisplay.isEnabled(enabledFeatures)) {
					int i = list.size();
					NetworkRecipeId networkRecipeId = new NetworkRecipeId(i);
					RecipeDisplayEntry recipeDisplayEntry = new RecipeDisplayEntry(
							networkRecipeId, recipeDisplay, optionalInt, recipe.getRecipeBookCategory(), optional
					);
					list.add(new ServerRecipeManager.ServerRecipe(recipeDisplayEntry, recipeEntry));
				}
			}
		}

		return list;
	}

	private static ServerRecipeManager.SoleIngredientGetter cookingIngredientGetter(RecipeType<? extends SingleStackRecipe> expectedType) {
		return recipe -> recipe.getType() == expectedType && recipe instanceof SingleStackRecipe singleStackRecipe
		                 ? Optional.of(singleStackRecipe.ingredient())
		                 : Optional.empty();
	}

	/**
	 * {@code MatchGetter}.
	 */
	public interface MatchGetter<I extends RecipeInput, T extends Recipe<I>> {

		Optional<RecipeEntry<T>> getFirstMatch(I input, ServerWorld world);
	}

	/**
	 * {@code PropertySetBuilder}.
	 */
	public static class PropertySetBuilder implements Consumer<Recipe<?>> {

		final RegistryKey<RecipePropertySet> propertySetKey;
		private final ServerRecipeManager.SoleIngredientGetter ingredientGetter;
		private final List<Ingredient> ingredients = new ArrayList<>();

		protected PropertySetBuilder(
				RegistryKey<RecipePropertySet> propertySetKey,
				ServerRecipeManager.SoleIngredientGetter ingredientGetter
		) {
			this.propertySetKey = propertySetKey;
			this.ingredientGetter = ingredientGetter;
		}

		public void accept(Recipe<?> recipe) {
			this.ingredientGetter.apply(recipe).ifPresent(this.ingredients::add);
		}

		public RecipePropertySet build(FeatureSet enabledFeatures) {
			return RecipePropertySet.of(ServerRecipeManager.filterIngredients(enabledFeatures, this.ingredients));
		}
	}

	/**
	 * {@code ServerRecipe}.
	 */
	public record ServerRecipe(RecipeDisplayEntry display, RecipeEntry<?> parent) {
	}

	@FunctionalInterface
	/**
	 * {@code SoleIngredientGetter}.
	 */
	public interface SoleIngredientGetter {

		Optional<Ingredient> apply(Recipe<?> recipe);
	}
}
