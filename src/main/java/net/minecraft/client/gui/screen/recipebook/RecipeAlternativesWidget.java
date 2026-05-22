package net.minecraft.client.gui.screen.recipebook;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.RecipeGridAligner;
import net.minecraft.recipe.display.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Всплывающий виджет альтернативных рецептов, отображаемый при правом клике
 * на кнопку результата с несколькими вариантами крафта.
 */
@Environment(EnvType.CLIENT)
public class RecipeAlternativesWidget implements Drawable, Element {

	private static final Identifier OVERLAY_RECIPE_TEXTURE = Identifier.ofVanilla("recipe_book/overlay_recipe");
	private static final int PADDING = 4;
	private static final int ROW_PADDING = 5;
	private static final float SCALE = 0.375F;
	public static final int BUTTON_WIDTH = 25;
	private static final int MAX_COLUMNS_SMALL = 4;
	private static final int MAX_COLUMNS_LARGE = 5;
	private static final int SMALL_GRID_THRESHOLD = 16;

	private final List<RecipeAlternativesWidget.AlternativeButtonWidget> alternativeButtons = Lists.newArrayList();
	private boolean visible;
	private int buttonX;
	private int buttonY;
	private RecipeResultCollection resultCollection = RecipeResultCollection.EMPTY;
	private @Nullable NetworkRecipeId lastClickedRecipe;
	final CurrentIndexProvider currentIndexProvider;
	private final boolean furnace;

	public RecipeAlternativesWidget(CurrentIndexProvider currentIndexProvider, boolean furnace) {
		this.currentIndexProvider = currentIndexProvider;
		this.furnace = furnace;
	}

	/**
	 * Показывает альтернативные рецепты для указанной коллекции результатов.
	 * Автоматически корректирует позицию виджета, чтобы он не выходил за границы экрана.
	 */
	public void showAlternativesForResult(
			RecipeResultCollection resultCollection,
			ContextParameterMap context,
			boolean filteringCraftable,
			int buttonX,
			int buttonY,
			int areaCenterX,
			int areaCenterY,
			float delta
	) {
		this.resultCollection = resultCollection;

		List<RecipeDisplayEntry> craftable = resultCollection.filter(RecipeResultCollection.RecipeFilterMode.CRAFTABLE);
		List<RecipeDisplayEntry> uncraftable = filteringCraftable
				? Collections.emptyList()
				: resultCollection.filter(RecipeResultCollection.RecipeFilterMode.NOT_CRAFTABLE);

		int craftableCount = craftable.size();
		int totalCount = craftableCount + uncraftable.size();
		int columnCount = totalCount <= SMALL_GRID_THRESHOLD ? MAX_COLUMNS_SMALL : MAX_COLUMNS_LARGE;
		int rowCount = (int) Math.ceil((float) totalCount / columnCount);

		this.buttonX = buttonX;
		this.buttonY = buttonY;

		float rightEdge = this.buttonX + Math.min(totalCount, columnCount) * BUTTON_WIDTH;
		float rightBound = areaCenterX + 50;

		if (rightEdge > rightBound) {
			this.buttonX = (int) (this.buttonX - delta * (int) ((rightEdge - rightBound) / delta));
		}

		float bottomEdge = this.buttonY + rowCount * BUTTON_WIDTH;
		float bottomBound = areaCenterY + 50;

		if (bottomEdge > bottomBound) {
			this.buttonY = (int) (this.buttonY - delta * MathHelper.ceil((bottomEdge - bottomBound) / delta));
		}

		float topEdge = this.buttonY;
		float topBound = areaCenterY - 100;

		if (topEdge < topBound) {
			this.buttonY = (int) (this.buttonY - delta * MathHelper.ceil((topEdge - topBound) / delta));
		}

		visible = true;
		alternativeButtons.clear();

		for (int index = 0; index < totalCount; index++) {
			boolean isCraftable = index < craftableCount;
			RecipeDisplayEntry entry = isCraftable ? craftable.get(index) : uncraftable.get(index - craftableCount);
			int entryX = this.buttonX + PADDING + BUTTON_WIDTH * (index % columnCount);
			int entryY = this.buttonY + ROW_PADDING + BUTTON_WIDTH * (index / columnCount);

			if (furnace) {
				alternativeButtons.add(new RecipeAlternativesWidget.FurnaceAlternativeButtonWidget(
						entryX, entryY, entry.id(), entry.display(), context, isCraftable
				));
			} else {
				alternativeButtons.add(new RecipeAlternativesWidget.CraftingAlternativeButtonWidget(
						entryX, entryY, entry.id(), entry.display(), context, isCraftable
				));
			}
		}

		lastClickedRecipe = null;
	}

	public RecipeResultCollection getResults() {
		return resultCollection;
	}

	public @Nullable NetworkRecipeId getLastClickedRecipe() {
		return lastClickedRecipe;
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() != 0) {
			return false;
		}

		for (RecipeAlternativesWidget.AlternativeButtonWidget button : alternativeButtons) {
			if (button.mouseClicked(click, doubled)) {
				lastClickedRecipe = button.recipeId;
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (!visible) {
			return;
		}

		int columnCount = alternativeButtons.size() <= SMALL_GRID_THRESHOLD ? MAX_COLUMNS_SMALL : MAX_COLUMNS_LARGE;
		int visibleColumns = Math.min(alternativeButtons.size(), columnCount);
		int rowCount = MathHelper.ceil((float) alternativeButtons.size() / columnCount);

		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				OVERLAY_RECIPE_TEXTURE,
				buttonX,
				buttonY,
				visibleColumns * BUTTON_WIDTH + 8,
				rowCount * BUTTON_WIDTH + 8
		);

		for (RecipeAlternativesWidget.AlternativeButtonWidget button : alternativeButtons) {
			button.render(context, mouseX, mouseY, deltaTicks);
		}
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isVisible() {
		return visible;
	}

	@Override
	public void setFocused(boolean focused) {
	}

	@Override
	public boolean isFocused() {
		return false;
	}

	@Environment(EnvType.CLIENT)
	abstract class AlternativeButtonWidget extends ClickableWidget {

		final NetworkRecipeId recipeId;
		private final boolean craftable;
		private final List<RecipeAlternativesWidget.AlternativeButtonWidget.InputSlot> inputSlots;

		public AlternativeButtonWidget(
				final int x,
				final int y,
				final NetworkRecipeId recipeId,
				final boolean craftable,
				final List<RecipeAlternativesWidget.AlternativeButtonWidget.InputSlot> inputSlots
		) {
			super(x, y, 24, 24, ScreenTexts.EMPTY);
			this.inputSlots = inputSlots;
			this.recipeId = recipeId;
			this.craftable = craftable;
		}

		protected static RecipeAlternativesWidget.AlternativeButtonWidget.InputSlot slot(
				int x,
				int y,
				List<ItemStack> stacks
		) {
			return new RecipeAlternativesWidget.AlternativeButtonWidget.InputSlot(3 + x * 7, 3 + y * 7, stacks);
		}

		protected abstract Identifier getOverlayTexture(boolean enabled);

		@Override
		public void appendClickableNarrations(NarrationMessageBuilder builder) {
			appendDefaultNarrations(builder);
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					getOverlayTexture(craftable),
					getX(),
					getY(),
					width,
					height
			);

			float originX = getX() + 2;
			float originY = getY() + 2;

			for (RecipeAlternativesWidget.AlternativeButtonWidget.InputSlot inputSlot : inputSlots) {
				context.getMatrices().pushMatrix();
				context.getMatrices().translate(originX + inputSlot.y, originY + inputSlot.x);
				context.getMatrices().scale(SCALE, SCALE);
				context.getMatrices().translate(-8.0F, -8.0F);
				context.drawItem(
						inputSlot.get(RecipeAlternativesWidget.this.currentIndexProvider.currentIndex()),
						0,
						0
				);
				context.getMatrices().popMatrix();
			}
		}

		@Environment(EnvType.CLIENT)
		protected record InputSlot(int y, int x, List<ItemStack> stacks) {

			public InputSlot(int y, int x, List<ItemStack> stacks) {
				if (stacks.isEmpty()) {
					throw new IllegalArgumentException("Ingredient list must be non-empty");
				}

				this.y = y;
				this.x = x;
				this.stacks = stacks;
			}

			public ItemStack get(int index) {
				return stacks.get(index % stacks.size());
			}
		}
	}

	@Environment(EnvType.CLIENT)
	class CraftingAlternativeButtonWidget extends RecipeAlternativesWidget.AlternativeButtonWidget {

		private static final Identifier CRAFTING_OVERLAY =
				Identifier.ofVanilla("recipe_book/crafting_overlay");
		private static final Identifier CRAFTING_OVERLAY_HIGHLIGHTED =
				Identifier.ofVanilla("recipe_book/crafting_overlay_highlighted");
		private static final Identifier CRAFTING_OVERLAY_DISABLED =
				Identifier.ofVanilla("recipe_book/crafting_overlay_disabled");
		private static final Identifier CRAFTING_OVERLAY_DISABLED_HIGHLIGHTED =
				Identifier.ofVanilla("recipe_book/crafting_overlay_disabled_highlighted");

		public CraftingAlternativeButtonWidget(
				final int x,
				final int y,
				final NetworkRecipeId recipeId,
				final RecipeDisplay display,
				final ContextParameterMap context,
				final boolean craftable
		) {
			super(x, y, recipeId, craftable, collectInputSlots(display, context));
		}

		private static List<RecipeAlternativesWidget.AlternativeButtonWidget.InputSlot> collectInputSlots(
				RecipeDisplay display,
				ContextParameterMap context
		) {
			List<RecipeAlternativesWidget.AlternativeButtonWidget.InputSlot> slots = new ArrayList<>();

			switch (display) {
				case ShapedCraftingRecipeDisplay shaped:
					RecipeGridAligner.alignRecipeToGrid(
							3,
							3,
							shaped.width(),
							shaped.height(),
							shaped.ingredients(),
							(slotDisplay, index, x, y) -> {
								List<ItemStack> stacks = slotDisplay.getStacks(context);
								if (!stacks.isEmpty()) {
									slots.add(slot(x, y, stacks));
								}
							}
					);
					break;
				case ShapelessCraftingRecipeDisplay shapeless:
					List<SlotDisplay> ingredients = shapeless.ingredients();
					for (int index = 0; index < ingredients.size(); index++) {
						List<ItemStack> stacks = ingredients.get(index).getStacks(context);
						if (!stacks.isEmpty()) {
							slots.add(slot(index % 3, index / 3, stacks));
						}
					}
					break;
				default:
			}

			return slots;
		}

		@Override
		protected Identifier getOverlayTexture(boolean enabled) {
			if (enabled) {
				return isSelected() ? CRAFTING_OVERLAY_HIGHLIGHTED : CRAFTING_OVERLAY;
			}

			return isSelected() ? CRAFTING_OVERLAY_DISABLED_HIGHLIGHTED : CRAFTING_OVERLAY_DISABLED;
		}
	}

	@Environment(EnvType.CLIENT)
	class FurnaceAlternativeButtonWidget extends RecipeAlternativesWidget.AlternativeButtonWidget {

		private static final Identifier FURNACE_OVERLAY =
				Identifier.ofVanilla("recipe_book/furnace_overlay");
		private static final Identifier FURNACE_OVERLAY_HIGHLIGHTED =
				Identifier.ofVanilla("recipe_book/furnace_overlay_highlighted");
		private static final Identifier FURNACE_OVERLAY_DISABLED =
				Identifier.ofVanilla("recipe_book/furnace_overlay_disabled");
		private static final Identifier FURNACE_OVERLAY_DISABLED_HIGHLIGHTED =
				Identifier.ofVanilla("recipe_book/furnace_overlay_disabled_highlighted");

		public FurnaceAlternativeButtonWidget(
				final int x,
				final int y,
				final NetworkRecipeId recipeId,
				final RecipeDisplay display,
				final ContextParameterMap context,
				final boolean craftable
		) {
			super(x, y, recipeId, craftable, alignRecipe(display, context));
		}

		private static List<RecipeAlternativesWidget.AlternativeButtonWidget.InputSlot> alignRecipe(
				RecipeDisplay display,
				ContextParameterMap context
		) {
			if (display instanceof FurnaceRecipeDisplay furnaceDisplay) {
				List<ItemStack> stacks = furnaceDisplay.ingredient().getStacks(context);
				if (!stacks.isEmpty()) {
					return List.of(slot(1, 1, stacks));
				}
			}

			return List.of();
		}

		@Override
		protected Identifier getOverlayTexture(boolean enabled) {
			if (enabled) {
				return isSelected() ? FURNACE_OVERLAY_HIGHLIGHTED : FURNACE_OVERLAY;
			}

			return isSelected() ? FURNACE_OVERLAY_DISABLED_HIGHLIGHTED : FURNACE_OVERLAY_DISABLED;
		}
	}
}
