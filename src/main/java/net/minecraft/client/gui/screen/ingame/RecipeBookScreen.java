package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

/**
 * Базовый экран с книгой рецептов. Управляет отображением виджета книги рецептов,
 * переключением режима «узкого» экрана и делегированием ввода.
 */
@Environment(EnvType.CLIENT)
public abstract class RecipeBookScreen<T extends AbstractRecipeScreenHandler> extends HandledScreen<T> implements RecipeBookProvider {

	private static final int RECIPE_BOOK_BUTTON_WIDTH = 20;
	private static final int RECIPE_BOOK_BUTTON_HEIGHT = 18;
	private static final int NARROW_SCREEN_THRESHOLD = 379;

	private final RecipeBookWidget<?> recipeBook;
	private boolean narrow;

	public RecipeBookScreen(T handler, RecipeBookWidget<?> recipeBook, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.recipeBook = recipeBook;
	}

	@Override
	protected void init() {
		super.init();
		narrow = width < NARROW_SCREEN_THRESHOLD;
		recipeBook.initialize(width, height, client, narrow);
		x = recipeBook.findLeftEdge(width, backgroundWidth);
		addRecipeBook();
	}

	protected abstract ScreenPos getRecipeBookButtonPos();

	private void addRecipeBook() {
		ScreenPos buttonPos = getRecipeBookButtonPos();
		addDrawableChild(new TexturedButtonWidget(
			buttonPos.x(),
			buttonPos.y(),
			RECIPE_BOOK_BUTTON_WIDTH,
			RECIPE_BOOK_BUTTON_HEIGHT,
			RecipeBookWidget.BUTTON_TEXTURES,
			button -> {
				recipeBook.toggleOpen();
				x = recipeBook.findLeftEdge(width, backgroundWidth);
				ScreenPos updatedPos = getRecipeBookButtonPos();
				button.setPosition(updatedPos.x(), updatedPos.y());
				onRecipeBookToggled();
			}
		));
		addSelectableChild(recipeBook);
	}

	protected void onRecipeBookToggled() {
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (recipeBook.isOpen() && narrow) {
			renderBackground(context, mouseX, mouseY, deltaTicks);
		} else {
			super.renderMain(context, mouseX, mouseY, deltaTicks);
		}

		context.createNewRootLayer();
		recipeBook.render(context, mouseX, mouseY, deltaTicks);
		context.createNewRootLayer();
		renderCursorStack(context, mouseX, mouseY);
		renderLetGoTouchStack(context);
		drawMouseoverTooltip(context, mouseX, mouseY);
		recipeBook.drawTooltip(context, mouseX, mouseY, focusedSlot);
	}

	@Override
	protected void drawSlots(DrawContext context, int mouseX, int mouseY) {
		super.drawSlots(context, mouseX, mouseY);
		recipeBook.drawGhostSlots(context, shouldAddPaddingToGhostResult());
	}

	protected boolean shouldAddPaddingToGhostResult() {
		return true;
	}

	@Override
	public boolean charTyped(CharInput input) {
		return recipeBook.charTyped(input) || super.charTyped(input);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		return recipeBook.keyPressed(input) || super.keyPressed(input);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (recipeBook.mouseClicked(click, doubled)) {
			setFocused(recipeBook);
			return true;
		}

		return narrow && recipeBook.isOpen() || super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		return recipeBook.mouseDragged(click, offsetX, offsetY) || super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	protected boolean isPointWithinBounds(int boundsX, int boundsY, int boundsWidth, int boundsHeight, double pointX, double pointY) {
		return (!narrow || !recipeBook.isOpen()) && super.isPointWithinBounds(boundsX, boundsY, boundsWidth, boundsHeight, pointX, pointY);
	}

	@Override
	protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top) {
		boolean outsideBackground = mouseX < left
			|| mouseY < top
			|| mouseX >= left + backgroundWidth
			|| mouseY >= top + backgroundHeight;

		return recipeBook.isClickOutsideBounds(mouseX, mouseY, x, y, backgroundWidth, backgroundHeight) && outsideBackground;
	}

	@Override
	protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
		super.onMouseClick(slot, slotId, button, actionType);
		recipeBook.onMouseClick(slot);
	}

	@Override
	public void handledScreenTick() {
		super.handledScreenTick();
		recipeBook.update();
	}

	@Override
	public void refreshRecipeBook() {
		recipeBook.refresh();
	}

	@Override
	public void onCraftFailed(RecipeDisplay display) {
		recipeBook.onCraftFailed(display);
	}
}
