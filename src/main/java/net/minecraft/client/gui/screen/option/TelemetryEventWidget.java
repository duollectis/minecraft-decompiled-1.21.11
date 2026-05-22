package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.session.telemetry.TelemetryEventProperty;
import net.minecraft.client.session.telemetry.TelemetryEventType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * Виджет отображения телеметрических событий — прокручиваемый список событий
 * с описанием их свойств и статуса (обязательное/опциональное/отключённое).
 */
@Environment(EnvType.CLIENT)
public class TelemetryEventWidget extends ScrollableTextFieldWidget {

	private static final int MARGIN_X = 32;
	private static final String REQUIRED_TRANSLATION_KEY = "telemetry.event.required";
	private static final String OPTIONAL_TRANSLATION_KEY = "telemetry.event.optional";
	private static final String DISABLED_TRANSLATION_KEY = "telemetry.event.optional.disabled";
	private static final Text PROPERTY_TITLE_TEXT =
			Text.translatable("telemetry_info.property_title").formatted(Formatting.UNDERLINE);

	private final TextRenderer textRenderer;
	private TelemetryEventWidget.Contents contents;
	private @Nullable DoubleConsumer scrollConsumer;

	public TelemetryEventWidget(int x, int y, int width, int height, TextRenderer textRenderer) {
		super(x, y, width, height, Text.empty());
		this.textRenderer = textRenderer;
		contents = collectContents(MinecraftClient.getInstance().isOptionalTelemetryEnabled());
	}

	/**
	 * Обновляет список событий в зависимости от того, включена ли опциональная телеметрия.
	 */
	public void refresh(boolean optionalTelemetryEnabled) {
		contents = collectContents(optionalTelemetryEnabled);
		refreshScroll();
	}

	public void initContents() {
		contents = collectContents(MinecraftClient.getInstance().isOptionalTelemetryEnabled());
		refreshScroll();
	}

	private TelemetryEventWidget.Contents collectContents(boolean optionalTelemetryEnabled) {
		TelemetryEventWidget.ContentsBuilder builder = new TelemetryEventWidget.ContentsBuilder(getGridWidth());
		List<TelemetryEventType> eventTypes = new ArrayList<>(TelemetryEventType.getTypes());
		eventTypes.sort(Comparator.comparing(TelemetryEventType::isOptional));

		for (int index = 0; index < eventTypes.size(); index++) {
			TelemetryEventType eventType = eventTypes.get(index);
			boolean disabled = eventType.isOptional() && !optionalTelemetryEnabled;
			appendEventInfo(builder, eventType, disabled);
			if (index < eventTypes.size() - 1) {
				builder.appendSpace(9);
			}
		}

		return builder.build();
	}

	public void setScrollConsumer(@Nullable DoubleConsumer scrollConsumer) {
		this.scrollConsumer = scrollConsumer;
	}

	@Override
	public void setScrollY(double scrollY) {
		super.setScrollY(scrollY);
		if (scrollConsumer != null) {
			scrollConsumer.accept(getScrollY());
		}
	}

	@Override
	protected int getContentsHeight() {
		return contents.grid().getHeight();
	}

	@Override
	protected double getDeltaYPerScroll() {
		return 9.0;
	}

	@Override
	protected void renderContents(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int textY = getTextY();
		int textX = getTextX();
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(textX, textY);
		contents.grid().forEachChild(widget -> widget.render(context, mouseX, mouseY, deltaTicks));
		context.getMatrices().popMatrix();
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, contents.narration());
	}

	private Text formatTitleText(Text title, boolean disabled) {
		return disabled ? title.copy().formatted(Formatting.GRAY) : title;
	}

	private void appendEventInfo(
			TelemetryEventWidget.ContentsBuilder builder,
			TelemetryEventType eventType,
			boolean disabled
	) {
		String translationKey = eventType.isOptional()
				? (disabled ? DISABLED_TRANSLATION_KEY : OPTIONAL_TRANSLATION_KEY)
				: REQUIRED_TRANSLATION_KEY;
		builder.appendText(textRenderer, formatTitleText(Text.translatable(translationKey, eventType.getTitle()), disabled));
		builder.appendText(textRenderer, eventType.getDescription().formatted(Formatting.GRAY));
		builder.appendSpace(9 / 2);
		builder.appendTitle(textRenderer, formatTitleText(PROPERTY_TITLE_TEXT, disabled), 2);
		appendProperties(eventType, builder, disabled);
	}

	private void appendProperties(
			TelemetryEventType eventType,
			TelemetryEventWidget.ContentsBuilder builder,
			boolean disabled
	) {
		for (TelemetryEventProperty<?> property : eventType.getProperties()) {
			builder.appendTitle(textRenderer, formatTitleText(property.getTitle(), disabled));
		}
	}

	private int getGridWidth() {
		return width - getPadding();
	}

	/**
	 * Иммутабельный снимок содержимого виджета: сетка виджетов и текст для нарратора.
	 */
	@Environment(EnvType.CLIENT)
	record Contents(LayoutWidget grid, Text narration) {
	}

	/**
	 * Построитель содержимого виджета телеметрии — накапливает виджеты и нарративный текст.
	 */
	@Environment(EnvType.CLIENT)
	static class ContentsBuilder {

		private final int gridWidth;
		private final DirectionalLayoutWidget layout;
		private final MutableText narration = Text.empty();

		public ContentsBuilder(int gridWidth) {
			this.gridWidth = gridWidth;
			layout = DirectionalLayoutWidget.vertical();
			layout.getMainPositioner().alignLeft();
			layout.add(EmptyWidget.ofWidth(gridWidth));
		}

		public void appendTitle(TextRenderer textRenderer, Text title) {
			appendTitle(textRenderer, title, 0);
		}

		public void appendTitle(TextRenderer textRenderer, Text title, int marginBottom) {
			layout.add(
					new MultilineTextWidget(title, textRenderer).setMaxWidth(gridWidth),
					positioner -> positioner.marginBottom(marginBottom)
			);
			narration.append(title).append("\n");
		}

		public void appendText(TextRenderer textRenderer, Text text) {
			layout.add(
					new MultilineTextWidget(text, textRenderer)
							.setMaxWidth(gridWidth - 64)
							.setCentered(true),
					positioner -> positioner.alignHorizontalCenter().marginX(MARGIN_X)
			);
			narration.append(text).append("\n");
		}

		public void appendSpace(int height) {
			layout.add(EmptyWidget.ofHeight(height));
		}

		public TelemetryEventWidget.Contents build() {
			layout.refreshPositions();
			return new TelemetryEventWidget.Contents(layout, narration);
		}
	}
}
