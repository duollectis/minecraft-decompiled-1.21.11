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
 * Серверная реализация менеджера рецептов. Загружает рецепты из JSON-ресурсов,
 * строит наборы свойств ингредиентов для каждого типа станка и формирует
 * плоский список {@link ServerRecipe} для сетевой синхронизации с клиентом.
 */
public class ServerRecipeManager
		extends SinglePreparationResourceReloader<PreparedRecipes>
		implements RecipeManager, FabricServerRecipeManager {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Маппинг ключей наборов свойств на геттеры единственного ингредиента рецепта.
	 * Используется при инициализации для заполнения {@link RecipePropertySet} по каждому типу станка.
	 */
	private static final Map<RegistryKey<RecipePropertySet>, SoleIngredientGetter> SOLE_INGREDIENT_GETTERS = Map.of(
			RecipePropertySet.SMITHING_ADDITION,
			recipe -> recipe instanceof SmithingRecipe smithing ? smithing.addition() : Optional.empty(),
			RecipePropertySet.SMITHING_BASE,
			recipe -> recipe instanceof SmithingRecipe smithing ? Optional.of(smithing.base()) : Optional.empty(),
			RecipePropertySet.SMITHING_TEMPLATE,
			recipe -> recipe instanceof SmithingRecipe smithing ? smithing.template() : Optional.empty(),
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
	private CuttingRecipeDisplay.Grouping<StonecuttingRecipe> stonecutterRecipes = CuttingRecipeDisplay.Grouping.empty();
	private List<ServerRecipe> recipes = List.of();
	private Map<RegistryKey<Recipe<?>>, List<ServerRecipe>> recipesByKey = Map.of();

	public ServerRecipeManager(RegistryWrapper.WrapperLookup registries) {
		this.registries = registries;
	}

	@Override
	protected PreparedRecipes prepare(ResourceManager resourceManager, Profiler profiler) {
		SortedMap<Identifier, Recipe<?>> sortedMap = new TreeMap<>();
		JsonDataLoader.load(resourceManager, FINDER, registries.getOps(JsonOps.INSTANCE), Recipe.CODEC, sortedMap);

		List<RecipeEntry<?>> entries = new ArrayList<>(sortedMap.size());
		sortedMap.forEach((id, recipe) -> {
			RegistryKey<Recipe<?>> key = RegistryKey.of(RegistryKeys.RECIPE, id);
			entries.add(new RecipeEntry<>(key, recipe));
		});

		return PreparedRecipes.of(entries);
	}

	@Override
	protected void apply(PreparedRecipes preparedRecipes, ResourceManager resourceManager, Profiler profiler) {
		this.preparedRecipes = preparedRecipes;
		LOGGER.info("Loaded {} recipes", preparedRecipes.recipes().size());
	}

	/**
	 * Инициализирует наборы свойств ингредиентов, список рецептов камнерезного станка
	 * и плоский список {@link ServerRecipe} для сетевой синхронизации.
	 * Вызывается после загрузки фич-флагов мира.
	 *
	 * @param features активный набор фич-флагов
	 */
	public void initialize(FeatureSet features) {
		List<CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe>> stonecutterEntries = new ArrayList<>();
		List<PropertySetBuilder> builders = SOLE_INGREDIENT_GETTERS.entrySet()
				.stream()
				.map(entry -> new PropertySetBuilder(entry.getKey(), entry.getValue()))
				.toList();

		preparedRecipes.recipes().forEach(recipeEntry -> {
			Recipe<?> recipe = recipeEntry.value();
			if (!recipe.isIgnoredInRecipeBook() && recipe.getIngredientPlacement().hasNoPlacement()) {
				LOGGER.warn(
						"Recipe {} can't be placed due to empty ingredients and will be ignored",
						recipeEntry.id().getValue()
				);
				return;
			}

			builders.forEach(builder -> builder.accept(recipe));

			if (recipe instanceof StonecuttingRecipe stonecuttingRecipe
					&& isEnabled(features, stonecuttingRecipe.ingredient())
					&& stonecuttingRecipe.createResultDisplay().isEnabled(features)) {
				stonecutterEntries.add(new CuttingRecipeDisplay.GroupEntry<>(
						stonecuttingRecipe.ingredient(),
						new CuttingRecipeDisplay<>(
								stonecuttingRecipe.createResultDisplay(),
								Optional.of((RecipeEntry<StonecuttingRecipe>) recipeEntry)
						)
				));
			}
		});

		propertySets = builders.stream()
				.collect(Collectors.toUnmodifiableMap(
						builder -> builder.propertySetKey,
						builder -> builder.build(features)
				));
		stonecutterRecipes = new CuttingRecipeDisplay.Grouping<>(stonecutterEntries);
		recipes = collectServerRecipes(preparedRecipes.recipes(), features);
		recipesByKey = recipes.stream()
				.collect(Collectors.groupingBy(
						recipe -> recipe.parent().id(),
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
			RecipeType<T> type, I input, World world, @Nullable RegistryKey<Recipe<?>> recipeKey
	) {
		RecipeEntry<T> cached = recipeKey != null ? get(type, recipeKey) : null;
		return getFirstMatch(type, input, world, cached);
	}

	public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeEntry<T>> getFirstMatch(
			RecipeType<T> type, I input, World world, @Nullable RecipeEntry<T> recipe
	) {
		return recipe != null && recipe.value().matches(input, world)
				? Optional.of(recipe)
				: getFirstMatch(type, input, world);
	}

	public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeEntry<T>> getFirstMatch(
			RecipeType<T> type, I input, World world
	) {
		return preparedRecipes.find(type, input, world).findFirst();
	}

	public Optional<RecipeEntry<?>> get(RegistryKey<Recipe<?>> key) {
		return Optional.ofNullable(preparedRecipes.get(key));
	}

	@SuppressWarnings("unchecked")
	private <T extends Recipe<?>> @Nullable RecipeEntry<T> get(RecipeType<T> type, RegistryKey<Recipe<?>> key) {
		RecipeEntry<?> entry = preparedRecipes.get(key);
		return entry != null && entry.value().getType().equals(type) ? (RecipeEntry<T>) entry : null;
	}

	public Map<RegistryKey<RecipePropertySet>, RecipePropertySet> getPropertySets() {
		return propertySets;
	}

	public CuttingRecipeDisplay.Grouping<StonecuttingRecipe> getStonecutterRecipeForSync() {
		return stonecutterRecipes;
	}

	@Override
	public RecipePropertySet getPropertySet(RegistryKey<RecipePropertySet> key) {
		return propertySets.getOrDefault(key, RecipePropertySet.EMPTY);
	}

	@Override
	public CuttingRecipeDisplay.Grouping<StonecuttingRecipe> getStonecutterRecipes() {
		return stonecutterRecipes;
	}

	public Collection<RecipeEntry<?>> values() {
		return preparedRecipes.recipes();
	}

	public @Nullable ServerRecipe get(NetworkRecipeId id) {
		int index = id.index();
		return index >= 0 && index < recipes.size() ? recipes.get(index) : null;
	}

	public void forEachRecipeDisplay(RegistryKey<Recipe<?>> key, Consumer<RecipeDisplayEntry> action) {
		List<ServerRecipe> matching = recipesByKey.get(key);
		if (matching == null) {
			return;
		}

		matching.forEach(recipe -> action.accept(recipe.display()));
	}

	@VisibleForTesting
	protected static RecipeEntry<?> deserialize(
			RegistryKey<Recipe<?>> key,
			JsonObject json,
			RegistryWrapper.WrapperLookup registries
	) {
		Recipe<?> recipe = Recipe.CODEC
				.parse(registries.getOps(JsonOps.INSTANCE), json)
				.getOrThrow(JsonParseException::new);
		return new RecipeEntry<>(key, recipe);
	}

	/**
	 * Создаёт кешированный {@link MatchGetter}, который запоминает ключ последнего
	 * совпавшего рецепта и при следующем вызове проверяет его первым — до полного перебора.
	 *
	 * @param type тип рецепта для поиска
	 * @return экземпляр {@link MatchGetter} с внутренним кешем последнего рецепта
	 */
	public static <I extends RecipeInput, T extends Recipe<I>> MatchGetter<I, T> createCachedMatchGetter(
			RecipeType<T> type
	) {
		return new MatchGetter<>() {
			private @Nullable RegistryKey<Recipe<?>> lastMatchedKey;

			@Override
			public Optional<RecipeEntry<T>> getFirstMatch(I input, ServerWorld world) {
				ServerRecipeManager manager = world.getRecipeManager();
				Optional<RecipeEntry<T>> match = manager.getFirstMatch(type, input, world, lastMatchedKey);
				if (match.isPresent()) {
					lastMatchedKey = match.get().id();
					return match;
				}

				return Optional.empty();
			}
		};
	}

	private static List<ServerRecipe> collectServerRecipes(
			Iterable<RecipeEntry<?>> recipeEntries,
			FeatureSet enabledFeatures
	) {
		List<ServerRecipe> result = new ArrayList<>();
		Object2IntMap<String> groupIndexMap = new Object2IntOpenHashMap<>();

		for (RecipeEntry<?> recipeEntry : recipeEntries) {
			Recipe<?> recipe = recipeEntry.value();

			OptionalInt groupIndex = recipe.getGroup().isEmpty()
					? OptionalInt.empty()
					: OptionalInt.of(groupIndexMap.computeIfAbsent(recipe.getGroup(), g -> groupIndexMap.size()));

			Optional<List<Ingredient>> ingredients = recipe.isIgnoredInRecipeBook()
					? Optional.empty()
					: Optional.of(recipe.getIngredientPlacement().getIngredients());

			for (RecipeDisplay display : recipe.getDisplays()) {
				if (!display.isEnabled(enabledFeatures)) {
					continue;
				}

				int networkIndex = result.size();
				NetworkRecipeId networkId = new NetworkRecipeId(networkIndex);
				RecipeDisplayEntry displayEntry = new RecipeDisplayEntry(
						networkId, display, groupIndex, recipe.getRecipeBookCategory(), ingredients
				);
				result.add(new ServerRecipe(displayEntry, recipeEntry));
			}
		}

		return result;
	}

	private static SoleIngredientGetter cookingIngredientGetter(RecipeType<? extends SingleStackRecipe> expectedType) {
		return recipe -> recipe.getType() == expectedType && recipe instanceof SingleStackRecipe singleStack
				? Optional.of(singleStack.ingredient())
				: Optional.empty();
	}

	/**
	 * Интерфейс для поиска первого подходящего рецепта по входным данным и миру.
	 * Реализации могут кешировать последний результат для ускорения повторных запросов.
	 */
	public interface MatchGetter<I extends RecipeInput, T extends Recipe<I>> {

		Optional<RecipeEntry<T>> getFirstMatch(I input, ServerWorld world);
	}

	/**
	 * Строитель набора свойств ингредиентов для одного типа станка.
	 * Накапливает ингредиенты из всех рецептов через {@link #accept(Recipe)},
	 * затем фильтрует по фич-флагам и строит {@link RecipePropertySet}.
	 */
	public static class PropertySetBuilder implements Consumer<Recipe<?>> {

		final RegistryKey<RecipePropertySet> propertySetKey;
		private final SoleIngredientGetter ingredientGetter;
		private final List<Ingredient> ingredients = new ArrayList<>();

		protected PropertySetBuilder(
				RegistryKey<RecipePropertySet> propertySetKey,
				SoleIngredientGetter ingredientGetter
		) {
			this.propertySetKey = propertySetKey;
			this.ingredientGetter = ingredientGetter;
		}

		@Override
		public void accept(Recipe<?> recipe) {
			ingredientGetter.apply(recipe).ifPresent(ingredients::add);
		}

		public RecipePropertySet build(FeatureSet enabledFeatures) {
			return RecipePropertySet.of(ServerRecipeManager.filterIngredients(enabledFeatures, ingredients));
		}
	}

	/**
	 * Пара из отображаемого рецепта для клиента и исходного серверного рецепта.
	 */
	public record ServerRecipe(RecipeDisplayEntry display, RecipeEntry<?> parent) {
	}

	/**
	 * Функциональный интерфейс для извлечения единственного ингредиента из рецепта
	 * при построении {@link RecipePropertySet}.
	 */
	@FunctionalInterface
	public interface SoleIngredientGetter {

		Optional<Ingredient> apply(Recipe<?> recipe);
	}
}
