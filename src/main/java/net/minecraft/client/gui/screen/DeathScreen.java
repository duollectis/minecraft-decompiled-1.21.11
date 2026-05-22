package net.minecraft.client.gui.screen;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Экран смерти игрока. В режиме хардкора предлагает перейти в режим наблюдателя,
 * в обычном режиме — возродиться или вернуться в главное меню.
 */
@Environment(EnvType.CLIENT)
public class DeathScreen extends Screen {

	private static final int BUTTON_COLUMNS = 2;
	private static final Identifier DRAFT_REPORT_ICON_TEXTURE = Identifier.ofVanilla("icon/draft_report");
	private static final int BUTTONS_ENABLE_DELAY = 20;
	private static final int TITLE_SCALE = 2;
	private static final int TITLE_Y = 30;
	private static final int MESSAGE_Y = 85;
	private static final int SCORE_Y = 100;

	private int ticksSinceDeath;
	private final @Nullable Text message;
	private final boolean isHardcore;
	private final ClientPlayerEntity decedent;
	private final Text scoreText;
	private final List<ButtonWidget> buttons = Lists.newArrayList();
	private @Nullable ButtonWidget titleScreenButton;

	public DeathScreen(@Nullable Text message, boolean isHardcore, ClientPlayerEntity decedent) {
		super(Text.translatable(isHardcore ? "deathScreen.title.hardcore" : "deathScreen.title"));
		this.message = message;
		this.isHardcore = isHardcore;
		this.decedent = decedent;
		Text scoreValue = Text.literal(Integer.toString(decedent.getScore())).formatted(Formatting.YELLOW);
		scoreText = Text.translatable("deathScreen.score.value", scoreValue);
	}

	@Override
	protected void init() {
		ticksSinceDeath = 0;
		buttons.clear();

		Text respawnText = isHardcore
				? Text.translatable("deathScreen.spectate")
				: Text.translatable("deathScreen.respawn");

		buttons.add(addDrawableChild(ButtonWidget.builder(
				respawnText, button -> {
					decedent.requestRespawn();
					button.active = false;
				}
		).dimensions(width / 2 - 100, height / 4 + 72, 200, 20).build()));

		titleScreenButton = addDrawableChild(
				ButtonWidget.builder(
						Text.translatable("deathScreen.titleScreen"),
						button -> client
								.getAbuseReportContext()
								.tryShowDraftScreen(client, this, this::onTitleScreenButtonClicked, true)
				).dimensions(width / 2 - 100, height / 4 + 96, 200, 20).build()
		);
		buttons.add(titleScreenButton);
		setButtonsActive(false);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	private void onTitleScreenButtonClicked() {
		if (isHardcore) {
			quitLevel();
			return;
		}

		ConfirmScreen confirmScreen = new TitleScreenConfirmScreen(
				confirmed -> {
					if (confirmed) {
						quitLevel();
					} else {
						decedent.requestRespawn();
						client.setScreen(null);
					}
				},
				Text.translatable("deathScreen.quit.confirm"),
				ScreenTexts.EMPTY,
				Text.translatable("deathScreen.titleScreen"),
				Text.translatable("deathScreen.respawn")
		);
		client.setScreen(confirmScreen);
		confirmScreen.disableButtons(20);
	}

	private void quitLevel() {
		if (client.world != null) {
			client.world.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT);
		}

		client.disconnectWithSavingScreen();
		client.setScreen(new TitleScreen());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		drawTitles(context.getTextConsumer(DrawContext.HoverType.TOOLTIP_AND_CURSOR));

		if (titleScreenButton != null && client.getAbuseReportContext().hasDraft()) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					DRAFT_REPORT_ICON_TEXTURE,
					titleScreenButton.getX() + titleScreenButton.getWidth() - 17,
					titleScreenButton.getY() + 3,
					15,
					15
			);
		}
	}

	private void drawTitles(DrawnTextConsumer drawer) {
		DrawnTextConsumer.Transformation transformation = drawer.getTransformation();
		int centerX = width / 2;

		drawer.setTransformation(transformation.scaled(TITLE_SCALE));
		drawer.text(Alignment.CENTER, centerX / TITLE_SCALE, TITLE_Y, title);
		drawer.setTransformation(transformation);

		if (message != null) {
			drawer.text(Alignment.CENTER, centerX, MESSAGE_Y, message);
		}

		drawer.text(Alignment.CENTER, centerX, SCORE_Y, scoreText);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		fillBackgroundGradient(context, width, height);
	}

	static void fillBackgroundGradient(DrawContext context, int width, int height) {
		context.fillGradient(0, 0, width, height, 1615855616, -1602211792);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		DrawnTextConsumer.ClickHandler clickHandler =
				new DrawnTextConsumer.ClickHandler(getTextRenderer(), (int) click.x(), (int) click.y());
		drawTitles(clickHandler);
		Style style = clickHandler.getStyle();

		return style != null && style.getClickEvent() instanceof ClickEvent.OpenUrl openUrl
				? handleOpenUri(client, this, openUrl.uri())
				: super.mouseClicked(click, doubled);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean keepOpenThroughPortal() {
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		ticksSinceDeath++;

		if (ticksSinceDeath == BUTTONS_ENABLE_DELAY) {
			setButtonsActive(true);
		}
	}

	private void setButtonsActive(boolean active) {
		for (ButtonWidget button : buttons) {
			button.active = active;
		}
	}

	/**
	 * Экран подтверждения выхода в главное меню с кастомным фоном смерти.
	 */
	@Environment(EnvType.CLIENT)
	public static class TitleScreenConfirmScreen extends ConfirmScreen {

		public TitleScreenConfirmScreen(
				BooleanConsumer callback,
				Text title,
				Text message,
				Text yesText,
				Text noText
		) {
			super(callback, title, message, yesText, noText);
		}

		@Override
		public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			DeathScreen.fillBackgroundGradient(context, width, height);
		}
	}
}
