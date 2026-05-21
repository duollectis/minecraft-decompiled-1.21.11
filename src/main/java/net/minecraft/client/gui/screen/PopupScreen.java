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

@Environment(EnvType.CLIENT)
/**
 * {@code PopupScreen}.
 */
public class PopupScreen extends Screen {

	private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("popup/background");
	private static final int VERTICAL_SPACING = 12;
	private static final int MARGIN_WIDTH = 18;
	private static final int BUTTON_HORIZONTAL_SPACING = 6;
	private static final int IMAGE_WIDTH = 130;
	private static final int IMAGE_HEIGHT = 64;
	private static final int DEFAULT_WIDTH = 250;
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
		this.innerWidth = width - 36;
	}

	@Override
	public void onDisplayed() {
		super.onDisplayed();
		this.backgroundScreen.blur();
	}

	@Override
	protected void init() {
		this.backgroundScreen.init(this.width, this.height);
		this.layout.spacing(12).getMainPositioner().alignHorizontalCenter();
		this.layout.add(new MultilineTextWidget(this.title.copy().formatted(Formatting.BOLD), this.textRenderer)
				.setMaxWidth(this.innerWidth)
				.setCentered(true));
		if (this.image != null) {
			this.layout.add(IconWidget.create(130, 64, this.image, 130, 64));
		}

		this.layout.add(new MultilineTextWidget(this.message, this.textRenderer)
				.setMaxWidth(this.innerWidth)
				.setCentered(true));
		this.layout.add(this.createButtonLayout());
		this.layout.forEachChild(child -> {
			ClickableWidget var10000 = this.addDrawableChild(child);
		});
		this.refreshWidgetPositions();
	}

	private DirectionalLayoutWidget createButtonLayout() {
		int i = 6 * (this.buttons.size() - 1);
		int j = Math.min((this.innerWidth - i) / this.buttons.size(), 150);
		DirectionalLayoutWidget directionalLayoutWidget = DirectionalLayoutWidget.horizontal();
		directionalLayoutWidget.spacing(6);

		for (PopupScreen.Button button : this.buttons) {
			directionalLayoutWidget.add(ButtonWidget
					.builder(button.message(), buttonx -> button.action().accept(this))
					.width(j)
					.build());
		}

		return directionalLayoutWidget;
	}

	@Override
	protected void refreshWidgetPositions() {
		this.backgroundScreen.resize(this.width, this.height);
		this.layout.refreshPositions();
		SimplePositioningWidget.setPos(this.layout, this.getNavigationFocus());
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		this.backgroundScreen.renderBackground(context, mouseX, mouseY, deltaTicks);
		context.createNewRootLayer();
		this.backgroundScreen.render(context, -1, -1, deltaTicks);
		context.createNewRootLayer();
		this.renderInGameBackground(context);
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				BACKGROUND_TEXTURE,
				this.layout.getX() - 18,
				this.layout.getY() - 18,
				this.layout.getWidth() + 36,
				this.layout.getHeight() + 36
		);
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(this.title, this.message);
	}

	@Override
	public void close() {
		if (this.onClosed != null) {
			this.onClosed.run();
		}

		this.client.setScreen(this.backgroundScreen);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private final Screen backgroundScreen;
		private final Text title;
		private Text message = ScreenTexts.EMPTY;
		private int width = 250;
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
			this.buttons.add(new PopupScreen.Button(message, action));
			return this;
		}

		public PopupScreen.Builder onClosed(Runnable onClosed) {
			this.onClosed = onClosed;
			return this;
		}

		public PopupScreen build() {
			if (this.buttons.isEmpty()) {
				throw new IllegalStateException("Popup must have at least one button");
			}
			else {
				return new PopupScreen(
						this.backgroundScreen,
						this.width,
						this.image,
						this.title,
						this.message,
						List.copyOf(this.buttons),
						this.onClosed
				);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Button}.
	 */
	record Button(Text message, Consumer<PopupScreen> action) {
	}
}
