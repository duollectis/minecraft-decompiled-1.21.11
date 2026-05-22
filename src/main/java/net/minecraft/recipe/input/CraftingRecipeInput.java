package net.minecraft.recipe.input;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeFinder;

import java.util.ArrayList;
import java.util.List;

/**
 * Входные данные рецепта крафта: двумерная сетка предметов с шириной и высотой.
 * При создании автоматически обрезает пустые строки и столбцы по краям,
 * возвращая позиционированный результат с координатами смещения в исходной сетке.
 */
public class CraftingRecipeInput implements RecipeInput {

	public static final CraftingRecipeInput EMPTY = new CraftingRecipeInput(0, 0, List.of());

	private final int width;
	private final int height;
	private final List<ItemStack> stacks;
	private final RecipeFinder matcher = new RecipeFinder();
	private final int stackCount;

	private CraftingRecipeInput(int width, int height, List<ItemStack> stacks) {
		this.width = width;
		this.height = height;
		this.stacks = stacks;

		int count = 0;

		for (ItemStack stack : stacks) {
			if (!stack.isEmpty()) {
				count++;
				matcher.addInput(stack, 1);
			}
		}

		stackCount = count;
	}

	public static CraftingRecipeInput create(int width, int height, List<ItemStack> stacks) {
		return createPositioned(width, height, stacks).input();
	}

	/**
	 * Создаёт позиционированный ввод, обрезая пустые строки и столбцы по краям сетки.
	 * Возвращает смещение {@code left}/{@code top} — позицию верхнего левого угла
	 * обрезанной области в исходной сетке, что необходимо для корректного выравнивания
	 * рецепта при заполнении слотов крафта.
	 *
	 * @param width  ширина исходной сетки
	 * @param height высота исходной сетки
	 * @param stacks список стеков в порядке строк (row-major)
	 * @return позиционированный ввод с обрезанными границами
	 */
	public static CraftingRecipeInput.Positioned createPositioned(int width, int height, List<ItemStack> stacks) {
		if (width == 0 || height == 0) {
			return CraftingRecipeInput.Positioned.EMPTY;
		}

		int minCol = width - 1;
		int maxCol = 0;
		int minRow = height - 1;
		int maxRow = 0;

		for (int row = 0; row < height; row++) {
			boolean rowEmpty = true;

			for (int col = 0; col < width; col++) {
				ItemStack stack = stacks.get(col + row * width);

				if (!stack.isEmpty()) {
					minCol = Math.min(minCol, col);
					maxCol = Math.max(maxCol, col);
					rowEmpty = false;
				}
			}

			if (!rowEmpty) {
				minRow = Math.min(minRow, row);
				maxRow = Math.max(maxRow, row);
			}
		}

		int trimmedWidth = maxCol - minCol + 1;
		int trimmedHeight = maxRow - minRow + 1;

		if (trimmedWidth <= 0 || trimmedHeight <= 0) {
			return CraftingRecipeInput.Positioned.EMPTY;
		}

		if (trimmedWidth == width && trimmedHeight == height) {
			return new CraftingRecipeInput.Positioned(new CraftingRecipeInput(width, height, stacks), minCol, minRow);
		}

		List<ItemStack> trimmed = new ArrayList<>(trimmedWidth * trimmedHeight);

		for (int row = 0; row < trimmedHeight; row++) {
			for (int col = 0; col < trimmedWidth; col++) {
				trimmed.add(stacks.get(col + minCol + (row + minRow) * width));
			}
		}

		return new CraftingRecipeInput.Positioned(new CraftingRecipeInput(trimmedWidth, trimmedHeight, trimmed), minCol, minRow);
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return stacks.get(slot);
	}

	public ItemStack getStackInSlot(int x, int y) {
		return stacks.get(x + y * width);
	}

	@Override
	public int size() {
		return stacks.size();
	}

	@Override
	public boolean isEmpty() {
		return stackCount == 0;
	}

	public RecipeFinder getRecipeMatcher() {
		return matcher;
	}

	public List<ItemStack> getStacks() {
		return stacks;
	}

	public int getStackCount() {
		return stackCount;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof CraftingRecipeInput other)) {
			return false;
		}

		return width == other.width
				&& height == other.height
				&& stackCount == other.stackCount
				&& ItemStack.stacksEqual(stacks, other.stacks);
	}

	@Override
	public int hashCode() {
		int hash = ItemStack.listHashCode(stacks);
		hash = 31 * hash + width;
		return 31 * hash + height;
	}

	/**
	 * Позиционированный ввод крафта: содержит обрезанную сетку и смещение
	 * её верхнего левого угла относительно исходной сетки крафтового стола.
	 */
	public record Positioned(CraftingRecipeInput input, int left, int top) {

		public static final CraftingRecipeInput.Positioned EMPTY =
				new CraftingRecipeInput.Positioned(CraftingRecipeInput.EMPTY, 0, 0);
	}
}
