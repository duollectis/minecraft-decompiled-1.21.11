package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerData;

/**
 * Экран торговли с жителем деревни. Отображает список сделок, полосу опыта,
 * скроллбар и кнопки выбора сделки.
 */
@Environment(EnvType.CLIENT)
public class MerchantScreen extends HandledScreen<MerchantScreenHandler> {

	private static final Identifier OUT_OF_STOCK_TEXTURE = Identifier.ofVanilla("container/villager/out_of_stock");
	private static final Identifier EXPERIENCE_BAR_BACKGROUND_TEXTURE = Identifier.ofVanilla("container/villager/experience_bar_background");
	private static final Identifier EXPERIENCE_BAR_CURRENT_TEXTURE = Identifier.ofVanilla("container/villager/experience_bar_current");
	private static final Identifier EXPERIENCE_BAR_RESULT_TEXTURE = Identifier.ofVanilla("container/villager/experience_bar_result");
	private static final Identifier SCROLLER_TEXTURE = Identifier.ofVanilla("container/villager/scroller");
	private static final Identifier SCROLLER_DISABLED_TEXTURE = Identifier.ofVanilla("container/villager/scroller_disabled");
	private static final Identifier TRADE_ARROW_OUT_OF_STOCK_TEXTURE = Identifier.ofVanilla("container/villager/trade_arrow_out_of_stock");
	private static final Identifier TRADE_ARROW_TEXTURE = Identifier.ofVanilla("container/villager/trade_arrow");
	private static final Identifier DISCOUNT_STRIKETHROUGH_TEXTURE = Identifier.ofVanilla("container/villager/discount_strikethrough");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/villager.png");

	private static final int TEXTURE_WIDTH = 512;
	private static final int TEXTURE_HEIGHT = 256;
	private static final int MAX_TRADE_USES = 99;
	private static final int EXPERIENCE_BAR_X_OFFSET = 136;
	private static final int EXPERIENCE_BAR_WIDTH = 102;
	private static final int EXPERIENCE_BAR_HEIGHT = 5;
	private static final int TRADE_LIST_AREA_Y_OFFSET = 16;
	private static final int FIRST_BUY_ITEM_X_OFFSET = 5;
	private static final int SECOND_BUY_ITEM_X_OFFSET = 35;
	private static final int SOLD_ITEM_X_OFFSET = 68;
	private static final int MAX_VISIBLE_TRADES = 7;
	private static final int TRADE_OFFER_BUTTON_HEIGHT = 20;
	private static final int TRADE_OFFER_BUTTON_WIDTH = 88;
	private static final int SCROLLBAR_HEIGHT = 27;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int SCROLLBAR_AREA_HEIGHT = 139;
	private static final int SCROLLBAR_OFFSET_Y = 18;
	private static final int SCROLLBAR_OFFSET_X = 94;
	private static final int SCROLLBAR_MAX_POSITION = 113;
	private static final float SCROLLBAR_DRAG_HALF_HANDLE = 13.5F;
	private static final float SCROLLBAR_DRAG_FULL_HANDLE = 27.0F;
	private static final int TEXT_COLOR_TITLE = -12566464;
	private static final int BACKGROUND_WIDTH = 276;
	private static final int PLAYER_INVENTORY_TITLE_X = 107;
	private static final int OUT_OF_STOCK_WIDTH = 28;
	private static final int OUT_OF_STOCK_HEIGHT = 21;
	private static final int ARROW_WIDTH = 10;
	private static final int ARROW_HEIGHT = 9;
	private static final int ARROW_Y_OFFSET = 3;
	private static final int DISCOUNT_STRIKETHROUGH_WIDTH = 9;
	private static final int DISCOUNT_STRIKETHROUGH_HEIGHT = 2;
	private static final int DISCOUNT_STRIKETHROUGH_X_OFFSET = 7;
	private static final int DISCOUNT_STRIKETHROUGH_Y_OFFSET = 12;
	private static final int DISCOUNT_ITEM_X_OFFSET = 14;

	private static final Text TRADES_TEXT = Text.translatable("merchant.trades");
	private static final Text DEPRECATED_TEXT = Text.translatable("merchant.deprecated");

	private int selectedIndex;
	private final MerchantScreen.WidgetButtonPage[] offers = new MerchantScreen.WidgetButtonPage[MAX_VISIBLE_TRADES];
	int indexStartOffset;
	private boolean scrolling;

	public MerchantScreen(MerchantScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		backgroundWidth = BACKGROUND_WIDTH;
		playerInventoryTitleX = PLAYER_INVENTORY_TITLE_X;
	}

	private void syncRecipeIndex() {
		handler.setRecipeIndex(selectedIndex);
		handler.switchTo(selectedIndex);
		client.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(selectedIndex));
	}

	@Override
	protected void init() {
		super.init();
		int bgX = (width - backgroundWidth) / 2;
		int buttonY = (height - backgroundHeight) / 2 + TRADE_LIST_AREA_Y_OFFSET + 2;

		for (int slot = 0; slot < MAX_VISIBLE_TRADES; slot++) {
			offers[slot] = addDrawableChild(new MerchantScreen.WidgetButtonPage(
				bgX + FIRST_BUY_ITEM_X_OFFSET,
				buttonY,
				slot,
				button -> {
					if (button instanceof MerchantScreen.WidgetButtonPage page) {
						selectedIndex = page.getIndex() + indexStartOffset;
						syncRecipeIndex();
					}
				}
			));
			buttonY += TRADE_OFFER_BUTTON_HEIGHT;
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		int levelProgress = handler.getLevelProgress();

		if (levelProgress > 0 && levelProgress <= 5 && handler.isLeveled()) {
			Text leveledTitle = Text.translatable("merchant.title", title, Text.translatable("merchant.level." + levelProgress));
			int titleWidth = textRenderer.getWidth(leveledTitle);
			int titleX = 49 + backgroundWidth / 2 - titleWidth / 2;
			context.drawText(textRenderer, leveledTitle, titleX, 6, TEXT_COLOR_TITLE, false);
		} else {
			int titleX = 49 + backgroundWidth / 2 - textRenderer.getWidth(title) / 2;
			context.drawText(textRenderer, title, titleX, 6, TEXT_COLOR_TITLE, false);
		}

		context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, TEXT_COLOR_TITLE, false);

		int tradesTextWidth = textRenderer.getWidth(TRADES_TEXT);
		context.drawText(textRenderer, TRADES_TEXT, 5 - tradesTextWidth / 2 + 48, 6, TEXT_COLOR_TITLE, false);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		int bgX = (width - backgroundWidth) / 2;
		int bgY = (height - backgroundHeight) / 2;

		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			bgX,
			bgY,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			TEXTURE_WIDTH,
			TEXTURE_HEIGHT
		);

		TradeOfferList tradeOffers = handler.getRecipes();
		if (tradeOffers.isEmpty()) {
			return;
		}

		if (selectedIndex < 0 || selectedIndex >= tradeOffers.size()) {
			return;
		}

		TradeOffer selectedOffer = tradeOffers.get(selectedIndex);
		if (selectedOffer.isDisabled()) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				OUT_OF_STOCK_TEXTURE,
				x + 83 + MAX_TRADE_USES,
				y + SECOND_BUY_ITEM_X_OFFSET,
				OUT_OF_STOCK_WIDTH,
				OUT_OF_STOCK_HEIGHT
			);
		}
	}

	/**
	 * Отрисовывает полосу опыта жителя с текущим прогрессом и предполагаемым приростом от сделки.
	 */
	private void drawLevelInfo(DrawContext context, int x, int y, TradeOffer tradeOffer) {
		int levelProgress = handler.getLevelProgress();
		int experience = handler.getExperience();

		if (levelProgress >= 5) {
			return;
		}

		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			EXPERIENCE_BAR_BACKGROUND_TEXTURE,
			x + EXPERIENCE_BAR_X_OFFSET,
			y + TRADE_LIST_AREA_Y_OFFSET,
			EXPERIENCE_BAR_WIDTH,
			EXPERIENCE_BAR_HEIGHT
		);

		int lowerBound = VillagerData.getLowerLevelExperience(levelProgress);
		if (experience < lowerBound || !VillagerData.canLevelUp(levelProgress)) {
			return;
		}

		float expPerPixel = (float) EXPERIENCE_BAR_WIDTH / (VillagerData.getUpperLevelExperience(levelProgress) - lowerBound);
		int currentBarWidth = Math.min(MathHelper.floor(expPerPixel * (experience - lowerBound)), EXPERIENCE_BAR_WIDTH);

		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			EXPERIENCE_BAR_CURRENT_TEXTURE,
			EXPERIENCE_BAR_WIDTH,
			EXPERIENCE_BAR_HEIGHT,
			0,
			0,
			x + EXPERIENCE_BAR_X_OFFSET,
			y + TRADE_LIST_AREA_Y_OFFSET,
			currentBarWidth,
			EXPERIENCE_BAR_HEIGHT
		);

		int rewardedExp = handler.getMerchantRewardedExperience();
		if (rewardedExp <= 0) {
			return;
		}

		int rewardBarWidth = Math.min(MathHelper.floor(rewardedExp * expPerPixel), EXPERIENCE_BAR_WIDTH - currentBarWidth);
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			EXPERIENCE_BAR_RESULT_TEXTURE,
			EXPERIENCE_BAR_WIDTH,
			EXPERIENCE_BAR_HEIGHT,
			currentBarWidth,
			0,
			x + EXPERIENCE_BAR_X_OFFSET + currentBarWidth,
			y + TRADE_LIST_AREA_Y_OFFSET,
			rewardBarWidth,
			EXPERIENCE_BAR_HEIGHT
		);
	}

	/**
	 * Отрисовывает скроллбар списка сделок. Если сделок больше {@link #MAX_VISIBLE_TRADES},
	 * позиция ползунка вычисляется пропорционально {@link #indexStartOffset}.
	 */
	private void renderScrollbar(DrawContext context, int x, int y, int mouseX, int mouseY, TradeOfferList tradeOffers) {
		int overflow = tradeOffers.size() + 1 - MAX_VISIBLE_TRADES;
		if (overflow <= 1) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				SCROLLER_DISABLED_TEXTURE,
				x + SCROLLBAR_OFFSET_X,
				y + SCROLLBAR_OFFSET_Y,
				SCROLLBAR_WIDTH,
				SCROLLBAR_HEIGHT
			);
			return;
		}

		int stepSize = SCROLLBAR_AREA_HEIGHT - (SCROLLBAR_HEIGHT + (overflow - 1) * SCROLLBAR_AREA_HEIGHT / overflow);
		int pixelsPerStep = 1 + stepSize / overflow + SCROLLBAR_AREA_HEIGHT / overflow;
		int scrollerY = Math.min(SCROLLBAR_MAX_POSITION, indexStartOffset * pixelsPerStep);

		if (indexStartOffset == overflow - 1) {
			scrollerY = SCROLLBAR_MAX_POSITION;
		}

		int scrollerX = x + SCROLLBAR_OFFSET_X;
		int scrollerAbsY = y + SCROLLBAR_OFFSET_Y + scrollerY;

		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SCROLLER_TEXTURE, scrollerX, scrollerAbsY, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);

		boolean hoveringScrollbar = mouseX >= scrollerX
			&& mouseX < x + SCROLLBAR_OFFSET_X + SCROLLBAR_WIDTH
			&& mouseY >= scrollerAbsY
			&& mouseY <= scrollerAbsY + SCROLLBAR_HEIGHT;

		if (hoveringScrollbar) {
			context.setCursor(scrolling ? StandardCursors.RESIZE_NS : StandardCursors.POINTING_HAND);
		}
	}

	@Override
	public void renderMain(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.renderMain(context, mouseX, mouseY, deltaTicks);

		TradeOfferList tradeOffers = handler.getRecipes();
		if (tradeOffers.isEmpty()) {
			drawMouseoverTooltip(context, mouseX, mouseY);
			return;
		}

		int bgX = (width - backgroundWidth) / 2;
		int bgY = (height - backgroundHeight) / 2;
		int rowY = bgY + TRADE_LIST_AREA_Y_OFFSET + 1;
		int itemX = bgX + FIRST_BUY_ITEM_X_OFFSET + FIRST_BUY_ITEM_X_OFFSET;

		renderScrollbar(context, bgX, bgY, mouseX, mouseY, tradeOffers);

		int offerIndex = 0;
		for (TradeOffer tradeOffer : tradeOffers) {
			boolean inVisibleRange = !canScroll(tradeOffers.size())
				|| (offerIndex >= indexStartOffset && offerIndex < MAX_VISIBLE_TRADES + indexStartOffset);

			if (inVisibleRange) {
				int itemY = rowY + 2;
				renderFirstBuyItem(context, tradeOffer.getDisplayedFirstBuyItem(), tradeOffer.getOriginalFirstBuyItem(), itemX, itemY);

				ItemStack secondBuyItem = tradeOffer.getDisplayedSecondBuyItem();
				if (!secondBuyItem.isEmpty()) {
					context.drawItemWithoutEntity(secondBuyItem, bgX + FIRST_BUY_ITEM_X_OFFSET + SECOND_BUY_ITEM_X_OFFSET, itemY);
					context.drawStackOverlay(textRenderer, secondBuyItem, bgX + FIRST_BUY_ITEM_X_OFFSET + SECOND_BUY_ITEM_X_OFFSET, itemY);
				}

				renderArrow(context, tradeOffer, bgX, itemY);

				ItemStack sellItem = tradeOffer.getSellItem();
				context.drawItemWithoutEntity(sellItem, bgX + FIRST_BUY_ITEM_X_OFFSET + SOLD_ITEM_X_OFFSET, itemY);
				context.drawStackOverlay(textRenderer, sellItem, bgX + FIRST_BUY_ITEM_X_OFFSET + SOLD_ITEM_X_OFFSET, itemY);

				rowY += TRADE_OFFER_BUTTON_HEIGHT;
			}

			offerIndex++;
		}

		TradeOffer selectedOffer = tradeOffers.get(selectedIndex);
		if (handler.isLeveled()) {
			drawLevelInfo(context, bgX, bgY, selectedOffer);
		}

		if (selectedOffer.isDisabled()
			&& isPointWithinBounds(186, SECOND_BUY_ITEM_X_OFFSET, 22, 21, mouseX, mouseY)
			&& handler.canRefreshTrades()
		) {
			context.drawTooltip(textRenderer, DEPRECATED_TEXT, mouseX, mouseY);
		}

		for (MerchantScreen.WidgetButtonPage page : offers) {
			if (page.isSelected()) {
				page.renderTooltip(context, mouseX, mouseY);
			}

			page.visible = page.index < handler.getRecipes().size();
		}

		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	private void renderArrow(DrawContext context, TradeOffer tradeOffer, int x, int y) {
		Identifier arrowTexture = tradeOffer.isDisabled() ? TRADE_ARROW_OUT_OF_STOCK_TEXTURE : TRADE_ARROW_TEXTURE;
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			arrowTexture,
			x + FIRST_BUY_ITEM_X_OFFSET + SECOND_BUY_ITEM_X_OFFSET + TRADE_OFFER_BUTTON_HEIGHT,
			y + ARROW_Y_OFFSET,
			ARROW_WIDTH,
			ARROW_HEIGHT
		);
	}

	private void renderFirstBuyItem(
		DrawContext context,
		ItemStack adjustedFirstBuyItem,
		ItemStack originalFirstBuyItem,
		int x,
		int y
	) {
		context.drawItemWithoutEntity(adjustedFirstBuyItem, x, y);

		if (originalFirstBuyItem.getCount() == adjustedFirstBuyItem.getCount()) {
			context.drawStackOverlay(textRenderer, adjustedFirstBuyItem, x, y);
			return;
		}

		context.drawStackOverlay(
			textRenderer,
			originalFirstBuyItem,
			x,
			y,
			originalFirstBuyItem.getCount() == 1 ? "1" : null
		);
		context.drawStackOverlay(
			textRenderer,
			adjustedFirstBuyItem,
			x + DISCOUNT_ITEM_X_OFFSET,
			y,
			adjustedFirstBuyItem.getCount() == 1 ? "1" : null
		);
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			DISCOUNT_STRIKETHROUGH_TEXTURE,
			x + DISCOUNT_STRIKETHROUGH_X_OFFSET,
			y + DISCOUNT_STRIKETHROUGH_Y_OFFSET,
			DISCOUNT_STRIKETHROUGH_WIDTH,
			DISCOUNT_STRIKETHROUGH_HEIGHT
		);
	}

	private boolean canScroll(int listSize) {
		return listSize > MAX_VISIBLE_TRADES;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		}

		int totalOffers = handler.getRecipes().size();
		if (canScroll(totalOffers)) {
			int maxOffset = totalOffers - MAX_VISIBLE_TRADES;
			indexStartOffset = MathHelper.clamp((int) (indexStartOffset - verticalAmount), 0, maxOffset);
		}

		return true;
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (!scrolling) {
			return super.mouseDragged(click, offsetX, offsetY);
		}

		int totalOffers = handler.getRecipes().size();
		int topY = y + SCROLLBAR_OFFSET_Y;
		int bottomY = topY + SCROLLBAR_AREA_HEIGHT;
		int maxOffset = totalOffers - MAX_VISIBLE_TRADES;
		float normalizedPosition = ((float) click.y() - topY - SCROLLBAR_DRAG_HALF_HANDLE) / (bottomY - topY - SCROLLBAR_DRAG_FULL_HANDLE);
		float scaledPosition = normalizedPosition * maxOffset + 0.5F;

		indexStartOffset = MathHelper.clamp((int) scaledPosition, 0, maxOffset);
		return true;
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		int bgX = (width - backgroundWidth) / 2;
		int bgY = (height - backgroundHeight) / 2;

		boolean inScrollbarX = click.x() > bgX + SCROLLBAR_OFFSET_X && click.x() < bgX + SCROLLBAR_OFFSET_X + SCROLLBAR_WIDTH;
		boolean inScrollbarY = click.y() > bgY + SCROLLBAR_OFFSET_Y && click.y() <= bgY + SCROLLBAR_OFFSET_Y + SCROLLBAR_AREA_HEIGHT + 1;

		if (canScroll(handler.getRecipes().size()) && inScrollbarX && inScrollbarY) {
			scrolling = true;
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseReleased(Click click) {
		scrolling = false;
		return super.mouseReleased(click);
	}

	/**
	 * Кнопка выбора конкретной сделки в списке торговли жителя.
	 */
	@Environment(EnvType.CLIENT)
	class WidgetButtonPage extends ButtonWidget.Text {

		final int index;

		public WidgetButtonPage(int x, int y, int index, ButtonWidget.PressAction onPress) {
			super(x, y, TRADE_OFFER_BUTTON_WIDTH, TRADE_OFFER_BUTTON_HEIGHT, ScreenTexts.EMPTY, onPress, DEFAULT_NARRATION_SUPPLIER);
			this.index = index;
			visible = false;
		}

		public int getIndex() {
			return index;
		}

		/**
		 * Отрисовывает тултип с предметами сделки в зависимости от позиции курсора:
		 * левая зона — первый покупаемый предмет, средняя — второй, правая — продаваемый.
		 */
		public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
			if (!hovered) {
				return;
			}

			int absoluteIndex = index + MerchantScreen.this.indexStartOffset;
			if (absoluteIndex >= MerchantScreen.this.handler.getRecipes().size()) {
				return;
			}

			TradeOffer offer = MerchantScreen.this.handler.getRecipes().get(absoluteIndex);

			if (mouseX < getX() + TRADE_OFFER_BUTTON_HEIGHT) {
				context.drawItemTooltip(MerchantScreen.this.textRenderer, offer.getDisplayedFirstBuyItem(), mouseX, mouseY);
			} else if (mouseX < getX() + 50 && mouseX > getX() + 30) {
				ItemStack secondBuyItem = offer.getDisplayedSecondBuyItem();
				if (!secondBuyItem.isEmpty()) {
					context.drawItemTooltip(MerchantScreen.this.textRenderer, secondBuyItem, mouseX, mouseY);
				}
			} else if (mouseX > getX() + 65) {
				context.drawItemTooltip(MerchantScreen.this.textRenderer, offer.getSellItem(), mouseX, mouseY);
			}
		}
	}
}
