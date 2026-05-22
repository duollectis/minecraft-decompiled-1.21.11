package net.minecraft.recipe.display;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;

import java.util.List;
import java.util.Optional;

/**
 * Отображение одного варианта рецепта режущего станка (камнерез, пилорама и т.д.)
 * для клиентского интерфейса. Содержит отображение слота результата и опциональную
 * ссылку на серверный {@link RecipeEntry} для проверки доступности рецепта.
 *
 * @param <T> тип рецепта
 */
public record CuttingRecipeDisplay<T extends Recipe<?>>(SlotDisplay optionDisplay, Optional<RecipeEntry<T>> recipe) {

	public static <T extends Recipe<?>> PacketCodec<RegistryByteBuf, CuttingRecipeDisplay<T>> codec() {
		return PacketCodec.tuple(
				SlotDisplay.PACKET_CODEC,
				CuttingRecipeDisplay::optionDisplay,
				display -> new CuttingRecipeDisplay<>(display, Optional.empty())
		);
	}

	/**
	 * Пара «ингредиент → вариант результата» для группировки рецептов по входному материалу.
	 *
	 * @param <T> тип рецепта
	 */
	public record GroupEntry<T extends Recipe<?>>(Ingredient input, CuttingRecipeDisplay<T> recipe) {

		public static <T extends Recipe<?>> PacketCodec<RegistryByteBuf, CuttingRecipeDisplay.GroupEntry<T>> codec() {
			return PacketCodec.tuple(
					Ingredient.PACKET_CODEC,
					CuttingRecipeDisplay.GroupEntry::input,
					CuttingRecipeDisplay.codec(),
					CuttingRecipeDisplay.GroupEntry::recipe,
					CuttingRecipeDisplay.GroupEntry::new
			);
		}
	}

	/**
	 * Группа всех вариантов результатов для одного входного материала.
	 * Используется для отображения списка доступных рецептов в интерфейсе станка.
	 *
	 * @param <T> тип рецепта
	 */
	public record Grouping<T extends Recipe<?>>(List<CuttingRecipeDisplay.GroupEntry<T>> entries) {

		public static <T extends Recipe<?>> CuttingRecipeDisplay.Grouping<T> empty() {
			return new CuttingRecipeDisplay.Grouping<>(List.of());
		}

		public static <T extends Recipe<?>> PacketCodec<RegistryByteBuf, CuttingRecipeDisplay.Grouping<T>> codec() {
			return PacketCodec.tuple(
					CuttingRecipeDisplay.GroupEntry.<T>codec().collect(PacketCodecs.toList()),
					CuttingRecipeDisplay.Grouping::entries,
					CuttingRecipeDisplay.Grouping::new
			);
		}

		public boolean contains(ItemStack stack) {
			return entries.stream().anyMatch(entry -> entry.input().test(stack));
		}

		/**
		 * Фильтрует группу, оставляя только варианты, принимающие данный стек как входной материал.
		 *
		 * @param stack стек для проверки совместимости
		 * @return новая группа с отфильтрованными вариантами
		 */
		public CuttingRecipeDisplay.Grouping<T> filter(ItemStack stack) {
			return new CuttingRecipeDisplay.Grouping<>(
					entries.stream()
							.filter(entry -> entry.input().test(stack))
							.toList()
			);
		}

		public boolean isEmpty() {
			return entries.isEmpty();
		}

		public int size() {
			return entries.size();
		}
	}
}
