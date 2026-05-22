package net.minecraft.client.gui.screen.ingame;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

/**
 * Экран редактирования книги. Позволяет писать текст постранично,
 * перелистывать страницы и переходить к экрану подписи книги.
 */
@Environment(EnvType.CLIENT)
public class BookEditScreen extends Screen {

	public static final int MAX_TEXT_WIDTH = 114;
	public static final int MAX_TEXT_HEIGHT = 126;
	public static final int WIDTH = 192;
	public static final int HEIGHT = 192;
	public static final int TEXTURE_WIDTH = 256;
	public static final int TEXTURE_HEIGHT = 256;
	private static final int EDIT_BOX_MARGIN = 4;
	private static final int BUTTON_WIDTH = 98;
	private static final int PAGE_TURN_BUTTON_Y_OFFSET = 157;
	private static final int PREV_PAGE_BUTTON_X_OFFSET = 43;
	private static final int NEXT_PAGE_BUTTON_X_OFFSET = 116;
	private static final int PAGE_INDICATOR_Y_OFFSET = 16;
	private static final int PAGE_INDICATOR_X_OFFSET = 148;
	private static final int MAX_PAGES = 100;
	private static final int EDIT_BOX_WIDTH = 122;
	private static final int EDIT_BOX_HEIGHT = 134;
	private static final int EDIT_BOX_Y = 28;
	private static final int EDIT_BOX_MAX_LENGTH = 1024;
	private static final int TEXT_COLOR_BLACK = -16777216;
	private static final int KEY_PAGE_UP = 266;
	private static final int KEY_PAGE_DOWN = 267;
	private static final Text TITLE_TEXT = Text.translatable("book.edit.title");
	private static final Text SIGN_BUTTON_TEXT = Text.translatable("book.signButton");

	private final PlayerEntity player;
	private final ItemStack stack;
	private final BookSigningScreen signingScreen;
	private int currentPage;
	private final List<String> pages = Lists.newArrayList();
	private PageTurnWidget nextPageButton;
	private PageTurnWidget previousPageButton;
	private final Hand hand;
	private Text pageIndicatorText = ScreenTexts.EMPTY;
	private EditBoxWidget editBox;

	public BookEditScreen(
		PlayerEntity player,
		ItemStack stack,
		Hand hand,
		WritableBookContentComponent writableBookContent
	) {
		super(TITLE_TEXT);
		this.player = player;
		this.stack = stack;
		this.hand = hand;
		writableBookContent.stream(MinecraftClient.getInstance().shouldFilterText()).forEach(pages::add);

		if (pages.isEmpty()) {
			pages.add("");
		}

		signingScreen = new BookSigningScreen(this, player, hand, pages);
	}

	private int countPages() {
		return pages.size();
	}

	@Override
	protected void init() {
		int bookX = getBookX();
		int bookY = getBookY();

		editBox = EditBoxWidget.builder()
			.hasOverlay(false)
			.textColor(TEXT_COLOR_BLACK)
			.cursorColor(TEXT_COLOR_BLACK)
			.hasBackground(false)
			.textShadow(false)
			.x((width - MAX_TEXT_WIDTH) / 2 - EDIT_BOX_MARGIN * 2)
			.y(EDIT_BOX_Y)
			.build(textRenderer, EDIT_BOX_WIDTH, EDIT_BOX_HEIGHT, ScreenTexts.EMPTY);
		editBox.setMaxLength(EDIT_BOX_MAX_LENGTH);
		editBox.setMaxLines(MAX_TEXT_HEIGHT / 9);
		editBox.setChangeListener(page -> pages.set(currentPage, page));
		addDrawableChild(editBox);
		updatePage();
		pageIndicatorText = getPageIndicatorText();

		previousPageButton = addDrawableChild(new PageTurnWidget(
			bookX + PREV_PAGE_BUTTON_X_OFFSET,
			bookY + PAGE_TURN_BUTTON_Y_OFFSET,
			false,
			button -> openPreviousPage(),
			true
		));
		nextPageButton = addDrawableChild(new PageTurnWidget(
			bookX + NEXT_PAGE_BUTTON_X_OFFSET,
			bookY + PAGE_TURN_BUTTON_Y_OFFSET,
			true,
			button -> openNextPage(),
			true
		));
		addDrawableChild(
			ButtonWidget.builder(SIGN_BUTTON_TEXT, button -> client.setScreen(signingScreen))
				.position(width / 2 - BUTTON_WIDTH - 2, getButtonsY())
				.width(BUTTON_WIDTH)
				.build()
		);
		addDrawableChild(
			ButtonWidget.builder(ScreenTexts.DONE, button -> {
				client.setScreen(null);
				finalizeBook();
			})
				.position(width / 2 + 2, getButtonsY())
				.width(BUTTON_WIDTH)
				.build()
		);
		updatePreviousPageButtonVisibility();
	}

	private int getBookX() {
		return (width - WIDTH) / 2;
	}

	private int getBookY() {
		return 2;
	}

	private int getButtonsY() {
		return getBookY() + WIDTH + 2;
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(editBox);
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(super.getNarratedTitle(), getPageIndicatorText());
	}

	private Text getPageIndicatorText() {
		return Text.translatable("book.pageIndicator", currentPage + 1, countPages())
			.withColor(TEXT_COLOR_BLACK)
			.withoutShadow();
	}

	private void openPreviousPage() {
		if (currentPage > 0) {
			currentPage--;
			updatePage();
		}

		updatePreviousPageButtonVisibility();
	}

	private void openNextPage() {
		if (currentPage < countPages() - 1) {
			currentPage++;
		} else {
			appendNewPage();

			if (currentPage < countPages() - 1) {
				currentPage++;
			}
		}

		updatePage();
		updatePreviousPageButtonVisibility();
	}

	private void updatePage() {
		editBox.setText(pages.get(currentPage), true);
		pageIndicatorText = getPageIndicatorText();
	}

	private void updatePreviousPageButtonVisibility() {
		previousPageButton.visible = currentPage > 0;
	}

	private void removeEmptyPages() {
		ListIterator<String> iterator = pages.listIterator(pages.size());

		while (iterator.hasPrevious() && iterator.previous().isEmpty()) {
			iterator.remove();
		}
	}

	private void finalizeBook() {
		removeEmptyPages();
		writeNbtData();
		int slot = hand == Hand.MAIN_HAND ? player.getInventory().getSelectedSlot() : 40;
		client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(slot, pages, Optional.empty()));
	}

	private void writeNbtData() {
		stack.set(
			DataComponentTypes.WRITABLE_BOOK_CONTENT,
			new WritableBookContentComponent(pages.stream().map(RawFilteredPair::of).toList())
		);
	}

	private void appendNewPage() {
		if (countPages() < MAX_PAGES) {
			pages.add("");
		}
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		switch (input.key()) {
			case KEY_PAGE_UP -> {
				previousPageButton.onPress(input);
				return true;
			}
			case KEY_PAGE_DOWN -> {
				nextPageButton.onPress(input);
				return true;
			}
			default -> {
				return super.keyPressed(input);
			}
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		renderPageContent(context.getTextConsumer());
	}

	private void renderPageContent(DrawnTextConsumer drawnTextConsumer) {
		int bookX = getBookX();
		int bookY = getBookY();
		drawnTextConsumer.text(Alignment.RIGHT, bookX + PAGE_INDICATOR_X_OFFSET, bookY + PAGE_INDICATOR_Y_OFFSET, pageIndicatorText);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.renderBackground(context, mouseX, mouseY, deltaTicks);
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			BookScreen.BOOK_TEXTURE,
			getBookX(),
			getBookY(),
			0.0F,
			0.0F,
			WIDTH,
			WIDTH,
			TEXTURE_WIDTH,
			TEXTURE_WIDTH
		);
	}
}
