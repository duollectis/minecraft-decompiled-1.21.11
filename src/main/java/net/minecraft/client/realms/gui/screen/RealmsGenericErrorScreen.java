package net.minecraft.client.realms.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.RealmsError;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsGenericErrorScreen}.
 */
public class RealmsGenericErrorScreen extends RealmsScreen {

	private static final Text DEFAULT_ERROR_TITLE = Text.translatable("mco.errorMessage.generic");
	private final Screen parent;
	private final Text errorDetail;
	private MultilineText errorDetailMultiline = MultilineText.EMPTY;

	public RealmsGenericErrorScreen(RealmsServiceException realmsServiceException, Screen parent) {
		this(RealmsGenericErrorScreen.ErrorMessages.fromException(realmsServiceException), parent);
	}

	public RealmsGenericErrorScreen(Text description, Screen parent) {
		this(new RealmsGenericErrorScreen.ErrorMessages(DEFAULT_ERROR_TITLE, description), parent);
	}

	public RealmsGenericErrorScreen(Text title, Text description, Screen parent) {
		this(new RealmsGenericErrorScreen.ErrorMessages(title, description), parent);
	}

	private RealmsGenericErrorScreen(RealmsGenericErrorScreen.ErrorMessages errorMessages, Screen screen) {
		super(errorMessages.title);
		this.parent = screen;
		this.errorDetail = Texts.withStyle(errorMessages.detail, Style.EMPTY.withColor(-2142128));
	}

	@Override
	public void init() {
		this.addDrawableChild(ButtonWidget
				.builder(ScreenTexts.OK, button -> this.close())
				.dimensions(this.width / 2 - 100, this.height - 52, 200, 20)
				.build());
		this.errorDetailMultiline = MultilineText.create(this.textRenderer, this.errorDetail, this.width * 3 / 4);
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(super.getNarratedTitle(), this.errorDetail);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 80, -1);
		DrawnTextConsumer drawnTextConsumer = context.getTextConsumer();
		this.errorDetailMultiline.draw(Alignment.CENTER, this.width / 2, 100, 9, drawnTextConsumer);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ErrorMessages}.
	 */
	record ErrorMessages(Text title, Text detail) {

		static RealmsGenericErrorScreen.ErrorMessages fromException(RealmsServiceException realmsServiceException) {
			RealmsError realmsError = realmsServiceException.error;
			return new RealmsGenericErrorScreen.ErrorMessages(
					Text.translatable("mco.errorMessage.realmsService.realmsError", realmsError.getErrorCode()),
					realmsError.getText()
			);
		}
	}
}
