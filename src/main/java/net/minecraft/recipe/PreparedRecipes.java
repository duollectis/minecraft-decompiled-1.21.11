package net.minecraft.recipe;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Иммутабельный снимок загруженных рецептов, индексированных по типу и ключу реестра.
 * Создаётся при загрузке ресурсов и передаётся в {@link ServerRecipeManager}.
 */
public class PreparedRecipes {

	public static final PreparedRecipes EMPTY = new PreparedRecipes(ImmutableMultimap.of(), Map.of());

	private final Multimap<RecipeType<?>, RecipeEntry<?>> byType;
	private final Map<RegistryKey<Recipe<?>>, RecipeEntry<?>> byKey;

	private PreparedRecipes(
		Multimap<RecipeType<?>, RecipeEntry<?>> byType,
		Map<RegistryKey<Recipe<?>>, RecipeEntry<?>> byKey
	) {
		this.byType = byType;
		this.byKey = byKey;
	}

	public static PreparedRecipes of(Iterable<RecipeEntry<?>> recipes) {
		ImmutableMultimap.Builder<RecipeType<?>, RecipeEntry<?>> byTypeBuilder = ImmutableMultimap.builder();
		ImmutableMap.Builder<RegistryKey<Recipe<?>>, RecipeEntry<?>> byKeyBuilder = ImmutableMap.builder();

		for (RecipeEntry<?> entry : recipes) {
			byTypeBuilder.put(entry.value().getType(), entry);
			byKeyBuilder.put(entry.id(), entry);
		}

		return new PreparedRecipes(byTypeBuilder.build(), byKeyBuilder.build());
	}

	@SuppressWarnings("unchecked")
	public <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeEntry<T>> getAll(RecipeType<T> type) {
		return (Collection<RecipeEntry<T>>) (Collection<?>) byType.get(type);
	}

	public Collection<RecipeEntry<?>> recipes() {
		return byKey.values();
	}

	public @Nullable RecipeEntry<?> get(RegistryKey<Recipe<?>> key) {
		return byKey.get(key);
	}

	public <I extends RecipeInput, T extends Recipe<I>> Stream<RecipeEntry<T>> find(
		RecipeType<T> type,
		I input,
		World world
	) {
		return input.isEmpty()
			? Stream.empty()
			: getAll(type).stream().filter(entry -> entry.value().matches(input, world));
	}
}
