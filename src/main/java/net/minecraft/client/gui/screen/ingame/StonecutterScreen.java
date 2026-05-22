package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.math.MathHelper;

/**
 * Экран камнерезного станка. Отображает список доступных рецептов с прокруткой
 * и позволяет выбрать нужный рецепт для обработки блока.
 */
@Environment(EnvType.CLIENT)
public class StonecutterScreen extends HandledScreen<StonecutterScreenHandler> {

	private static final Identifier SCROLLER_TEXTURE = Identifier.ofVanilla("container/stonecutter/scroller");
	private static final Identifier SCROLLER_DISABLED_TEXTURE = Identifier.ofVanilla("container/stonecutter/scroller_disabled");
	private static final Identifier RECIPE_SELECTED_TEXTURE = Identifier.ofVanilla("container/stonecutter/recipe_selected");
	private static final Identifier RECIPE_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("container/stonecutter/recipe_highlighted");
	private static final Identifier RECIPE_TEXTURE = Identifier.ofVanilla("container/stonecutter/recipe");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/stonecutter.png");
	private static final int SCROLLBAR_WIDTH = 12;
	private static final int SCROLLBAR_HEIGHT = 15;
	private static final int RECIPE_LIST_COLUMNS = 4;
	private static final int RECIPE_LIST_ROWS = 3;
	private static final int RECIPE_ENTRY_WIDTH = 16;
	private static final int RECIPE_ENTRY_HEIGHT = 18;
	private static final int SCROLLBAR_AREA_HEIGHT = 54;
	private static final int RECIPE_LIST_OFFSET_X = 52;
	private static final int RECIPE_LIST_OFFSET_Y = 14;
	private static final float SCROLL_RANGE = 41.0F;
	private static final float SCROLL_DRAG_HALF_HANDLE = 7.5F;
	private static final float SCROLL_DRAG_HANDLE_SIZE = 15.0F;

	private float scrollAmount;
	private boolean scrollbarClicked;
	private int scrollOffset;
	private boolean canCraft;

	public StonecutterScreen(StonecutterScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		handler.setContentsChangedListener(this::onInventoryChange);
		titleY--;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		int bgX = x;
		int bgY = y;

		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			bgX,
			bgY,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);

		int scrollY = (int) (SCROLL_RANGE * scrollAmount);
		Identifier scrollerTexture = shouldScroll() ? SCROLLER_TEXTURE : SCROLLER_DISABLED_TEXTURE;
		int scrollerX = bgX + 119;
		int scrollerY = bgY + SCROLLBAR_HEIGHT + scrollY;

		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, scrollerTexture, scrollerX, scrollerY, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);

		if (mouseX >= scrollerX && mouseX < scrollerX + SCROLLBAR_WIDTH
			&& mouseY >= scrollerY && mouseY < scrollerY + SCROLLBAR_HEIGHT
		) {
			context.setCursor(scrollbarClicked ? StandardCursors.RESIZE_NS : StandardCursors.POINTING_HAND);
		}

		int listX = x + RECIPE_LIST_OFFSET_X;
		int listY = y + RECIPE_LIST_OFFSET_Y;
		int endOffset = scrollOffset + SCROLLBAR_WIDTH;

		renderRecipeBackground(context, mouseX, mouseY, listX, listY, endOffset);
		renderRecipeIcons(context, listX, listY, endOffset);
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
		super.drawMouseoverTooltip(context, mouseX, mouseY);

		if (!canCraft) {
			return;
		}

		int listX = x + RECIPE_LIST_OFFSET_X;
		int listY = y + RECIPE_LIST_OFFSET_Y;
		int endOffset = scrollOffset + SCROLLBAR_WIDTH;
		CuttingRecipeDisplay.Grouping<StonecuttingRecipe> grouping = handler.getAvailableRecipes();

		for (int index = scrollOffset; index < endOffset && index < grouping.size(); index++) {
			int relativeIndex = index - scrollOffset;
			int entryX = listX + relativeIndex % RECIPE_LIST_COLUMNS * RECIPE_ENTRY_WIDTH;
			int entryY = listY + relativeIndex / RECIPE_LIST_COLUMNS * RECIPE_ENTRY_HEIGHT + 2;

			if (mouseX >= entryX && mouseX < entryX + RECIPE_ENTRY_WIDTH
				&& mouseY >= entryY && mouseY < entryY + RECIPE_ENTRY_HEIGHT
			) {
				ContextParameterMap contextParams = SlotDisplayContexts.createParameters(client.world);
				SlotDisplay slotDisplay = grouping.entries().get(index).recipe().optionDisplay();
				context.drawItemTooltip(textRenderer, slotDisplay.getFirst(contextParams), mouseX, mouseY);
			}
		}
	}

	private void renderRecipeBackground(DrawContext context, int mouseX, int mouseY, int listX, int listY, int endOffset) {
		for (int index = scrollOffset; index < endOffset && index < handler.getAvailableRecipeCount(); index++) {
			int relativeIndex = index - scrollOffset;
			int entryX = listX + relativeIndex % RECIPE_LIST_COLUMNS * RECIPE_ENTRY_WIDTH;
			int row = relativeIndex / RECIPE_LIST_COLUMNS;
			int entryY = listY + row * RECIPE_ENTRY_HEIGHT + 2;

			Identifier entryTexture;
			if (index == handler.getSelectedRecipe()) {
				entryTexture = RECIPE_SELECTED_TEXTURE;
			} else if (mouseX >= entryX && mouseY >= entryY
				&& mouseX < entryX + RECIPE_ENTRY_WIDTH
				&& mouseY < entryY + RECIPE_ENTRY_HEIGHT
			) {
				entryTexture = RECIPE_HIGHLIGHTED_TEXTURE;
			} else {
				entryTexture = RECIPE_TEXTURE;
			}

			int drawY = entryY - 1;
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, entryTexture, entryX, drawY, RECIPE_ENTRY_WIDTH, RECIPE_ENTRY_HEIGHT);

			if (mouseX >= entryX && mouseY >= drawY
				&& mouseX < entryX + RECIPE_ENTRY_WIDTH
				&& mouseY < drawY + RECIPE_ENTRY_HEIGHT
			) {
				context.setCursor(StandardCursors.POINTING_HAND);
			}
		}
	}

	private void renderRecipeIcons(DrawContext context, int listX, int listY, int endOffset) {
		CuttingRecipeDisplay.Grouping<StonecuttingRecipe> grouping = handler.getAvailableRecipes();
		ContextParameterMap contextParams = SlotDisplayContexts.createParameters(client.world);

		for (int index = scrollOffset; index < endOffset && index < grouping.size(); index++) {
			int relativeIndex = index - scrollOffset;
			int entryX = listX + relativeIndex % RECIPE_LIST_COLUMNS * RECIPE_ENTRY_WIDTH;
			int row = relativeIndex / RECIPE_LIST_COLUMNS;
			int entryY = listY + row * RECIPE_ENTRY_HEIGHT + 2;
			SlotDisplay slotDisplay = grouping.entries().get(index).recipe().optionDisplay();
			context.drawItem(slotDisplay.getFirst(contextParams), entryX, entryY);
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (canCraft) {
			int listX = x + RECIPE_LIST_OFFSET_X;
			int listY = y + RECIPE_LIST_OFFSET_Y;
			int endOffset = scrollOffset + SCROLLBAR_WIDTH;

			for (int index = scrollOffset; index < endOffset; index++) {
				int relativeIndex = index - scrollOffset;
				double relX = click.x() - (listX + relativeIndex % RECIPE_LIST_COLUMNS * RECIPE_ENTRY_WIDTH);
				double relY = click.y() - (listY + relativeIndex / RECIPE_LIST_COLUMNS * RECIPE_ENTRY_HEIGHT);

				if (relX >= 0.0 && relY >= 0.0 && relX < RECIPE_ENTRY_WIDTH && relY < RECIPE_ENTRY_HEIGHT
					&& handler.onButtonClick(client.player, index)
				) {
					MinecraftClient.getInstance()
						.getSoundManager()
						.play(PositionedSoundInstance.master(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
					client.interactionManager.clickButton(handler.syncId, index);
					return true;
				}
			}

			int scrollbarX = x + 119;
			int scrollbarY = y + RECIPE_LIST_OFFSET_Y - 1;

			if (click.x() >= scrollbarX && click.x() < scrollbarX + SCROLLBAR_WIDTH
				&& click.y() >= scrollbarY && click.y() < scrollbarY + SCROLLBAR_AREA_HEIGHT
			) {
				scrollbarClicked = true;
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (scrollbarClicked && shouldScroll()) {
			int topY = y + RECIPE_LIST_OFFSET_Y;
			int bottomY = topY + SCROLLBAR_AREA_HEIGHT;
			scrollAmount = ((float) click.y() - topY - SCROLL_DRAG_HALF_HANDLE) / (bottomY - topY - SCROLL_DRAG_HANDLE_SIZE);
			scrollAmount = MathHelper.clamp(scrollAmount, 0.0F, 1.0F);
			scrollOffset = (int) (scrollAmount * getMaxScroll() + 0.5) * RECIPE_LIST_COLUMNS;
			return true;
		}

		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		scrollbarClicked = false;
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		}

		if (shouldScroll()) {
			int maxScroll = getMaxScroll();
			float delta = (float) verticalAmount / maxScroll;
			scrollAmount = MathHelper.clamp(scrollAmount - delta, 0.0F, 1.0F);
			scrollOffset = (int) (scrollAmount * maxScroll + 0.5) * RECIPE_LIST_COLUMNS;
		}

		return true;
	}

	private boolean shouldScroll() {
		return canCraft && handler.getAvailableRecipeCount() > SCROLLBAR_WIDTH;
	}

	protected int getMaxScroll() {
		return (handler.getAvailableRecipeCount() + RECIPE_LIST_COLUMNS - 1) / RECIPE_LIST_COLUMNS - RECIPE_LIST_ROWS;
	}

	private void onInventoryChange() {
		canCraft = handler.canCraft();

		if (!canCraft) {
			scrollAmount = 0.0F;
			scrollOffset = 0;
		}
	}
}
