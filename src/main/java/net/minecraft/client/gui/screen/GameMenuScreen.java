package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.toast.NowPlayingToast;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.dialog.Dialogs;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.DialogTags;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.ServerLinks;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Urls;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code GameMenuScreen}.
 */
public class GameMenuScreen extends Screen {

	private static final Identifier DRAFT_REPORT_ICON_TEXTURE = Identifier.ofVanilla("icon/draft_report");
	private static final int GRID_COLUMNS = 2;
	private static final int BUTTONS_TOP_MARGIN = 50;
	private static final int GRID_MARGIN = 4;
	private static final int WIDE_BUTTON_WIDTH = 204;
	private static final int NORMAL_BUTTON_WIDTH = 98;
	private static final Text RETURN_TO_GAME_TEXT = Text.translatable("menu.returnToGame");
	private static final Text ADVANCEMENTS_TEXT = Text.translatable("gui.advancements");
	private static final Text STATS_TEXT = Text.translatable("gui.stats");
	private static final Text SEND_FEEDBACK_TEXT = Text.translatable("menu.sendFeedback");
	private static final Text REPORT_BUGS_TEXT = Text.translatable("menu.reportBugs");
	private static final Text FEEDBACK_TEXT = Text.translatable("menu.feedback");
	private static final Text OPTIONS_TEXT = Text.translatable("menu.options");
	private static final Text SHARE_TO_LAN_TEXT = Text.translatable("menu.shareToLan");
	private static final Text PLAYER_REPORTING_TEXT = Text.translatable("menu.playerReporting");
	private static final Text GAME_TEXT = Text.translatable("menu.game");
	private static final Text PAUSED_TEXT = Text.translatable("menu.paused");
	private static final Tooltip CUSTOM_OPTIONS_TOOLTIP = Tooltip.of(Text.translatable("menu.custom_options.tooltip"));
	private final boolean showMenu;
	private @Nullable ButtonWidget exitButton;

	public GameMenuScreen(boolean showMenu) {
		super(showMenu ? GAME_TEXT : PAUSED_TEXT);
		this.showMenu = showMenu;
	}

	public boolean shouldShowMenu() {
		return this.showMenu;
	}

	@Override
	protected void init() {
		if (this.showMenu) {
			this.initWidgets();
		}

		int i = this.textRenderer.getWidth(this.title);
		this.addDrawableChild(new TextWidget(
				this.width / 2 - i / 2,
				this.showMenu ? 40 : 10,
				i,
				9,
				this.title,
				this.textRenderer
		));
	}

	private void initWidgets() {
		GridWidget gridWidget = new GridWidget();
		gridWidget.getMainPositioner().margin(4, 4, 4, 0);
		GridWidget.Adder adder = gridWidget.createAdder(2);
		adder.add(
				ButtonWidget.builder(
						RETURN_TO_GAME_TEXT, button -> {
							this.client.setScreen(null);
							this.client.mouse.lockCursor();
						}
				).width(204).build(), 2, gridWidget.copyPositioner().marginTop(50)
		);
		adder.add(this.createButton(
				ADVANCEMENTS_TEXT,
				() -> new AdvancementsScreen(this.client.player.networkHandler.getAdvancementHandler(), this)
		));
		adder.add(this.createButton(STATS_TEXT, () -> new StatsScreen(this, this.client.player.getStatHandler())));
		Optional<? extends RegistryEntry<Dialog>> optional = this.getCustomOptionsDialog();
		if (optional.isEmpty()) {
			addFeedbackAndBugsButtons(this, adder);
		}
		else {
			this.addFeedbackAndCustomOptionsButtons(this.client, (RegistryEntry<Dialog>) optional.get(), adder);
		}

		adder.add(this.createButton(OPTIONS_TEXT, () -> new OptionsScreen(this, this.client.options)));
		if (this.client.isIntegratedServerRunning() && !this.client.getServer().isRemote()) {
			adder.add(this.createButton(SHARE_TO_LAN_TEXT, () -> new OpenToLanScreen(this)));
		}
		else {
			adder.add(this.createButton(PLAYER_REPORTING_TEXT, () -> new SocialInteractionsScreen(this)));
		}

		this.exitButton = adder.add(
				ButtonWidget.builder(
						ScreenTexts.returnToMenuOrDisconnect(this.client.isInSingleplayer()), button -> {
							button.active = false;
							this.client
									.getAbuseReportContext()
									.tryShowDraftScreen(
											this.client,
											this,
											() -> this.client.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT),
											true
									);
						}
				).width(204).build(), 2
		);
		gridWidget.refreshPositions();
		SimplePositioningWidget.setPos(gridWidget, 0, 0, this.width, this.height, 0.5F, 0.25F);
		gridWidget.forEachChild(this::addDrawableChild);
	}

	private Optional<? extends RegistryEntry<Dialog>> getCustomOptionsDialog() {
		Registry<Dialog>
				registry =
				this.client.player.networkHandler.getRegistryManager().getOrThrow(RegistryKeys.DIALOG);
		Optional<? extends RegistryEntryList<Dialog>>
				optional =
				registry.getOptional(DialogTags.PAUSE_SCREEN_ADDITIONS);
		if (optional.isPresent()) {
			RegistryEntryList<Dialog> registryEntryList = (RegistryEntryList<Dialog>) optional.get();
			if (registryEntryList.size() > 0) {
				if (registryEntryList.size() == 1) {
					return Optional.of(registryEntryList.get(0));
				}

				return registry.getOptional(Dialogs.CUSTOM_OPTIONS);
			}
		}

		ServerLinks serverLinks = this.client.player.networkHandler.getServerLinks();
		return !serverLinks.isEmpty() ? registry.getOptional(Dialogs.SERVER_LINKS) : Optional.empty();
	}

	static void addFeedbackAndBugsButtons(Screen parentScreen, GridWidget.Adder gridAdder) {
		gridAdder.add(createUrlButton(
				parentScreen,
				SEND_FEEDBACK_TEXT,
				SharedConstants.getGameVersion().stable() ? Urls.JAVA_FEEDBACK : Urls.SNAPSHOT_FEEDBACK
		));
		gridAdder.add(createUrlButton(parentScreen, REPORT_BUGS_TEXT, Urls.SNAPSHOT_BUGS)).active =
				!SharedConstants.getGameVersion()
				                .dataVersion()
				                .isNotMainSeries();
	}

	private void addFeedbackAndCustomOptionsButtons(
			MinecraftClient client,
			RegistryEntry<Dialog> dialog,
			GridWidget.Adder gridAdder
	) {
		gridAdder.add(this.createButton(FEEDBACK_TEXT, () -> new GameMenuScreen.FeedbackScreen(this)));
		gridAdder.add(
				ButtonWidget
						.builder(
								dialog.value().common().getExternalTitle(),
								button -> client.player.networkHandler.showDialog(dialog, this)
						)
						.width(98)
						.tooltip(CUSTOM_OPTIONS_TOOLTIP)
						.build()
		);
	}

	@Override
	public void tick() {
		if (this.shouldShowNowPlayingToast()) {
			NowPlayingToast.tick();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		if (this.shouldShowNowPlayingToast()) {
			NowPlayingToast.draw(context, this.textRenderer);
		}

		if (this.showMenu && this.client.getAbuseReportContext().hasDraft() && this.exitButton != null) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					DRAFT_REPORT_ICON_TEXTURE,
					this.exitButton.getX() + this.exitButton.getWidth() - 17,
					this.exitButton.getY() + 3,
					15,
					15
			);
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (this.showMenu) {
			super.renderBackground(context, mouseX, mouseY, deltaTicks);
		}
	}

	public boolean shouldShowNowPlayingToast() {
		GameOptions gameOptions = this.client.options;
		return gameOptions.getMusicToast().getValue().canShow()
				&& gameOptions.getSoundVolume(SoundCategory.MUSIC) > 0.0F && this.showMenu;
	}

	private ButtonWidget createButton(Text text, Supplier<Screen> screenSupplier) {
		return ButtonWidget.builder(text, button -> this.client.setScreen(screenSupplier.get())).width(98).build();
	}

	private static ButtonWidget createUrlButton(Screen parent, Text text, URI uri) {
		return ButtonWidget.builder(text, ConfirmLinkScreen.opening(parent, uri)).width(98).build();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FeedbackScreen}.
	 */
	static class FeedbackScreen extends Screen {

		private static final Text TITLE = Text.translatable("menu.feedback.title");
		public final Screen parent;
		private final ThreePartsLayoutWidget layoutWidget = new ThreePartsLayoutWidget(this);

		protected FeedbackScreen(Screen parent) {
			super(TITLE);
			this.parent = parent;
		}

		@Override
		protected void init() {
			this.layoutWidget.addHeader(TITLE, this.textRenderer);
			GridWidget gridWidget = this.layoutWidget.addBody(new GridWidget());
			gridWidget.getMainPositioner().margin(4, 4, 4, 0);
			GridWidget.Adder adder = gridWidget.createAdder(2);
			GameMenuScreen.addFeedbackAndBugsButtons(this, adder);
			this.layoutWidget.addFooter(ButtonWidget
					.builder(ScreenTexts.BACK, button -> this.close())
					.width(200)
					.build());
			this.layoutWidget.forEachChild(this::addDrawableChild);
			this.refreshWidgetPositions();
		}

		@Override
		protected void refreshWidgetPositions() {
			this.layoutWidget.refreshPositions();
		}

		@Override
		public void close() {
			this.client.setScreen(this.parent);
		}
	}
}
