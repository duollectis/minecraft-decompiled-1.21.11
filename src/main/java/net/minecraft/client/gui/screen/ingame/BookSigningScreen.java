package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.StringHelper;

import java.util.List;
import java.util.Optional;

/**
 * Экран подписи книги. Позволяет ввести название книги перед её финализацией.
 */
@Environment(EnvType.CLIENT)
public class BookSigningScreen extends Screen {

	private static final Text EDIT_TITLE_TEXT = Text.translatable("book.editTitle");
	private static final Text FINALIZE_WARNING_TEXT = Text.translatable("book.finalizeWarning");
	private static final Text TITLE_TEXT = Text.translatable("book.sign.title");
	private static final Text TITLE_BOX_TEXT = Text.translatable("book.sign.titlebox");
	private static final int BOOK_WIDTH = 192;
	private static final int BOOK_Y = 2;
	private static final int TITLE_FIELD_WIDTH = 114;
	private static final int TITLE_FIELD_Y = 50;
	private static final int TITLE_FIELD_MAX_LENGTH = 15;
	private static final int BUTTON_Y = 196;
	private static final int BUTTON_WIDTH = 98;
	private static final int BUTTON_HEIGHT = 20;
	private static final int EDIT_TITLE_Y = 34;
	private static final int BYLINE_Y = 60;
	private static final int WARNING_Y = 82;
	private static final int TEXT_COLOR_BLACK = -16777216;
	private static final int TEXTURE_SIZE = 256;
	private static final int OFFHAND_SLOT = 40;

	private final BookEditScreen editScreen;
	private final PlayerEntity player;
	private final List<String> pages;
	private final Hand hand;
	private final Text bylineText;
	private TextFieldWidget bookTitleTextField;
	private String bookTitle = "";

	public BookSigningScreen(BookEditScreen editScreen, PlayerEntity player, Hand hand, List<String> pages) {
		super(TITLE_TEXT);
		this.editScreen = editScreen;
		this.player = player;
		this.hand = hand;
		this.pages = pages;
		bylineText = Text.translatable("book.byAuthor", player.getName()).formatted(Formatting.DARK_GRAY);
	}

	@Override
	protected void init() {
		ButtonWidget finalizeButton = ButtonWidget.builder(
			Text.translatable("book.finalizeButton"),
			button -> {
				onFinalize();
				client.setScreen(null);
			}
		).dimensions(width / 2 - 100, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
		finalizeButton.active = false;

		bookTitleTextField = addDrawableChild(new TextFieldWidget(
			client.textRenderer,
			(width - TITLE_FIELD_WIDTH) / 2 - 3,
			TITLE_FIELD_Y,
			TITLE_FIELD_WIDTH,
			BUTTON_HEIGHT,
			TITLE_BOX_TEXT
		));
		bookTitleTextField.setMaxLength(TITLE_FIELD_MAX_LENGTH);
		bookTitleTextField.setDrawsBackground(false);
		bookTitleTextField.setCentered(true);
		bookTitleTextField.setEditableColor(TEXT_COLOR_BLACK);
		bookTitleTextField.setTextShadow(false);
		bookTitleTextField.setChangedListener(title -> finalizeButton.active = !StringHelper.isBlank(title));
		bookTitleTextField.setText(bookTitle);

		addDrawableChild(finalizeButton);
		addDrawableChild(
			ButtonWidget.builder(ScreenTexts.CANCEL, button -> {
				bookTitle = bookTitleTextField.getText();
				client.setScreen(editScreen);
			}).dimensions(width / 2 + 2, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build()
		);
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(bookTitleTextField);
	}

	private void onFinalize() {
		int slot = hand == Hand.MAIN_HAND ? player.getInventory().getSelectedSlot() : OFFHAND_SLOT;
		client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(
			slot,
			pages,
			Optional.of(bookTitleTextField.getText().trim())
		));
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (bookTitleTextField.isFocused() && !bookTitleTextField.getText().isEmpty() && input.isEnter()) {
			onFinalize();
			client.setScreen(null);
			return true;
		}

		return super.keyPressed(input);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		int bookX = (width - BOOK_WIDTH) / 2;
		int titleWidth = textRenderer.getWidth(EDIT_TITLE_TEXT);
		context.drawText(textRenderer, EDIT_TITLE_TEXT, bookX + 36 + (TITLE_FIELD_WIDTH - titleWidth) / 2, EDIT_TITLE_Y, TEXT_COLOR_BLACK, false);
		int bylineWidth = textRenderer.getWidth(bylineText);
		context.drawText(textRenderer, bylineText, bookX + 36 + (TITLE_FIELD_WIDTH - bylineWidth) / 2, BYLINE_Y, TEXT_COLOR_BLACK, false);
		context.drawWrappedText(textRenderer, FINALIZE_WARNING_TEXT, bookX + 36, WARNING_Y, TITLE_FIELD_WIDTH, TEXT_COLOR_BLACK, false);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.renderBackground(context, mouseX, mouseY, deltaTicks);
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			BookScreen.BOOK_TEXTURE,
			(width - BOOK_WIDTH) / 2,
			BOOK_Y,
			0.0F,
			0.0F,
			BOOK_WIDTH,
			BOOK_WIDTH,
			TEXTURE_SIZE,
			TEXTURE_SIZE
		);
	}
}
