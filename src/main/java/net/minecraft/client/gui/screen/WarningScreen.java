package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для экранов предупреждений с прокручиваемым текстом и опциональным чекбоксом.
 */
@Environment(EnvType.CLIENT)
public abstract class WarningScreen extends Screen {

	private static final int CONTENT_WIDTH = 100;
	private final Text messageText;
	private final @Nullable Text checkMessage;
	private final Text narratedText;
	protected @Nullable CheckboxWidget checkbox;
	private @Nullable ScrollableTextWidget textWidget;
	private final SimplePositioningWidget positioningWidget;

	protected WarningScreen(Text header, Text message, Text narratedText) {
		this(header, message, null, narratedText);
	}

	protected WarningScreen(Text header, Text messageText, @Nullable Text checkMessage, Text narratedText) {
		super(header);
		this.messageText = messageText;
		this.checkMessage = checkMessage;
		this.narratedText = narratedText;
		positioningWidget = new SimplePositioningWidget(0, 0, width, height);
	}

	protected abstract LayoutWidget getLayout();

	@Override
	protected void init() {
		DirectionalLayoutWidget contentLayout = positioningWidget.add(DirectionalLayoutWidget.vertical().spacing(8));
		contentLayout.getMainPositioner().alignHorizontalCenter();
		contentLayout.add(new TextWidget(getTitle(), textRenderer));
		textWidget = contentLayout.add(
				new ScrollableTextWidget(
						0,
						0,
						width - CONTENT_WIDTH,
						height - CONTENT_WIDTH,
						messageText,
						textRenderer
				), positioner -> positioner.margin(12)
		);

		DirectionalLayoutWidget bottomLayout = contentLayout.add(DirectionalLayoutWidget.vertical().spacing(8));
		bottomLayout.getMainPositioner().alignHorizontalCenter();

		if (checkMessage != null) {
			checkbox = bottomLayout.add(CheckboxWidget.builder(checkMessage, textRenderer).build());
		}

		bottomLayout.add(getLayout());
		positioningWidget.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		if (textWidget != null) {
			textWidget.setWidth(width - CONTENT_WIDTH);
			textWidget.setHeight(height - CONTENT_WIDTH);
			textWidget.updateHeight();
		}

		positioningWidget.refreshPositions();
		SimplePositioningWidget.setPos(positioningWidget, getNavigationFocus());
	}

	@Override
	public Text getNarratedTitle() {
		return narratedText;
	}
}
