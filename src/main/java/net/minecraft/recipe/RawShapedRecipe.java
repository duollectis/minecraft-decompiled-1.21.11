package net.minecraft.recipe;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Низкоуровневое представление форменного рецепта крафта.
 * <p>
 * Хранит нормализованный (без отступов) паттерн в виде плоского списка
 * {@link Optional}{@code <Ingredient>} размером {@code width × height}.
 * Поддерживает зеркальное отражение по горизонтали: если паттерн симметричен,
 * зеркальная проверка пропускается для экономии времени.
 */
public final class RawShapedRecipe {

	private static final int MAX_GRID_SIZE = 3;
	public static final char SPACE = ' ';

	public static final MapCodec<RawShapedRecipe> CODEC = RawShapedRecipe.Data.CODEC
		.flatXmap(
			RawShapedRecipe::fromData,
			recipe -> recipe.data
				.<DataResult<RawShapedRecipe.Data>>map(DataResult::success)
				.orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe"))
		);
	public static final PacketCodec<RegistryByteBuf, RawShapedRecipe> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT, recipe -> recipe.width,
		PacketCodecs.VAR_INT, recipe -> recipe.height,
		Ingredient.OPTIONAL_PACKET_CODEC.collect(PacketCodecs.toList()), recipe -> recipe.ingredients,
		RawShapedRecipe::create
	);

	private final int width;
	private final int height;
	private final List<Optional<Ingredient>> ingredients;
	private final Optional<RawShapedRecipe.Data> data;
	private final int ingredientCount;
	private final boolean symmetrical;

	public RawShapedRecipe(
		int width,
		int height,
		List<Optional<Ingredient>> ingredients,
		Optional<RawShapedRecipe.Data> data
	) {
		this.width = width;
		this.height = height;
		this.ingredients = ingredients;
		this.data = data;
		this.ingredientCount = (int) ingredients.stream().flatMap(Optional::stream).count();
		this.symmetrical = Util.isSymmetrical(width, height, ingredients);
	}

	private static RawShapedRecipe create(Integer width, Integer height, List<Optional<Ingredient>> ingredients) {
		return new RawShapedRecipe(width, height, ingredients, Optional.empty());
	}

	public static RawShapedRecipe create(Map<Character, Ingredient> key, String... pattern) {
		return create(key, List.of(pattern));
	}

	/**
	 * Создаёт {@link RawShapedRecipe} из карты символов и строкового паттерна.
	 * Паттерн обрезается от пустых строк и пробелов по краям.
	 *
	 * @param key     карта символов паттерна на ингредиенты
	 * @param pattern строки паттерна (максимум 3×3)
	 * @return готовый рецепт
	 * @throws IllegalStateException если паттерн содержит неопределённые символы
	 *                               или ключ содержит неиспользуемые символы
	 */
	public static RawShapedRecipe create(Map<Character, Ingredient> key, List<String> pattern) {
		RawShapedRecipe.Data data = new RawShapedRecipe.Data(key, pattern);
		return fromData(data).getOrThrow();
	}

	private static DataResult<RawShapedRecipe> fromData(RawShapedRecipe.Data data) {
		String[] trimmedPattern = removePadding(data.pattern);
		int width = trimmedPattern[0].length();
		int height = trimmedPattern.length;
		List<Optional<Ingredient>> ingredients = new ArrayList<>(width * height);
		CharSet unusedKeys = new CharArraySet(data.key.keySet());

		for (String row : trimmedPattern) {
			for (int col = 0; col < row.length(); col++) {
				char symbol = row.charAt(col);

				if (symbol == SPACE) {
					ingredients.add(Optional.empty());
				} else {
					Ingredient ingredient = data.key.get(symbol);

					if (ingredient == null) {
						return DataResult.error(
							() -> "Pattern references symbol '" + symbol + "' but it's not defined in the key"
						);
					}

					ingredients.add(Optional.of(ingredient));
				}

				unusedKeys.remove(symbol);
			}
		}

		return unusedKeys.isEmpty()
			? DataResult.success(new RawShapedRecipe(width, height, ingredients, Optional.of(data)))
			: DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + unusedKeys);
	}

	@VisibleForTesting
	static String[] removePadding(List<String> pattern) {
		int firstSymbolCol = Integer.MAX_VALUE;
		int lastSymbolCol = 0;
		int emptyRowsAtTop = 0;
		int emptyRowsAtBottom = 0;

		for (int rowIndex = 0; rowIndex < pattern.size(); rowIndex++) {
			String row = pattern.get(rowIndex);
			firstSymbolCol = Math.min(firstSymbolCol, findFirstSymbol(row));
			int lastCol = findLastSymbol(row);
			lastSymbolCol = Math.max(lastSymbolCol, lastCol);

			if (lastCol < 0) {
				if (emptyRowsAtTop == rowIndex) {
					emptyRowsAtTop++;
				}

				emptyRowsAtBottom++;
			} else {
				emptyRowsAtBottom = 0;
			}
		}

		if (pattern.size() == emptyRowsAtBottom) {
			return new String[0];
		}

		String[] trimmed = new String[pattern.size() - emptyRowsAtBottom - emptyRowsAtTop];

		for (int i = 0; i < trimmed.length; i++) {
			trimmed[i] = pattern.get(i + emptyRowsAtTop).substring(firstSymbolCol, lastSymbolCol + 1);
		}

		return trimmed;
	}

	private static int findFirstSymbol(String line) {
		int index = 0;

		while (index < line.length() && line.charAt(index) == SPACE) {
			index++;
		}

		return index;
	}

	private static int findLastSymbol(String line) {
		int index = line.length() - 1;

		while (index >= 0 && line.charAt(index) == SPACE) {
			index--;
		}

		return index;
	}

	/**
	 * Проверяет соответствие входной сетки крафта данному паттерну.
	 * Сначала проверяется зеркальное отражение (если паттерн несимметричен),
	 * затем — прямое.
	 */
	public boolean matches(CraftingRecipeInput input) {
		if (input.getStackCount() != ingredientCount) {
			return false;
		}

		if (input.getWidth() != width || input.getHeight() != height) {
			return false;
		}

		if (!symmetrical && matches(input, true)) {
			return true;
		}

		return matches(input, false);
	}

	private boolean matches(CraftingRecipeInput input, boolean mirrored) {
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				int ingredientIndex = mirrored
					? width - col - 1 + row * width
					: col + row * width;

				Optional<Ingredient> ingredient = ingredients.get(ingredientIndex);
				ItemStack stack = input.getStackInSlot(col, row);

				if (!Ingredient.matches(ingredient, stack)) {
					return false;
				}
			}
		}

		return true;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public List<Optional<Ingredient>> getIngredients() {
		return ingredients;
	}

	/**
	 * Сырые данные форменного рецепта в формате JSON-датапака:
	 * карта символов и строковый паттерн до нормализации.
	 */
	public record Data(Map<Character, Ingredient> key, List<String> pattern) {

		private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(
			pattern -> {
				if (pattern.size() > MAX_GRID_SIZE) {
					return DataResult.error(() -> "Invalid pattern: too many rows, 3 is maximum");
				}

				if (pattern.isEmpty()) {
					return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
				}

				int firstRowLength = pattern.getFirst().length();

				for (String row : pattern) {
					if (row.length() > MAX_GRID_SIZE) {
						return DataResult.error(() -> "Invalid pattern: too many columns, 3 is maximum");
					}

					if (firstRowLength != row.length()) {
						return DataResult.error(() -> "Invalid pattern: each row must be the same width");
					}
				}

				return DataResult.success(pattern);
			},
			Function.identity()
		);

		private static final Codec<Character> KEY_ENTRY_CODEC = Codec.STRING.comapFlatMap(
			keyEntry -> {
				if (keyEntry.length() != 1) {
					return DataResult.error(
						() -> "Invalid key entry: '" + keyEntry + "' is an invalid symbol (must be 1 character only)."
					);
				}

				return " ".equals(keyEntry)
					? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.")
					: DataResult.success(keyEntry.charAt(0));
			},
			String::valueOf
		);

		public static final MapCodec<RawShapedRecipe.Data> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Codecs.strictUnboundedMap(KEY_ENTRY_CODEC, Ingredient.CODEC).fieldOf("key").forGetter(Data::key),
				PATTERN_CODEC.fieldOf("pattern").forGetter(Data::pattern)
			).apply(instance, RawShapedRecipe.Data::new)
		);
	}
}
