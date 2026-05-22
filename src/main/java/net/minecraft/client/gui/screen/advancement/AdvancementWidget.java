package net.minecraft.client.gui.screen.advancement;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Виджет одного достижения в дереве вкладки достижений.
 * Отвечает за отрисовку рамки, иконки, линий связи и всплывающей подсказки.
 */
@Environment(EnvType.CLIENT)
public class AdvancementWidget {

	private static final Identifier TITLE_BOX_TEXTURE = Identifier.ofVanilla("advancements/title_box");
	private static final int WIDGET_HEIGHT = 26;
	private static final int MAX_WIDGET_WIDTH = 200;
	private static final int ICON_OFFSET_X = 8;
	private static final int ICON_OFFSET_Y = 5;
	private static final int PROGRESS_PADDING = 3;
	private static final int DESCRIPTION_PADDING = 5;
	private static final int TITLE_OFFSET_X = 32;
	private static final int TITLE_MAX_WIDTH = 163;
	private static final int MIN_DESCRIPTION_WIDTH = 80;
	private static final int LINE_HEIGHT = 9;
	private static final int TITLE_BOX_PADDING = 8;
	private static final int DESCRIPTION_BOX_PADDING = 6;
	private static final int DESCRIPTION_WRAP_THRESHOLD = 10;
	private static final int PAGE_HEIGHT = 113;
	private static final int[] SPLIT_OFFSET_CANDIDATES = new int[]{0, 10, -10, 25, -25};
	private static final int COLOR_WHITE = -1;
	private static final int COLOR_GREEN = -16711936;

	private final AdvancementTab tab;
	private final PlacedAdvancement advancement;
	private final AdvancementDisplay display;
	private final List<OrderedText> title;
	private final int width;
	private final List<OrderedText> description;
	private final MinecraftClient client;
	private @Nullable AdvancementWidget parent;
	private final List<AdvancementWidget> children = Lists.newArrayList();
	private @Nullable AdvancementProgress progress;
	private final int x;
	private final int y;

	public AdvancementWidget(
		AdvancementTab tab,
		MinecraftClient client,
		PlacedAdvancement advancement,
		AdvancementDisplay display
	) {
		this.tab = tab;
		this.advancement = advancement;
		this.display = display;
		this.client = client;
		this.title = client.textRenderer.wrapLines(display.getTitle(), TITLE_MAX_WIDTH);
		x = MathHelper.floor(display.getX() * 28.0F);
		y = MathHelper.floor(display.getY() * 27.0F);
		int titleWidth = Math.max(title.stream().mapToInt(client.textRenderer::getWidth).max().orElse(0), MIN_DESCRIPTION_WIDTH);
		int progressWidth = getProgressWidth();
		int boxWidth = 29 + titleWidth + progressWidth;
		this.description = Language.getInstance()
			.reorder(wrapDescription(
				Texts.withStyle(
					display.getDescription(),
					Style.EMPTY.withColor(display.getFrame().getTitleFormat())
				), boxWidth
			));

		for (OrderedText line : this.description) {
			boxWidth = Math.max(boxWidth, client.textRenderer.getWidth(line));
		}

		width = boxWidth + PROGRESS_PADDING + DESCRIPTION_PADDING;
	}

	private int getProgressWidth() {
		int requirementCount = advancement.getAdvancement().requirements().getLength();
		if (requirementCount <= 1) {
			return 0;
		}

		Text progressText = Text.translatable("advancements.progress", requirementCount, requirementCount);
		return client.textRenderer.getWidth(progressText) + TITLE_BOX_PADDING;
	}

	private static float getMaxWidth(TextHandler textHandler, List<StringVisitable> lines) {
		return (float) lines.stream().mapToDouble(textHandler::getWidth).max().orElse(0.0);
	}

	private List<StringVisitable> wrapDescription(Text text, int boxWidth) {
		TextHandler textHandler = client.textRenderer.getTextHandler();
		List<StringVisitable> bestLines = null;
		float bestDelta = Float.MAX_VALUE;

		for (int offset : SPLIT_OFFSET_CANDIDATES) {
			List<StringVisitable> lines = textHandler.wrapLines(text, boxWidth - offset, Style.EMPTY);
			float delta = Math.abs(getMaxWidth(textHandler, lines) - boxWidth);
			if (delta <= DESCRIPTION_WRAP_THRESHOLD) {
				return lines;
			}

			if (delta < bestDelta) {
				bestDelta = delta;
				bestLines = lines;
			}
		}

		return bestLines;
	}

	private @Nullable AdvancementWidget getParent(PlacedAdvancement placed) {
		do {
			placed = placed.getParent();
		}
		while (placed != null && placed.getAdvancement().display().isEmpty());

		return placed != null && placed.getAdvancement().display().isPresent()
			? tab.getWidget(placed.getAdvancementEntry())
			: null;
	}

	public void renderLines(DrawContext context, int originX, int originY, boolean border) {
		if (parent != null) {
			int parentCenterX = originX + parent.x + 13;
			int parentRightX = originX + parent.x + 26 + 4;
			int parentCenterY = originY + parent.y + 13;
			int thisCenterX = originX + x + 13;
			int thisCenterY = originY + y + 13;
			int lineColor = border ? -16777216 : COLOR_WHITE;

			if (border) {
				context.drawHorizontalLine(parentRightX, parentCenterX, parentCenterY - 1, lineColor);
				context.drawHorizontalLine(parentRightX + 1, parentCenterX, parentCenterY, lineColor);
				context.drawHorizontalLine(parentRightX, parentCenterX, parentCenterY + 1, lineColor);
				context.drawHorizontalLine(thisCenterX, parentRightX - 1, thisCenterY - 1, lineColor);
				context.drawHorizontalLine(thisCenterX, parentRightX - 1, thisCenterY, lineColor);
				context.drawHorizontalLine(thisCenterX, parentRightX - 1, thisCenterY + 1, lineColor);
				context.drawVerticalLine(parentRightX - 1, thisCenterY, parentCenterY, lineColor);
				context.drawVerticalLine(parentRightX + 1, thisCenterY, parentCenterY, lineColor);
			}
			else {
				context.drawHorizontalLine(parentRightX, parentCenterX, parentCenterY, lineColor);
				context.drawHorizontalLine(thisCenterX, parentRightX, thisCenterY, lineColor);
				context.drawVerticalLine(parentRightX, thisCenterY, parentCenterY, lineColor);
			}
		}

		for (AdvancementWidget child : children) {
			child.renderLines(context, originX, originY, border);
		}
	}

	public void renderWidgets(DrawContext context, int originX, int originY) {
		boolean visible = !display.isHidden() || (progress != null && progress.isDone());
		if (visible) {
			float progressPct = progress == null ? 0.0F : progress.getProgressBarPercentage();
			AdvancementObtainedStatus status = progressPct >= 1.0F
				? AdvancementObtainedStatus.OBTAINED
				: AdvancementObtainedStatus.UNOBTAINED;
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				status.getFrameTexture(display.getFrame()),
				originX + x + PROGRESS_PADDING,
				originY + y,
				WIDGET_HEIGHT,
				WIDGET_HEIGHT
			);
			context.drawItemWithoutEntity(display.getIcon(), originX + x + ICON_OFFSET_X, originY + y + ICON_OFFSET_Y);
		}

		for (AdvancementWidget child : children) {
			child.renderWidgets(context, originX, originY);
		}
	}

	public int getWidth() {
		return width;
	}

	public void setProgress(AdvancementProgress progress) {
		this.progress = progress;
	}

	public void addChild(AdvancementWidget widget) {
		children.add(widget);
	}

	/**
	 * Отрисовывает всплывающую подсказку достижения с прогресс-баром, описанием и иконкой.
	 * Автоматически выбирает сторону отображения, чтобы не выйти за границы экрана.
	 *
	 * @param context контекст отрисовки
	 * @param originX смещение дерева по X
	 * @param originY смещение дерева по Y
	 * @param alpha прозрачность фона подсказки
	 * @param screenX абсолютная X-координата страницы
	 * @param screenY абсолютная Y-координата страницы
	 */
	public void drawTooltip(DrawContext context, int originX, int originY, float alpha, int screenX, int screenY) {
		TextRenderer textRenderer = client.textRenderer;
		int titleBoxHeight = LINE_HEIGHT * title.size() + LINE_HEIGHT + TITLE_BOX_PADDING;
		int boxTop = originY + y + (WIDGET_HEIGHT - titleBoxHeight) / 2;
		int boxBottom = boxTop + titleBoxHeight;
		int descLinesHeight = description.size() * LINE_HEIGHT;
		int descBoxHeight = DESCRIPTION_BOX_PADDING + descLinesHeight;
		boolean flipLeft = screenX + originX + x + width + WIDGET_HEIGHT >= tab.getScreen().width;
		Text progressFraction = progress == null ? null : progress.getProgressBarFraction();
		int progressTextWidth = progressFraction == null ? 0 : textRenderer.getWidth(progressFraction);
		boolean flipUp = boxBottom + descBoxHeight >= PAGE_HEIGHT;
		float progressPct = progress == null ? 0.0F : progress.getProgressBarPercentage();
		int progressBarWidth = MathHelper.floor(progressPct * width);

		AdvancementObtainedStatus leftStatus;
		AdvancementObtainedStatus rightStatus;
		AdvancementObtainedStatus frameStatus;

		if (progressPct >= 1.0F) {
			progressBarWidth = width / 2;
			leftStatus = AdvancementObtainedStatus.OBTAINED;
			rightStatus = AdvancementObtainedStatus.OBTAINED;
			frameStatus = AdvancementObtainedStatus.OBTAINED;
		}
		else if (progressBarWidth < 2) {
			progressBarWidth = width / 2;
			leftStatus = AdvancementObtainedStatus.UNOBTAINED;
			rightStatus = AdvancementObtainedStatus.UNOBTAINED;
			frameStatus = AdvancementObtainedStatus.UNOBTAINED;
		}
		else if (progressBarWidth > width - 2) {
			progressBarWidth = width / 2;
			leftStatus = AdvancementObtainedStatus.OBTAINED;
			rightStatus = AdvancementObtainedStatus.OBTAINED;
			frameStatus = AdvancementObtainedStatus.UNOBTAINED;
		}
		else {
			leftStatus = AdvancementObtainedStatus.OBTAINED;
			rightStatus = AdvancementObtainedStatus.UNOBTAINED;
			frameStatus = AdvancementObtainedStatus.UNOBTAINED;
		}

		int rightBarWidth = width - progressBarWidth;
		int boxLeft = flipLeft
			? originX + x - width + WIDGET_HEIGHT + DESCRIPTION_BOX_PADDING
			: originX + x;
		int totalBoxHeight = titleBoxHeight + descBoxHeight;

		if (!description.isEmpty()) {
			int boxY = flipUp ? boxBottom - totalBoxHeight : boxTop;
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, TITLE_BOX_TEXTURE, boxLeft, boxY, width, totalBoxHeight);
		}

		if (leftStatus != rightStatus) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				leftStatus.getBoxTexture(),
				MAX_WIDGET_WIDTH,
				titleBoxHeight,
				0,
				0,
				boxLeft,
				boxTop,
				progressBarWidth,
				titleBoxHeight
			);
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				rightStatus.getBoxTexture(),
				MAX_WIDGET_WIDTH,
				titleBoxHeight,
				MAX_WIDGET_WIDTH - rightBarWidth,
				0,
				boxLeft + progressBarWidth,
				boxTop,
				rightBarWidth,
				titleBoxHeight
			);
		}
		else {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				leftStatus.getBoxTexture(),
				boxLeft,
				boxTop,
				width,
				titleBoxHeight
			);
		}

		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			frameStatus.getFrameTexture(display.getFrame()),
			originX + x + PROGRESS_PADDING,
			originY + y,
			WIDGET_HEIGHT,
			WIDGET_HEIGHT
		);

		int textX = boxLeft + DESCRIPTION_PADDING;
		if (flipLeft) {
			drawText(context, title, textX, boxTop + LINE_HEIGHT, COLOR_WHITE);
			if (progressFraction != null) {
				context.drawTextWithShadow(textRenderer, progressFraction, originX + x - progressTextWidth, boxTop + LINE_HEIGHT, COLOR_WHITE);
			}
		}
		else {
			drawText(context, title, originX + x + TITLE_OFFSET_X, boxTop + LINE_HEIGHT, COLOR_WHITE);
			if (progressFraction != null) {
				context.drawTextWithShadow(textRenderer, progressFraction, originX + x + width - progressTextWidth - DESCRIPTION_PADDING, boxTop + LINE_HEIGHT, COLOR_WHITE);
			}
		}

		if (flipUp) {
			drawText(context, description, textX, boxTop - descLinesHeight + 1, COLOR_GREEN);
		}
		else {
			drawText(context, description, textX, boxBottom, COLOR_GREEN);
		}

		context.drawItemWithoutEntity(display.getIcon(), originX + x + ICON_OFFSET_X, originY + y + ICON_OFFSET_Y);
	}

	private void drawText(DrawContext context, List<OrderedText> lines, int x, int y, int color) {
		TextRenderer textRenderer = client.textRenderer;
		for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
			context.drawTextWithShadow(textRenderer, lines.get(lineIndex), x, y + lineIndex * LINE_HEIGHT, color);
		}
	}

	public boolean shouldRender(int originX, int originY, int mouseX, int mouseY) {
		boolean visible = !display.isHidden() || (progress != null && progress.isDone());
		if (!visible) {
			return false;
		}

		int left = originX + x;
		int right = left + WIDGET_HEIGHT;
		int top = originY + y;
		int bottom = top + WIDGET_HEIGHT;
		return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
	}

	public void addToTree() {
		if (parent == null && advancement.getParent() != null) {
			parent = getParent(advancement);
			if (parent != null) {
				parent.addChild(this);
			}
		}
	}

	public int getY() {
		return y;
	}

	public int getX() {
		return x;
	}
}
