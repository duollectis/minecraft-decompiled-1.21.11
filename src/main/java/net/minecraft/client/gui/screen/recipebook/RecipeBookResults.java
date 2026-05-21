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

@Environment(EnvType.CLIENT)
/**
 * {@code RecipeBookResults}.
 */
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
	private final List<AnimatedResultButton> resultButtons = Lists.newArrayListWithCapacity(20);
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
		this.alternatesWidget = new RecipeAlternativesWidget(currentIndexProvider, furnace);

		for (int i = 0; i < 20; i++) {
			this.resultButtons.add(new AnimatedResultButton(currentIndexProvider));
		}
	}

	/**
	 * Инициализирует ialize.
	 *
	 * @param client client
	 * @param parentLeft parent left
	 * @param parentTop parent top
	 */
	public void initialize(MinecraftClient client, int parentLeft, int parentTop) {
		this.client = client;
		this.recipeBook = client.player.getRecipeBook();

		for (int i = 0; i < this.resultButtons.size(); i++) {
			this.resultButtons.get(i).setPosition(parentLeft + 11 + 25 * (i % 5), parentTop + 31 + 25 * (i / 5));
		}

		this.nextPageButton = new TexturedButtonWidget(
				parentLeft + 93,
				parentTop + 137,
				12,
				17,
				PAGE_FORWARD_TEXTURES,
				buttonWidget -> this.hideShowPageButtons(),
				NEXT_PAGE_TOOLTIP
		);
		this.nextPageButton.setTooltip(Tooltip.of(NEXT_PAGE_TOOLTIP));
		this.prevPageButton = new TexturedButtonWidget(
				parentLeft + 38,
				parentTop + 137,
				12,
				17,
				PAGE_BACKWARD_TEXTURES,
				buttonWidget -> this.hideShowPageButtons(),
				PREVIOUS_PAGE_TOOLTIP
		);
		this.prevPageButton.setTooltip(Tooltip.of(PREVIOUS_PAGE_TOOLTIP));
	}

	public void setResults(
			List<RecipeResultCollection> resultCollections,
			boolean resetCurrentPage,
			boolean filteringCraftable
	) {
		this.resultCollections = resultCollections;
		this.filteringCraftable = filteringCraftable;
		this.pageCount = (int) Math.ceil(resultCollections.size() / 20.0);
		if (this.pageCount <= this.currentPage || resetCurrentPage) {
			this.currentPage = 0;
		}

		this.refreshResultButtons();
	}

	private void refreshResultButtons() {
		int i = 20 * this.currentPage;
		ContextParameterMap contextParameterMap = SlotDisplayContexts.createParameters(this.client.world);

		for (int j = 0; j < this.resultButtons.size(); j++) {
			AnimatedResultButton animatedResultButton = this.resultButtons.get(j);
			if (i + j < this.resultCollections.size()) {
				RecipeResultCollection recipeResultCollection = this.resultCollections.get(i + j);
				animatedResultButton.showResultCollection(
						recipeResultCollection,
						this.filteringCraftable,
						this,
						contextParameterMap
				);
				animatedResultButton.visible = true;
			}
			else {
				animatedResultButton.visible = false;
			}
		}

		this.hideShowPageButtons();
	}

	private void hideShowPageButtons() {
		if (this.nextPageButton != null) {
			this.nextPageButton.visible = this.pageCount > 1 && this.currentPage < this.pageCount - 1;
		}

		if (this.prevPageButton != null) {
			this.prevPageButton.visible = this.pageCount > 1 && this.currentPage > 0;
		}
	}

	/**
	 * Draw.
	 *
	 * @param context context
	 * @param x x
	 * @param y y
	 * @param mouseX mouse x
	 * @param mouseY mouse y
	 * @param deltaTicks delta ticks
	 */
	public void draw(DrawContext context, int x, int y, int mouseX, int mouseY, float deltaTicks) {
		if (this.pageCount > 1) {
			Text text = Text.translatable("gui.recipebook.page", this.currentPage + 1, this.pageCount);
			int i = this.client.textRenderer.getWidth(text);
			context.drawTextWithShadow(this.client.textRenderer, text, x - i / 2 + 73, y + 141, -1);
		}

		this.hoveredResultButton = null;

		for (AnimatedResultButton animatedResultButton : this.resultButtons) {
			animatedResultButton.render(context, mouseX, mouseY, deltaTicks);
			if (animatedResultButton.visible && animatedResultButton.isSelected()) {
				this.hoveredResultButton = animatedResultButton;
			}
		}

		if (this.nextPageButton != null) {
			this.nextPageButton.render(context, mouseX, mouseY, deltaTicks);
		}

		if (this.prevPageButton != null) {
			this.prevPageButton.render(context, mouseX, mouseY, deltaTicks);
		}

		context.createNewRootLayer();
		this.alternatesWidget.render(context, mouseX, mouseY, deltaTicks);
	}

	/**
	 * Draw tooltip.
	 *
	 * @param context context
	 * @param x x
	 * @param y y
	 */
	public void drawTooltip(DrawContext context, int x, int y) {
		if (this.client.currentScreen != null && this.hoveredResultButton != null
				&& !this.alternatesWidget.isVisible()) {
			ItemStack itemStack = this.hoveredResultButton.getDisplayStack();
			Identifier identifier = itemStack.get(DataComponentTypes.TOOLTIP_STYLE);
			context.drawTooltip(
					this.client.textRenderer,
					this.hoveredResultButton.getTooltip(itemStack),
					x,
					y,
					identifier
			);
		}
	}

	public @Nullable NetworkRecipeId getLastClickedRecipe() {
		return this.lastClickedRecipe;
	}

	public @Nullable RecipeResultCollection getLastClickedResults() {
		return this.resultCollection;
	}

	/**
	 * Hide alternates.
	 */
	public void hideAlternates() {
		this.alternatesWidget.setVisible(false);
	}

	/**
	 * Mouse clicked.
	 *
	 * @param click click
	 * @param left left
	 * @param top top
	 * @param width width
	 * @param height height
	 * @param doubled doubled
	 *
	 * @return boolean — результат операции
	 */
	public boolean mouseClicked(Click click, int left, int top, int width, int height, boolean doubled) {
		this.lastClickedRecipe = null;
		this.resultCollection = null;
		if (this.alternatesWidget.isVisible()) {
			if (this.alternatesWidget.mouseClicked(click, doubled)) {
				this.lastClickedRecipe = this.alternatesWidget.getLastClickedRecipe();
				this.resultCollection = this.alternatesWidget.getResults();
			}
			else {
				this.alternatesWidget.setVisible(false);
			}

			return true;
		}
		else if (this.nextPageButton.mouseClicked(click, doubled)) {
			this.currentPage++;
			this.refreshResultButtons();
			return true;
		}
		else if (this.prevPageButton.mouseClicked(click, doubled)) {
			this.currentPage--;
			this.refreshResultButtons();
			return true;
		}
		else {
			ContextParameterMap contextParameterMap = SlotDisplayContexts.createParameters(this.client.world);

			for (AnimatedResultButton animatedResultButton : this.resultButtons) {
				if (animatedResultButton.mouseClicked(click, doubled)) {
					if (click.button() == 0) {
						this.lastClickedRecipe = animatedResultButton.getCurrentId();
						this.resultCollection = animatedResultButton.getResultCollection();
					}
					else if (click.button() == 1 && !this.alternatesWidget.isVisible()
							&& !animatedResultButton.hasSingleResult()) {
						this.alternatesWidget
								.showAlternativesForResult(
										animatedResultButton.getResultCollection(),
										contextParameterMap,
										this.filteringCraftable,
										animatedResultButton.getX(),
										animatedResultButton.getY(),
										left + width / 2,
										top + 13 + height / 2,
										animatedResultButton.getWidth()
								);
					}

					return true;
				}
			}

			return false;
		}
	}

	/**
	 * Обрабатывает событие recipe displayed.
	 *
	 * @param recipeId recipe id
	 */
	public void onRecipeDisplayed(NetworkRecipeId recipeId) {
		this.recipeBookWidget.onRecipeDisplayed(recipeId);
	}

	public ClientRecipeBook getRecipeBook() {
		return this.recipeBook;
	}

	/**
	 * For each button.
	 *
	 * @param action action
	 */
	protected void forEachButton(Consumer<ClickableWidget> action) {
		this.resultButtons.forEach(action);
	}
}
