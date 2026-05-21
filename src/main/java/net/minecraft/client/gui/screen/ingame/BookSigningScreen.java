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

@Environment(EnvType.CLIENT)
/**
 * {@code BookSigningScreen}.
 */
public class BookSigningScreen extends Screen {

	private static final Text EDIT_TITLE_TEXT = Text.translatable("book.editTitle");
	private static final Text FINALIZE_WARNING_TEXT = Text.translatable("book.finalizeWarning");
	private static final Text TITLE_TEXT = Text.translatable("book.sign.title");
	private static final Text TITLE_BOX_TEXT = Text.translatable("book.sign.titlebox");
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
		this.bylineText = Text.translatable("book.byAuthor", player.getName()).formatted(Formatting.DARK_GRAY);
	}

	@Override
	protected void init() {
		ButtonWidget buttonWidget = ButtonWidget.builder(
				Text.translatable("book.finalizeButton"), button -> {
					this.onFinalize();
					this.client.setScreen(null);
				}
		).dimensions(this.width / 2 - 100, 196, 98, 20).build();
		buttonWidget.active = false;
		this.bookTitleTextField =
				this.addDrawableChild(new TextFieldWidget(
						this.client.textRenderer,
						(this.width - 114) / 2 - 3,
						50,
						114,
						20,
						TITLE_BOX_TEXT
				));
		this.bookTitleTextField.setMaxLength(15);
		this.bookTitleTextField.setDrawsBackground(false);
		this.bookTitleTextField.setCentered(true);
		this.bookTitleTextField.setEditableColor(-16777216);
		this.bookTitleTextField.setTextShadow(false);
		this.bookTitleTextField.setChangedListener(bookTitle -> buttonWidget.active = !StringHelper.isBlank(bookTitle));
		this.bookTitleTextField.setText(this.bookTitle);
		this.addDrawableChild(buttonWidget);
		this.addDrawableChild(ButtonWidget.builder(
				ScreenTexts.CANCEL, button -> {
					this.bookTitle = this.bookTitleTextField.getText();
					this.client.setScreen(this.editScreen);
				}
		).dimensions(this.width / 2 + 2, 196, 98, 20).build());
	}

	@Override
	protected void setInitialFocus() {
		this.setInitialFocus(this.bookTitleTextField);
	}

	private void onFinalize() {
		int i = this.hand == Hand.MAIN_HAND ? this.player.getInventory().getSelectedSlot() : 40;
		this.client
				.getNetworkHandler()
				.sendPacket(new BookUpdateC2SPacket(
						i,
						this.pages,
						Optional.of(this.bookTitleTextField.getText().trim())
				));
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.bookTitleTextField.isFocused() && !this.bookTitleTextField.getText().isEmpty() && input.isEnter()) {
			this.onFinalize();
			this.client.setScreen(null);
			return true;
		}
		else {
			return super.keyPressed(input);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		int i = (this.width - 192) / 2;
		int j = 2;
		int k = this.textRenderer.getWidth(EDIT_TITLE_TEXT);
		context.drawText(this.textRenderer, EDIT_TITLE_TEXT, i + 36 + (114 - k) / 2, 34, -16777216, false);
		int l = this.textRenderer.getWidth(this.bylineText);
		context.drawText(this.textRenderer, this.bylineText, i + 36 + (114 - l) / 2, 60, -16777216, false);
		context.drawWrappedText(this.textRenderer, FINALIZE_WARNING_TEXT, i + 36, 82, 114, -16777216, false);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.renderBackground(context, mouseX, mouseY, deltaTicks);
		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				BookScreen.BOOK_TEXTURE,
				(this.width - 192) / 2,
				2,
				0.0F,
				0.0F,
				192,
				192,
				256,
				256
		);
	}
}
