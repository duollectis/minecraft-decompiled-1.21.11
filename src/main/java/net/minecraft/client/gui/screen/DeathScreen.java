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

@Environment(EnvType.CLIENT)
/**
 * {@code DeathScreen}.
 */
public class DeathScreen extends Screen {

	private static final int BUTTON_COLUMNS = 2;
	private static final Identifier DRAFT_REPORT_ICON_TEXTURE = Identifier.ofVanilla("icon/draft_report");
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
		Text text = Text.literal(Integer.toString(decedent.getScore())).formatted(Formatting.YELLOW);
		this.scoreText = Text.translatable("deathScreen.score.value", text);
	}

	@Override
	protected void init() {
		this.ticksSinceDeath = 0;
		this.buttons.clear();
		Text
				text =
				this.isHardcore ? Text.translatable("deathScreen.spectate") : Text.translatable("deathScreen.respawn");
		this.buttons.add(this.addDrawableChild(ButtonWidget.builder(
				text, button -> {
					this.decedent.requestRespawn();
					button.active = false;
				}
		).dimensions(this.width / 2 - 100, this.height / 4 + 72, 200, 20).build()));
		this.titleScreenButton = this.addDrawableChild(
				ButtonWidget.builder(
						            Text.translatable("deathScreen.titleScreen"),
						            button -> this.client
								            .getAbuseReportContext()
								            .tryShowDraftScreen(this.client, this, this::onTitleScreenButtonClicked, true)
				            )
				            .dimensions(this.width / 2 - 100, this.height / 4 + 96, 200, 20)
				            .build()
		);
		this.buttons.add(this.titleScreenButton);
		this.setButtonsActive(false);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	private void onTitleScreenButtonClicked() {
		if (this.isHardcore) {
			this.quitLevel();
		}
		else {
			ConfirmScreen confirmScreen = new DeathScreen.TitleScreenConfirmScreen(
					confirmed -> {
						if (confirmed) {
							this.quitLevel();
						}
						else {
							this.decedent.requestRespawn();
							this.client.setScreen(null);
						}
					},
					Text.translatable("deathScreen.quit.confirm"),
					ScreenTexts.EMPTY,
					Text.translatable("deathScreen.titleScreen"),
					Text.translatable("deathScreen.respawn")
			);
			this.client.setScreen(confirmScreen);
			confirmScreen.disableButtons(20);
		}
	}

	private void quitLevel() {
		if (this.client.world != null) {
			this.client.world.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT);
		}

		this.client.disconnectWithSavingScreen();
		this.client.setScreen(new TitleScreen());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		this.drawTitles(context.getTextConsumer(DrawContext.HoverType.TOOLTIP_AND_CURSOR));
		if (this.titleScreenButton != null && this.client.getAbuseReportContext().hasDraft()) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					DRAFT_REPORT_ICON_TEXTURE,
					this.titleScreenButton.getX() + this.titleScreenButton.getWidth() - 17,
					this.titleScreenButton.getY() + 3,
					15,
					15
			);
		}
	}

	private void drawTitles(DrawnTextConsumer drawer) {
		DrawnTextConsumer.Transformation transformation = drawer.getTransformation();
		int i = this.width / 2;
		drawer.setTransformation(transformation.scaled(2.0F));
		drawer.text(Alignment.CENTER, i / 2, 30, this.title);
		drawer.setTransformation(transformation);
		if (this.message != null) {
			drawer.text(Alignment.CENTER, i, 85, this.message);
		}

		drawer.text(Alignment.CENTER, i, 100, this.scoreText);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		fillBackgroundGradient(context, this.width, this.height);
	}

	static void fillBackgroundGradient(DrawContext context, int width, int height) {
		context.fillGradient(0, 0, width, height, 1615855616, -1602211792);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		DrawnTextConsumer.ClickHandler
				clickHandler =
				new DrawnTextConsumer.ClickHandler(this.getTextRenderer(), (int) click.x(), (int) click.y());
		this.drawTitles(clickHandler);
		Style style = clickHandler.getStyle();
		return style != null && style.getClickEvent() instanceof ClickEvent.OpenUrl openUrl
		       ? handleOpenUri(this.client, this, openUrl.uri())
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
		this.ticksSinceDeath++;
		if (this.ticksSinceDeath == 20) {
			this.setButtonsActive(true);
		}
	}

	private void setButtonsActive(boolean active) {
		for (ButtonWidget buttonWidget : this.buttons) {
			buttonWidget.active = active;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TitleScreenConfirmScreen}.
	 */
	public static class TitleScreenConfirmScreen extends ConfirmScreen {

		public TitleScreenConfirmScreen(
				BooleanConsumer booleanConsumer,
				Text text,
				Text text2,
				Text text3,
				Text text4
		) {
			super(booleanConsumer, text, text2, text3, text4);
		}

		@Override
		public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			DeathScreen.fillBackgroundGradient(context, this.width, this.height);
		}
	}
}
