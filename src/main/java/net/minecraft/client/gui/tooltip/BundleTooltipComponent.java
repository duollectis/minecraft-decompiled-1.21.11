package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.math.Fraction;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Компонент тултипа для предмета «сумка» (bundle).
 * Отображает сетку слотов с содержимым сумки, полосу заполненности
 * и тултип выбранного предмета при наведении курсора.
 *
 * <p>Сетка строится справа налево, снизу вверх — порядок соответствует
 * порядку добавления предметов в сумку (последний добавленный — в правом нижнем углу).</p>
 */
@Environment(EnvType.CLIENT)
public class BundleTooltipComponent implements TooltipComponent {

	private static final Identifier BUNDLE_PROGRESS_BAR_BORDER_TEXTURE =
		Identifier.ofVanilla("container/bundle/bundle_progressbar_border");
	private static final Identifier BUNDLE_PROGRESS_BAR_FILL_TEXTURE =
		Identifier.ofVanilla("container/bundle/bundle_progressbar_fill");
	private static final Identifier BUNDLE_PROGRESS_BAR_FULL_TEXTURE =
		Identifier.ofVanilla("container/bundle/bundle_progressbar_full");
	private static final Identifier BUNDLE_SLOT_HIGHLIGHT_BACK_TEXTURE =
		Identifier.ofVanilla("container/bundle/slot_highlight_back");
	private static final Identifier BUNDLE_SLOT_HIGHLIGHT_FRONT_TEXTURE =
		Identifier.ofVanilla("container/bundle/slot_highlight_front");
	private static final Identifier BUNDLE_SLOT_BACKGROUND_TEXTURE =
		Identifier.ofVanilla("container/bundle/slot_background");

	private static final int SLOTS_PER_ROW = 4;
	/** Максимальное количество видимых слотов в тултипе. */
	private static final int MAX_VISIBLE_SLOTS = 12;
	private static final int SLOT_DIMENSION = 24;
	private static final int ROW_WIDTH = 96;
	/** Смещение предмета внутри слота от его левого/верхнего края. */
	private static final int ITEM_SLOT_OFFSET = 4;
	/** Горизонтальная позиция центра текста метки прогресс-бара. */
	private static final int PROGRESS_BAR_LABEL_CENTER_X = 48;
	/** Вертикальная позиция текста метки прогресс-бара. */
	private static final int PROGRESS_BAR_LABEL_Y = 3;
	private static final int PROGRESS_BAR_HEIGHT = 13;
	private static final int PROGRESS_BAR_OUTER_WIDTH = 96;
	/** Ширина заполняемой части прогресс-бара (без рамки). */
	private static final int PROGRESS_BAR_FILL_WIDTH = 94;
	/** Отступ прогресс-бара от содержимого сверху. */
	private static final int PROGRESS_BAR_MARGIN_TOP = 4;
	/** Нижний отступ тултипа. */
	private static final int TOOLTIP_BOTTOM_PADDING = 8;
	/** Вертикальный отступ тултипа выбранного предмета. */
	private static final int SELECTED_ITEM_TOOLTIP_OFFSET_Y = 15;
	/** Горизонтальный сдвиг центра тултипа выбранного предмета. */
	private static final int SELECTED_ITEM_TOOLTIP_OFFSET_X = 12;
	/** Горизонтальная позиция текста «+N» в слоте переполнения. */
	private static final int OVERFLOW_TEXT_OFFSET_X = 12;
	/** Вертикальная позиция текста «+N» в слоте переполнения. */
	private static final int OVERFLOW_TEXT_OFFSET_Y = 10;
	/** Высота одной строки текста описания. */
	private static final int DESCRIPTION_LINE_HEIGHT = 9;
	/** Цвет текста описания пустой сумки (серый). */
	private static final int DESCRIPTION_TEXT_COLOR = -5592406;
	/** Цвет текста тултипа (белый). */
	private static final int TOOLTIP_TEXT_COLOR = -1;

	private static final Text BUNDLE_FULL = Text.translatable("item.minecraft.bundle.full");
	private static final Text BUNDLE_EMPTY = Text.translatable("item.minecraft.bundle.empty");
	private static final Text BUNDLE_EMPTY_DESCRIPTION = Text.translatable("item.minecraft.bundle.empty.description");

	private final BundleContentsComponent bundleContents;

	public BundleTooltipComponent(BundleContentsComponent bundleContents) {
		this.bundleContents = bundleContents;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return bundleContents.isEmpty()
			? getHeightOfEmpty(textRenderer)
			: getHeightOfNonEmpty();
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return ROW_WIDTH;
	}

	@Override
	public boolean isSticky() {
		return true;
	}

	private static int getHeightOfEmpty(TextRenderer textRenderer) {
		return getDescriptionHeight(textRenderer) + PROGRESS_BAR_HEIGHT + TOOLTIP_BOTTOM_PADDING;
	}

	private int getHeightOfNonEmpty() {
		return getRowsHeight() + PROGRESS_BAR_HEIGHT + TOOLTIP_BOTTOM_PADDING;
	}

	private int getRowsHeight() {
		return getRows() * SLOT_DIMENSION;
	}

	private int getXMargin(int width) {
		return (width - ROW_WIDTH) / 2;
	}

	private int getRows() {
		return MathHelper.ceilDiv(getNumVisibleSlots(), SLOTS_PER_ROW);
	}

	private int getNumVisibleSlots() {
		return Math.min(MAX_VISIBLE_SLOTS, bundleContents.size());
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		if (bundleContents.isEmpty()) {
			drawEmptyTooltip(textRenderer, x, y, width, context);
		} else {
			drawNonEmptyTooltip(textRenderer, x, y, width, context);
		}
	}

	private void drawEmptyTooltip(TextRenderer textRenderer, int x, int y, int width, DrawContext context) {
		int margin = getXMargin(width);
		drawEmptyDescription(x + margin, y, textRenderer, context);
		drawProgressBar(
			x + margin,
			y + getDescriptionHeight(textRenderer) + PROGRESS_BAR_MARGIN_TOP,
			textRenderer,
			context
		);
	}

	/**
	 * Отрисовывает непустой тултип сумки: сетку слотов с предметами,
	 * тултип выбранного предмета и полосу заполненности.
	 * Слоты нумеруются справа налево, снизу вверх.
	 */
	private void drawNonEmptyTooltip(TextRenderer textRenderer, int x, int y, int width, DrawContext context) {
		boolean hasOverflow = bundleContents.size() > MAX_VISIBLE_SLOTS;
		List<ItemStack> visibleStacks = firstStacksInContents(bundleContents.getNumberOfStacksShown());
		int rightEdge = x + getXMargin(width) + ROW_WIDTH;
		int bottomEdge = y + getRows() * SLOT_DIMENSION;
		int slotIndex = 1;

		for (int row = 1; row <= getRows(); row++) {
			for (int col = 1; col <= SLOTS_PER_ROW; col++) {
				int slotX = rightEdge - col * SLOT_DIMENSION;
				int slotY = bottomEdge - row * SLOT_DIMENSION;

				if (shouldDrawExtraItemsCount(hasOverflow, col, row)) {
					drawExtraItemsCount(slotX, slotY, numContentItemsAfter(visibleStacks), textRenderer, context);
				} else if (shouldDrawItem(visibleStacks, slotIndex)) {
					drawItem(slotIndex, slotX, slotY, visibleStacks, slotIndex, textRenderer, context);
					slotIndex++;
				}
			}
		}

		drawSelectedItemTooltip(textRenderer, context, x, y, width);
		drawProgressBar(x + getXMargin(width), y + getRowsHeight() + PROGRESS_BAR_MARGIN_TOP, textRenderer, context);
	}

	private List<ItemStack> firstStacksInContents(int numberOfStacksShown) {
		int count = Math.min(bundleContents.size(), numberOfStacksShown);
		return bundleContents.stream().toList().subList(0, count);
	}

	/** Слот переполнения — только первый слот (col=1, row=1, произведение = 1). */
	private static boolean shouldDrawExtraItemsCount(boolean hasMoreItems, int column, int row) {
		return hasMoreItems && column * row == 1;
	}

	private static boolean shouldDrawItem(List<ItemStack> items, int itemIndex) {
		return items.size() >= itemIndex;
	}

	private int numContentItemsAfter(List<ItemStack> items) {
		return bundleContents.stream().skip(items.size()).mapToInt(ItemStack::getCount).sum();
	}

	private void drawItem(
		int index,
		int x,
		int y,
		List<ItemStack> stacks,
		int seed,
		TextRenderer textRenderer,
		DrawContext drawContext
	) {
		int stackIndex = stacks.size() - index;
		boolean isSelected = stackIndex == bundleContents.getSelectedStackIndex();
		ItemStack itemStack = stacks.get(stackIndex);
		Identifier slotTexture = isSelected ? BUNDLE_SLOT_HIGHLIGHT_BACK_TEXTURE : BUNDLE_SLOT_BACKGROUND_TEXTURE;
		drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, slotTexture, x, y, SLOT_DIMENSION, SLOT_DIMENSION);
		drawContext.drawItem(itemStack, x + ITEM_SLOT_OFFSET, y + ITEM_SLOT_OFFSET, seed);
		drawContext.drawStackOverlay(textRenderer, itemStack, x + ITEM_SLOT_OFFSET, y + ITEM_SLOT_OFFSET);

		if (isSelected) {
			drawContext.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				BUNDLE_SLOT_HIGHLIGHT_FRONT_TEXTURE,
				x,
				y,
				SLOT_DIMENSION,
				SLOT_DIMENSION
			);
		}
	}

	private static void drawExtraItemsCount(
		int x,
		int y,
		int numExtra,
		TextRenderer textRenderer,
		DrawContext drawContext
	) {
		drawContext.drawCenteredTextWithShadow(
			textRenderer,
			"+" + numExtra,
			x + OVERFLOW_TEXT_OFFSET_X,
			y + OVERFLOW_TEXT_OFFSET_Y,
			TOOLTIP_TEXT_COLOR
		);
	}

	private void drawSelectedItemTooltip(TextRenderer textRenderer, DrawContext drawContext, int x, int y, int width) {
		if (!bundleContents.hasSelectedStack()) {
			return;
		}

		ItemStack itemStack = bundleContents.get(bundleContents.getSelectedStackIndex());
		Text itemName = itemStack.getFormattedName();
		int textWidth = textRenderer.getWidth(itemName.asOrderedText());
		int tooltipX = x + width / 2 - SELECTED_ITEM_TOOLTIP_OFFSET_X;
		TooltipComponent tooltipComponent = TooltipComponent.of(itemName.asOrderedText());
		drawContext.drawTooltipImmediately(
			textRenderer,
			List.of(tooltipComponent),
			tooltipX - textWidth / 2,
			y - SELECTED_ITEM_TOOLTIP_OFFSET_Y,
			HoveredTooltipPositioner.INSTANCE,
			itemStack.get(DataComponentTypes.TOOLTIP_STYLE)
		);
	}

	private void drawProgressBar(int x, int y, TextRenderer textRenderer, DrawContext drawContext) {
		drawContext.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			getProgressBarFillTexture(),
			x + 1,
			y,
			getProgressBarFill(),
			PROGRESS_BAR_HEIGHT
		);
		drawContext.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			BUNDLE_PROGRESS_BAR_BORDER_TEXTURE,
			x,
			y,
			PROGRESS_BAR_OUTER_WIDTH,
			PROGRESS_BAR_HEIGHT
		);

		Text label = getProgressBarLabel();
		if (label != null) {
			drawContext.drawCenteredTextWithShadow(
				textRenderer,
				label,
				x + PROGRESS_BAR_LABEL_CENTER_X,
				y + PROGRESS_BAR_LABEL_Y,
				TOOLTIP_TEXT_COLOR
			);
		}
	}

	private static void drawEmptyDescription(int x, int y, TextRenderer textRenderer, DrawContext drawContext) {
		drawContext.drawWrappedTextWithShadow(textRenderer, BUNDLE_EMPTY_DESCRIPTION, x, y, ROW_WIDTH, DESCRIPTION_TEXT_COLOR);
	}

	private static int getDescriptionHeight(TextRenderer textRenderer) {
		return textRenderer.wrapLines(BUNDLE_EMPTY_DESCRIPTION, ROW_WIDTH).size() * DESCRIPTION_LINE_HEIGHT;
	}

	private int getProgressBarFill() {
		return MathHelper.clamp(
			MathHelper.multiplyFraction(bundleContents.getOccupancy(), PROGRESS_BAR_FILL_WIDTH),
			0,
			PROGRESS_BAR_FILL_WIDTH
		);
	}

	private Identifier getProgressBarFillTexture() {
		return bundleContents.getOccupancy().compareTo(Fraction.ONE) >= 0
			? BUNDLE_PROGRESS_BAR_FULL_TEXTURE
			: BUNDLE_PROGRESS_BAR_FILL_TEXTURE;
	}

	private @Nullable Text getProgressBarLabel() {
		if (bundleContents.isEmpty()) {
			return BUNDLE_EMPTY;
		}

		return bundleContents.getOccupancy().compareTo(Fraction.ONE) >= 0 ? BUNDLE_FULL : null;
	}
}
