package net.minecraft.client.gui.screen.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Область результатов книги рецептов: управляет сеткой кнопок результатов,
 * пагинацией страниц и виджетом альтернативных рецептов.
 */
@Environment(EnvType.CLIENT)
public class RecipeBookResults {

	public static final int RESULTS_PER_PAGE = 20;
	private static final ButtonTextures PAGE_FORWARD_TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("recipe_book/page_forward"),
			Identifier.ofVanilla("recipe_book/page_forward_highlighted")
	);
	private static final ButtonTextures PAGE_BACKWARD_TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("recipe_book/page_backward"),
			Identifier.ofVanilla("recipe_book/page_backward_highlighted")
	);
	private static final Text NEXT_PAGE_TOOLTIP = Text.translatable("gui.recipebook.next_page");
	private static final Text PREVIOUS_PAGE_TOOLTIP = Text.translatable("gui.recipebook.previous_page");
	private static final int GRID_COLUMNS = 12;
	private static final int GRID_ROWS = 17;
	private static final int BUTTON_COLUMN_COUNT = 5;
	private static final int BUTTON_SPACING = 25;
	private static final int BUTTON_GRID_OFFSET_X = 11;
	private static final int BUTTON_GRID_OFFSET_Y = 31;
	private static final int NEXT_PAGE_OFFSET_X = 93;
	private static final int PREV_PAGE_OFFSET_X = 38;
	private static final int PAGE_BUTTONS_OFFSET_Y = 137;

	private final List<AnimatedResultButton> resultButtons = Lists.newArrayListWithCapacity(RESULTS_PER_PAGE);
	private @Nullable AnimatedResultButton hoveredResultButton;
	private final RecipeAlternativesWidget alternatesWidget;
	private MinecraftClient client;
	private final RecipeBookWidget<?> recipeBookWidget;
	private List<RecipeResultCollection> resultCollections = ImmutableList.of();
	private @Nullable TexturedButtonWidget nextPageButton;
	private @Nullable TexturedButtonWidget prevPageButton;
	private int pageCount;
	private int currentPage;
	private ClientRecipeBook recipeBook;
	private @Nullable NetworkRecipeId lastClickedRecipe;
	private @Nullable RecipeResultCollection resultCollection;
	private boolean filteringCraftable;

	public RecipeBookResults(
			RecipeBookWidget<?> recipeBookWidget,
			CurrentIndexProvider currentIndexProvider,
			boolean furnace
	) {
		this.recipeBookWidget = recipeBookWidget;
		alternatesWidget = new RecipeAlternativesWidget(currentIndexProvider, furnace);

		for (int index = 0; index < RESULTS_PER_PAGE; index++) {
			resultButtons.add(new AnimatedResultButton(currentIndexProvider));
		}
	}

	/**
	 * Инициализирует позиции кнопок результатов и кнопок пагинации относительно родительского виджета.
	 */
	public void initialize(MinecraftClient client, int parentLeft, int parentTop) {
		this.client = client;
		recipeBook = client.player.getRecipeBook();

		for (int index = 0; index < resultButtons.size(); index++) {
			resultButtons.get(index).setPosition(
					parentLeft + BUTTON_GRID_OFFSET_X + BUTTON_SPACING * (index % BUTTON_COLUMN_COUNT),
					parentTop + BUTTON_GRID_OFFSET_Y + BUTTON_SPACING * (index / BUTTON_COLUMN_COUNT)
			);
		}

		nextPageButton = new TexturedButtonWidget(
				parentLeft + NEXT_PAGE_OFFSET_X,
				parentTop + PAGE_BUTTONS_OFFSET_Y,
				GRID_COLUMNS,
				GRID_ROWS,
				PAGE_FORWARD_TEXTURES,
				buttonWidget -> hideShowPageButtons(),
				NEXT_PAGE_TOOLTIP
		);
		nextPageButton.setTooltip(Tooltip.of(NEXT_PAGE_TOOLTIP));

		prevPageButton = new TexturedButtonWidget(
				parentLeft + PREV_PAGE_OFFSET_X,
				parentTop + PAGE_BUTTONS_OFFSET_Y,
				GRID_COLUMNS,
				GRID_ROWS,
				PAGE_BACKWARD_TEXTURES,
				buttonWidget -> hideShowPageButtons(),
				PREVIOUS_PAGE_TOOLTIP
		);
		prevPageButton.setTooltip(Tooltip.of(PREVIOUS_PAGE_TOOLTIP));
	}

	public void setResults(
			List<RecipeResultCollection> resultCollections,
			boolean resetCurrentPage,
			boolean filteringCraftable
	) {
		this.resultCollections = resultCollections;
		this.filteringCraftable = filteringCraftable;
		pageCount = (int) Math.ceil(resultCollections.size() / 20.0);

		if (pageCount <= currentPage || resetCurrentPage) {
			currentPage = 0;
		}

		refreshResultButtons();
	}

	private void refreshResultButtons() {
		int pageOffset = RESULTS_PER_PAGE * currentPage;
		ContextParameterMap context = SlotDisplayContexts.createParameters(client.world);

		for (int index = 0; index < resultButtons.size(); index++) {
			AnimatedResultButton button = resultButtons.get(index);

			if (pageOffset + index < resultCollections.size()) {
				button.showResultCollection(
						resultCollections.get(pageOffset + index),
						filteringCraftable,
						this,
						context
				);
				button.visible = true;
			} else {
				button.visible = false;
			}
		}

		hideShowPageButtons();
	}

	private void hideShowPageButtons() {
		if (nextPageButton != null) {
			nextPageButton.visible = pageCount > 1 && currentPage < pageCount - 1;
		}

		if (prevPageButton != null) {
			prevPageButton.visible = pageCount > 1 && currentPage > 0;
		}
	}

	public void draw(DrawContext context, int x, int y, int mouseX, int mouseY, float deltaTicks) {
		if (pageCount > 1) {
			Text pageText = Text.translatable("gui.recipebook.page", currentPage + 1, pageCount);
			int textWidth = client.textRenderer.getWidth(pageText);
			context.drawTextWithShadow(client.textRenderer, pageText, x - textWidth / 2 + 73, y + 141, -1);
		}

		hoveredResultButton = null;

		for (AnimatedResultButton button : resultButtons) {
			button.render(context, mouseX, mouseY, deltaTicks);

			if (button.visible && button.isSelected()) {
				hoveredResultButton = button;
			}
		}

		if (nextPageButton != null) {
			nextPageButton.render(context, mouseX, mouseY, deltaTicks);
		}

		if (prevPageButton != null) {
			prevPageButton.render(context, mouseX, mouseY, deltaTicks);
		}

		context.createNewRootLayer();
		alternatesWidget.render(context, mouseX, mouseY, deltaTicks);
	}

	public void drawTooltip(DrawContext context, int x, int y) {
		if (client.currentScreen == null || hoveredResultButton == null || alternatesWidget.isVisible()) {
			return;
		}

		ItemStack displayStack = hoveredResultButton.getDisplayStack();
		context.drawTooltip(
				client.textRenderer,
				hoveredResultButton.getTooltip(displayStack),
				x,
				y,
				displayStack.get(DataComponentTypes.TOOLTIP_STYLE)
		);
	}

	public @Nullable NetworkRecipeId getLastClickedRecipe() {
		return lastClickedRecipe;
	}

	public @Nullable RecipeResultCollection getLastClickedResults() {
		return resultCollection;
	}

	public void hideAlternates() {
		alternatesWidget.setVisible(false);
	}

	/**
	 * Обрабатывает клик мыши: делегирует виджету альтернатив, кнопкам пагинации
	 * или кнопкам результатов. При правом клике открывает виджет альтернативных рецептов.
	 */
	public boolean mouseClicked(Click click, int left, int top, int width, int height, boolean doubled) {
		lastClickedRecipe = null;
		resultCollection = null;

		if (alternatesWidget.isVisible()) {
			if (alternatesWidget.mouseClicked(click, doubled)) {
				lastClickedRecipe = alternatesWidget.getLastClickedRecipe();
				resultCollection = alternatesWidget.getResults();
			} else {
				alternatesWidget.setVisible(false);
			}

			return true;
		}

		if (nextPageButton.mouseClicked(click, doubled)) {
			currentPage++;
			refreshResultButtons();
			return true;
		}

		if (prevPageButton.mouseClicked(click, doubled)) {
			currentPage--;
			refreshResultButtons();
			return true;
		}

		ContextParameterMap context = SlotDisplayContexts.createParameters(client.world);

		for (AnimatedResultButton button : resultButtons) {
			if (!button.mouseClicked(click, doubled)) {
				continue;
			}

			if (click.button() == 0) {
				lastClickedRecipe = button.getCurrentId();
				resultCollection = button.getResultCollection();
			} else if (click.button() == 1 && !alternatesWidget.isVisible() && !button.hasSingleResult()) {
				alternatesWidget.showAlternativesForResult(
						button.getResultCollection(),
						context,
						filteringCraftable,
						button.getX(),
						button.getY(),
						left + width / 2,
						top + 13 + height / 2,
						button.getWidth()
				);
			}

			return true;
		}

		return false;
	}

	public void onRecipeDisplayed(NetworkRecipeId recipeId) {
		recipeBookWidget.onRecipeDisplayed(recipeId);
	}

	public ClientRecipeBook getRecipeBook() {
		return recipeBook;
	}

	protected void forEachButton(Consumer<ClickableWidget> action) {
		resultButtons.forEach(action);
	}
}
