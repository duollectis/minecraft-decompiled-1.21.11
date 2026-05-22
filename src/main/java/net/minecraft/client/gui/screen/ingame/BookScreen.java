package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Экран чтения книги. Отображает страницы книги с поддержкой кликабельных ссылок,
 * перелистывания и навигации по страницам через клавиши PageUp/PageDown.
 */
@Environment(EnvType.CLIENT)
public class BookScreen extends Screen {

	public static final int PAGE_INDICATOR_Y_OFFSET = 16;
	public static final int TEXT_X_OFFSET = 36;
	public static final int TEXT_Y_OFFSET = 30;
	private static final int TEXTURE_WIDTH = 256;
	private static final int TEXTURE_HEIGHT = 256;
	private static final int PAGE_INDICATOR_X_OFFSET = 148;
	private static final int PAGE_TURN_BUTTON_Y_OFFSET = 157;
	private static final int PREV_PAGE_BUTTON_X_OFFSET = 43;
	private static final int NEXT_PAGE_BUTTON_X_OFFSET = 116;
	private static final int CLOSE_BUTTON_WIDTH = 200;
	private static final int LINE_HEIGHT = 9;
	private static final int KEY_PAGE_UP = 266;
	private static final int KEY_PAGE_DOWN = 267;
	private static final int LEFT_MOUSE_BUTTON = 0;
	private static final Text TITLE_TEXT = Text.translatable("book.view.title");
	private static final Style TEXT_STYLE = Style.EMPTY.withoutShadow().withColor(-16777216);
	public static final BookScreen.Contents EMPTY_PROVIDER = new BookScreen.Contents(List.of());
	public static final Identifier BOOK_TEXTURE = Identifier.ofVanilla("textures/gui/book.png");
	protected static final int MAX_TEXT_WIDTH = 114;
	protected static final int MAX_TEXT_HEIGHT = 128;
	protected static final int WIDTH = 192;
	protected static final int HEIGHT = 192;

	private BookScreen.Contents contents;
	private int pageIndex;
	private List<OrderedText> cachedPage = Collections.emptyList();
	private int cachedPageIndex = -1;
	private Text pageIndexText = ScreenTexts.EMPTY;
	private PageTurnWidget nextPageButton;
	private PageTurnWidget previousPageButton;
	private final boolean pageTurnSound;

	public BookScreen(BookScreen.Contents pageProvider) {
		this(pageProvider, true);
	}

	public BookScreen() {
		this(EMPTY_PROVIDER, false);
	}

	private BookScreen(BookScreen.Contents contents, boolean playPageTurnSound) {
		super(TITLE_TEXT);
		this.contents = contents;
		pageTurnSound = playPageTurnSound;
	}

	public void setPageProvider(BookScreen.Contents pageProvider) {
		contents = pageProvider;
		pageIndex = MathHelper.clamp(pageIndex, 0, pageProvider.getPageCount());
		updatePageButtons();
		cachedPageIndex = -1;
	}

	public boolean setPage(int index) {
		int clampedIndex = MathHelper.clamp(index, 0, contents.getPageCount() - 1);

		if (clampedIndex == pageIndex) {
			return false;
		}

		pageIndex = clampedIndex;
		updatePageButtons();
		cachedPageIndex = -1;
		return true;
	}

	protected boolean jumpToPage(int page) {
		return setPage(page);
	}

	@Override
	protected void init() {
		addCloseButton();
		addPageButtons();
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinLines(
			super.getNarratedTitle(),
			getPageIndicatorText(),
			contents.getPage(pageIndex)
		);
	}

	private Text getPageIndicatorText() {
		return Text.translatable("book.pageIndicator", pageIndex + 1, Math.max(getPageCount(), 1))
			.fillStyle(TEXT_STYLE);
	}

	protected void addCloseButton() {
		addDrawableChild(
			ButtonWidget.builder(ScreenTexts.DONE, button -> close())
				.position((width - CLOSE_BUTTON_WIDTH) / 2, getCloseButtonY())
				.width(CLOSE_BUTTON_WIDTH)
				.build()
		);
	}

	protected void addPageButtons() {
		int bookX = getBookX();
		int bookY = getBookY();

		nextPageButton = addDrawableChild(new PageTurnWidget(
			bookX + NEXT_PAGE_BUTTON_X_OFFSET,
			bookY + PAGE_TURN_BUTTON_Y_OFFSET,
			true,
			button -> goToNextPage(),
			pageTurnSound
		));
		previousPageButton = addDrawableChild(new PageTurnWidget(
			bookX + PREV_PAGE_BUTTON_X_OFFSET,
			bookY + PAGE_TURN_BUTTON_Y_OFFSET,
			false,
			button -> goToPreviousPage(),
			pageTurnSound
		));
		updatePageButtons();
	}

	private int getPageCount() {
		return contents.getPageCount();
	}

	protected void goToPreviousPage() {
		if (pageIndex > 0) {
			pageIndex--;
		}

		updatePageButtons();
	}

	protected void goToNextPage() {
		if (pageIndex < getPageCount() - 1) {
			pageIndex++;
		}

		updatePageButtons();
	}

	private void updatePageButtons() {
		nextPageButton.visible = pageIndex < getPageCount() - 1;
		previousPageButton.visible = pageIndex > 0;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (super.keyPressed(input)) {
			return true;
		}

		return switch (input.key()) {
			case KEY_PAGE_UP -> {
				previousPageButton.onPress(input);
				yield true;
			}
			case KEY_PAGE_DOWN -> {
				nextPageButton.onPress(input);
				yield true;
			}
			default -> false;
		};
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		renderPageContent(context.getTextConsumer(DrawContext.HoverType.TOOLTIP_AND_CURSOR), false);
	}

	private void renderPageContent(DrawnTextConsumer drawer, boolean clickMode) {
		if (cachedPageIndex != pageIndex) {
			StringVisitable styledPage = Texts.withStyle(contents.getPage(pageIndex), TEXT_STYLE);
			cachedPage = textRenderer.wrapLines(styledPage, MAX_TEXT_WIDTH);
			pageIndexText = getPageIndicatorText();
			cachedPageIndex = pageIndex;
		}

		int bookX = getBookX();
		int bookY = getBookY();

		if (!clickMode) {
			drawer.text(Alignment.RIGHT, bookX + PAGE_INDICATOR_X_OFFSET, bookY + PAGE_INDICATOR_Y_OFFSET, pageIndexText);
		}

		int visibleLines = Math.min(MAX_TEXT_HEIGHT / LINE_HEIGHT, cachedPage.size());

		for (int lineIndex = 0; lineIndex < visibleLines; lineIndex++) {
			OrderedText line = cachedPage.get(lineIndex);
			drawer.text(bookX + TEXT_X_OFFSET, bookY + TEXT_Y_OFFSET + lineIndex * LINE_HEIGHT, line);
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.renderBackground(context, mouseX, mouseY, deltaTicks);
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			BOOK_TEXTURE,
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

	private int getBookX() {
		return (width - WIDTH) / 2;
	}

	private int getBookY() {
		return 2;
	}

	protected int getCloseButtonY() {
		return getBookY() + WIDTH + 2;
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == LEFT_MOUSE_BUTTON) {
			DrawnTextConsumer.ClickHandler clickHandler = new DrawnTextConsumer.ClickHandler(
				textRenderer,
				(int) click.x(),
				(int) click.y()
			);
			renderPageContent(clickHandler, true);
			Style style = clickHandler.getStyle();

			if (style != null && handleClickEvent(style.getClickEvent())) {
				return true;
			}
		}

		return super.mouseClicked(click, doubled);
	}

	/**
	 * Обрабатывает клик по тексту книги. Поддерживает переход на страницу,
	 * выполнение команды и стандартные события кликов.
	 */
	protected boolean handleClickEvent(@Nullable ClickEvent clickEvent) {
		if (clickEvent == null) {
			return false;
		}

		ClientPlayerEntity player = Objects.requireNonNull(client.player, "Player not available");

		switch (clickEvent) {
			case ClickEvent.ChangePage(int page) -> jumpToPage(page - 1);
			case ClickEvent.RunCommand(String command) -> {
				closeScreen();
				handleRunCommand(player, command, null);
			}
			default -> handleClickEvent(clickEvent, client, this);
		}

		return true;
	}

	protected void closeScreen() {
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}

	/**
	 * Провайдер страниц книги. Хранит список текстовых страниц и умеет
	 * создавать себя из компонентов написанной или записываемой книги.
	 */
	@Environment(EnvType.CLIENT)
	public record Contents(List<Text> pages) {

		public int getPageCount() {
			return pages.size();
		}

		public Text getPage(int index) {
			return index >= 0 && index < getPageCount() ? pages.get(index) : ScreenTexts.EMPTY;
		}

		public static BookScreen.@Nullable Contents create(ItemStack stack) {
			boolean filterText = MinecraftClient.getInstance().shouldFilterText();
			WrittenBookContentComponent writtenBook = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);

			if (writtenBook != null) {
				return new BookScreen.Contents(writtenBook.getPages(filterText));
			}

			WritableBookContentComponent writableBook = stack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);

			return writableBook != null
				? new BookScreen.Contents((List<Text>) (List<?>) writableBook.stream(filterText).map(Text::literal).toList())
				: null;
		}
	}
}
