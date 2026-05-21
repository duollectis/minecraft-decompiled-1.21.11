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

@Environment(EnvType.CLIENT)
/**
 * {@code BookScreen}.
 */
public class BookScreen extends Screen {

	public static final int PAGE_INDICATOR_Y_OFFSET = 16;
	public static final int TEXT_X_OFFSET = 36;
	public static final int TEXT_Y_OFFSET = 30;
	private static final int TEXTURE_WIDTH = 256;
	private static final int TEXTURE_HEIGHT = 256;
	private static final Text TITLE_TEXT = Text.translatable("book.view.title");
	private static final Style TEXT_STYLE = Style.EMPTY.withoutShadow().withColor(-16777216);
	public static final BookScreen.Contents EMPTY_PROVIDER = new BookScreen.Contents(List.of());
	public static final Identifier BOOK_TEXTURE = Identifier.ofVanilla("textures/gui/book.png");
	protected static final int MAX_TEXT_WIDTH = 114;
	protected static final int MAX_TEXT_HEIGHT = 128;
	protected static final int WIDTH = 192;
	private static final int PAGE_INDICATOR_X_OFFSET = 148;
	protected static final int HEIGHT = 192;
	private static final int PAGE_TURN_BUTTON_Y_OFFSET = 157;
	private static final int PREV_PAGE_BUTTON_X_OFFSET = 43;
	private static final int NEXT_PAGE_BUTTON_X_OFFSET = 116;
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
		this.pageTurnSound = playPageTurnSound;
	}

	public void setPageProvider(BookScreen.Contents pageProvider) {
		this.contents = pageProvider;
		this.pageIndex = MathHelper.clamp(this.pageIndex, 0, pageProvider.getPageCount());
		this.updatePageButtons();
		this.cachedPageIndex = -1;
	}

	public boolean setPage(int index) {
		int i = MathHelper.clamp(index, 0, this.contents.getPageCount() - 1);
		if (i != this.pageIndex) {
			this.pageIndex = i;
			this.updatePageButtons();
			this.cachedPageIndex = -1;
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Jump to page.
	 *
	 * @param page page
	 *
	 * @return boolean — результат операции
	 */
	protected boolean jumpToPage(int page) {
		return this.setPage(page);
	}

	@Override
	protected void init() {
		this.addCloseButton();
		this.addPageButtons();
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinLines(
				super.getNarratedTitle(),
				this.getPageIndicatorText(),
				this.contents.getPage(this.pageIndex)
		);
	}

	private Text getPageIndicatorText() {
		return Text
				.translatable("book.pageIndicator", this.pageIndex + 1, Math.max(this.getPageCount(), 1))
				.fillStyle(TEXT_STYLE);
	}

	/**
	 * Добавляет close button.
	 */
	protected void addCloseButton() {
		this.addDrawableChild(
				ButtonWidget
						.builder(ScreenTexts.DONE, button -> this.close())
						.position((this.width - 200) / 2, this.getCloseButtonY())
						.width(200)
						.build()
		);
	}

	/**
	 * Добавляет page buttons.
	 */
	protected void addPageButtons() {
		int i = this.getBookX();
		int j = this.getBookY();
		this.nextPageButton =
				this.addDrawableChild(new PageTurnWidget(
						i + 116,
						j + 157,
						true,
						button -> this.goToNextPage(),
						this.pageTurnSound
				));
		this.previousPageButton =
				this.addDrawableChild(new PageTurnWidget(
						i + 43,
						j + 157,
						false,
						button -> this.goToPreviousPage(),
						this.pageTurnSound
				));
		this.updatePageButtons();
	}

	private int getPageCount() {
		return this.contents.getPageCount();
	}

	/**
	 * Go to previous page.
	 */
	protected void goToPreviousPage() {
		if (this.pageIndex > 0) {
			this.pageIndex--;
		}

		this.updatePageButtons();
	}

	/**
	 * Go to next page.
	 */
	protected void goToNextPage() {
		if (this.pageIndex < this.getPageCount() - 1) {
			this.pageIndex++;
		}

		this.updatePageButtons();
	}

	private void updatePageButtons() {
		this.nextPageButton.visible = this.pageIndex < this.getPageCount() - 1;
		this.previousPageButton.visible = this.pageIndex > 0;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (super.keyPressed(input)) {
			return true;
		}
		else {
			return switch (input.key()) {
				case 266 -> {
					this.previousPageButton.onPress(input);
					yield true;
				}
				case 267 -> {
					this.nextPageButton.onPress(input);
					yield true;
				}
				default -> false;
			};
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		this.renderPageContent(context.getTextConsumer(DrawContext.HoverType.TOOLTIP_AND_CURSOR), false);
	}

	private void renderPageContent(DrawnTextConsumer drawer, boolean bl) {
		if (this.cachedPageIndex != this.pageIndex) {
			StringVisitable stringVisitable = Texts.withStyle(this.contents.getPage(this.pageIndex), TEXT_STYLE);
			this.cachedPage = this.textRenderer.wrapLines(stringVisitable, 114);
			this.pageIndexText = this.getPageIndicatorText();
			this.cachedPageIndex = this.pageIndex;
		}

		int i = this.getBookX();
		int j = this.getBookY();
		if (!bl) {
			drawer.text(Alignment.RIGHT, i + 148, j + 16, this.pageIndexText);
		}

		int k = Math.min(128 / 9, this.cachedPage.size());

		for (int l = 0; l < k; l++) {
			OrderedText orderedText = this.cachedPage.get(l);
			drawer.text(i + 36, j + 30 + l * 9, orderedText);
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.renderBackground(context, mouseX, mouseY, deltaTicks);
		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				BOOK_TEXTURE,
				this.getBookX(),
				this.getBookY(),
				0.0F,
				0.0F,
				192,
				192,
				256,
				256
		);
	}

	private int getBookX() {
		return (this.width - 192) / 2;
	}

	private int getBookY() {
		return 2;
	}

	protected int getCloseButtonY() {
		return this.getBookY() + 192 + 2;
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0) {
			DrawnTextConsumer.ClickHandler
					clickHandler =
					new DrawnTextConsumer.ClickHandler(this.textRenderer, (int) click.x(), (int) click.y());
			this.renderPageContent(clickHandler, true);
			Style style = clickHandler.getStyle();
			if (style != null && this.handleClickEvent(style.getClickEvent())) {
				return true;
			}
		}

		return super.mouseClicked(click, doubled);
	}

	/**
	 * Обрабатывает click event.
	 *
	 * @param clickEvent click event
	 *
	 * @return boolean — результат операции
	 */
	protected boolean handleClickEvent(@Nullable ClickEvent clickEvent) {
		if (clickEvent == null) {
			return false;
		}
		else {
			ClientPlayerEntity clientPlayerEntity = Objects.requireNonNull(this.client.player, "Player not available");
			switch (clickEvent) {
				case ClickEvent.ChangePage(int var12):
					this.jumpToPage(var12 - 1);
					break;
				case ClickEvent.RunCommand(String var9):
					this.closeScreen();
					handleRunCommand(clientPlayerEntity, var9, null);
					break;
				default:
					handleClickEvent(clickEvent, this.client, this);
			}

			return true;
		}
	}

	/**
	 * Закрывает screen.
	 */
	protected void closeScreen() {
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Contents}.
	 */
	public record Contents(List<Text> pages) {

		public int getPageCount() {
			return this.pages.size();
		}

		public Text getPage(int index) {
			return index >= 0 && index < this.getPageCount() ? this.pages.get(index) : ScreenTexts.EMPTY;
		}

		public static BookScreen.@Nullable Contents create(ItemStack stack) {
			boolean bl = MinecraftClient.getInstance().shouldFilterText();
			WrittenBookContentComponent
					writtenBookContentComponent =
					stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
			if (writtenBookContentComponent != null) {
				return new BookScreen.Contents(writtenBookContentComponent.getPages(bl));
			}
			else {
				WritableBookContentComponent
						writableBookContentComponent =
						stack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
				return writableBookContentComponent != null
				       ? new BookScreen.Contents((List<Text>) (List<?>) writableBookContentComponent
				                                                        .stream(bl)
				                                                        .map(Text::literal)
				                                                        .toList()) : null;
			}
		}
	}
}
