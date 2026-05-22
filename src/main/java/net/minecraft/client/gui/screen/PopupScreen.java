package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Всплывающий экран поверх фонового экрана с заголовком, сообщением и кнопками выбора.
 * Фоновый экран продолжает рендериться позади попапа.
 */
@Environment(EnvType.CLIENT)
public class PopupScreen extends Screen {

	private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("popup/background");
	private static final int VERTICAL_SPACING = 12;
	private static final int MARGIN_WIDTH = 18;
	private static final int BUTTON_HORIZONTAL_SPACING = 6;
	private static final int IMAGE_WIDTH = 130;
	private static final int IMAGE_HEIGHT = 64;
	private static final int DEFAULT_WIDTH = 250;
	private static final int MAX_BUTTON_WIDTH = 150;
	private final Screen backgroundScreen;
	private final @Nullable Identifier image;
	private final Text message;
	private final List<PopupScreen.Button> buttons;
	private final @Nullable Runnable onClosed;
	private final int innerWidth;
	private final DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical();

	PopupScreen(
			Screen backgroundScreen,
			int width,
			@Nullable Identifier image,
			Text title,
			Text message,
			List<PopupScreen.Button> buttons,
			@Nullable Runnable onClosed
	) {
		super(title);
		this.backgroundScreen = backgroundScreen;
		this.image = image;
		this.message = message;
		this.buttons = buttons;
		this.onClosed = onClosed;
		innerWidth = width - 36;
	}

	@Override
	public void onDisplayed() {
		super.onDisplayed();
		backgroundScreen.blur();
	}

	@Override
	protected void init() {
		backgroundScreen.init(width, height);
		layout.spacing(VERTICAL_SPACING).getMainPositioner().alignHorizontalCenter();
		layout.add(
				new MultilineTextWidget(title.copy().formatted(Formatting.BOLD), textRenderer)
						.setMaxWidth(innerWidth)
						.setCentered(true)
		);

		if (image != null) {
			layout.add(IconWidget.create(IMAGE_WIDTH, IMAGE_HEIGHT, image, IMAGE_WIDTH, IMAGE_HEIGHT));
		}

		layout.add(
				new MultilineTextWidget(message, textRenderer)
						.setMaxWidth(innerWidth)
						.setCentered(true)
		);
		layout.add(createButtonLayout());
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	private DirectionalLayoutWidget createButtonLayout() {
		int totalSpacing = BUTTON_HORIZONTAL_SPACING * (buttons.size() - 1);
		int buttonWidth = Math.min((innerWidth - totalSpacing) / buttons.size(), MAX_BUTTON_WIDTH);
		DirectionalLayoutWidget buttonRow = DirectionalLayoutWidget.horizontal();
		buttonRow.spacing(BUTTON_HORIZONTAL_SPACING);

		for (PopupScreen.Button button : buttons) {
			buttonRow.add(ButtonWidget
					.builder(button.message(), pressed -> button.action().accept(this))
					.width(buttonWidth)
					.build());
		}

		return buttonRow;
	}

	@Override
	protected void refreshWidgetPositions() {
		backgroundScreen.resize(width, height);
		layout.refreshPositions();
		SimplePositioningWidget.setPos(layout, getNavigationFocus());
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		backgroundScreen.renderBackground(context, mouseX, mouseY, deltaTicks);
		context.createNewRootLayer();
		backgroundScreen.render(context, -1, -1, deltaTicks);
		context.createNewRootLayer();
		renderInGameBackground(context);
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				BACKGROUND_TEXTURE,
				layout.getX() - MARGIN_WIDTH,
				layout.getY() - MARGIN_WIDTH,
				layout.getWidth() + 36,
				layout.getHeight() + 36
		);
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(title, message);
	}

	@Override
	public void close() {
		if (onClosed != null) {
			onClosed.run();
		}

		client.setScreen(backgroundScreen);
	}

	/**
	 * Строитель всплывающего экрана. Требует хотя бы одну кнопку перед вызовом {@link Builder#build()}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final Screen backgroundScreen;
		private final Text title;
		private Text message = ScreenTexts.EMPTY;
		private int width = DEFAULT_WIDTH;
		private @Nullable Identifier image;
		private final List<PopupScreen.Button> buttons = new ArrayList<>();
		private @Nullable Runnable onClosed = null;

		public Builder(Screen backgroundScreen, Text title) {
			this.backgroundScreen = backgroundScreen;
			this.title = title;
		}

		public PopupScreen.Builder width(int width) {
			this.width = width;
			return this;
		}

		public PopupScreen.Builder image(Identifier image) {
			this.image = image;
			return this;
		}

		public PopupScreen.Builder message(Text message) {
			this.message = message;
			return this;
		}

		public PopupScreen.Builder button(Text message, Consumer<PopupScreen> action) {
			buttons.add(new PopupScreen.Button(message, action));
			return this;
		}

		public PopupScreen.Builder onClosed(Runnable onClosed) {
			this.onClosed = onClosed;
			return this;
		}

		public PopupScreen build() {
			if (buttons.isEmpty()) {
				throw new IllegalStateException("Popup must have at least one button");
			}

			return new PopupScreen(backgroundScreen, width, image, title, message, List.copyOf(buttons), onClosed);
		}
	}

	@Environment(EnvType.CLIENT)
	record Button(Text message, Consumer<PopupScreen> action) {}
}
