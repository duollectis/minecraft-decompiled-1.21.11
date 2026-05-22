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

/**
 * Игровое меню паузы. В режиме показа меню отображает полный набор кнопок,
 * в режиме паузы — только заголовок.
 */
@Environment(EnvType.CLIENT)
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
		return showMenu;
	}

	@Override
	protected void init() {
		if (showMenu) {
			initWidgets();
		}

		int titleWidth = textRenderer.getWidth(title);
		addDrawableChild(new TextWidget(
				width / 2 - titleWidth / 2,
				showMenu ? 40 : 10,
				titleWidth,
				9,
				title,
				textRenderer
		));
	}

	private void initWidgets() {
		GridWidget gridWidget = new GridWidget();
		gridWidget.getMainPositioner().margin(4, 4, 4, 0);
		GridWidget.Adder adder = gridWidget.createAdder(GRID_COLUMNS);
		adder.add(
				ButtonWidget.builder(
						RETURN_TO_GAME_TEXT, button -> {
							client.setScreen(null);
							client.mouse.lockCursor();
						}
				).width(WIDE_BUTTON_WIDTH).build(), 2, gridWidget.copyPositioner().marginTop(BUTTONS_TOP_MARGIN)
		);
		adder.add(createButton(
				ADVANCEMENTS_TEXT,
				() -> new AdvancementsScreen(client.player.networkHandler.getAdvancementHandler(), this)
		));
		adder.add(createButton(STATS_TEXT, () -> new StatsScreen(this, client.player.getStatHandler())));

		Optional<? extends RegistryEntry<Dialog>> customOptionsDialog = getCustomOptionsDialog();
		if (customOptionsDialog.isEmpty()) {
			addFeedbackAndBugsButtons(this, adder);
		} else {
			addFeedbackAndCustomOptionsButtons(client, (RegistryEntry<Dialog>) customOptionsDialog.get(), adder);
		}

		adder.add(createButton(OPTIONS_TEXT, () -> new OptionsScreen(this, client.options)));

		if (client.isIntegratedServerRunning() && !client.getServer().isRemote()) {
			adder.add(createButton(SHARE_TO_LAN_TEXT, () -> new OpenToLanScreen(this)));
		} else {
			adder.add(createButton(PLAYER_REPORTING_TEXT, () -> new SocialInteractionsScreen(this)));
		}

		exitButton = adder.add(
				ButtonWidget.builder(
						ScreenTexts.returnToMenuOrDisconnect(client.isInSingleplayer()), button -> {
							button.active = false;
							client.getAbuseReportContext().tryShowDraftScreen(
									client,
									this,
									() -> client.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT),
									true
							);
						}
				).width(WIDE_BUTTON_WIDTH).build(), 2
		);
		gridWidget.refreshPositions();
		SimplePositioningWidget.setPos(gridWidget, 0, 0, width, height, 0.5F, 0.25F);
		gridWidget.forEachChild(this::addDrawableChild);
	}

	private Optional<? extends RegistryEntry<Dialog>> getCustomOptionsDialog() {
		Registry<Dialog> registry = client.player.networkHandler.getRegistryManager().getOrThrow(RegistryKeys.DIALOG);
		Optional<? extends RegistryEntryList<Dialog>> optional = registry.getOptional(DialogTags.PAUSE_SCREEN_ADDITIONS);

		if (optional.isPresent()) {
			RegistryEntryList<Dialog> dialogList = (RegistryEntryList<Dialog>) optional.get();

			if (dialogList.size() > 0) {
				return dialogList.size() == 1
						? Optional.of(dialogList.get(0))
						: registry.getOptional(Dialogs.CUSTOM_OPTIONS);
			}
		}

		ServerLinks serverLinks = client.player.networkHandler.getServerLinks();
		return serverLinks.isEmpty() ? Optional.empty() : registry.getOptional(Dialogs.SERVER_LINKS);
	}

	static void addFeedbackAndBugsButtons(Screen parentScreen, GridWidget.Adder gridAdder) {
		gridAdder.add(createUrlButton(
				parentScreen,
				SEND_FEEDBACK_TEXT,
				SharedConstants.getGameVersion().stable() ? Urls.JAVA_FEEDBACK : Urls.SNAPSHOT_FEEDBACK
		));
		gridAdder.add(createUrlButton(parentScreen, REPORT_BUGS_TEXT, Urls.SNAPSHOT_BUGS)).active =
				!SharedConstants.getGameVersion().dataVersion().isNotMainSeries();
	}

	private void addFeedbackAndCustomOptionsButtons(
			MinecraftClient client,
			RegistryEntry<Dialog> dialog,
			GridWidget.Adder gridAdder
	) {
		gridAdder.add(createButton(FEEDBACK_TEXT, () -> new FeedbackScreen(this)));
		gridAdder.add(
				ButtonWidget.builder(
						dialog.value().common().getExternalTitle(),
						button -> client.player.networkHandler.showDialog(dialog, this)
				).width(NORMAL_BUTTON_WIDTH).tooltip(CUSTOM_OPTIONS_TOOLTIP).build()
		);
	}

	@Override
	public void tick() {
		if (shouldShowNowPlayingToast()) {
			NowPlayingToast.tick();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);

		if (shouldShowNowPlayingToast()) {
			NowPlayingToast.draw(context, textRenderer);
		}

		if (showMenu && client.getAbuseReportContext().hasDraft() && exitButton != null) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					DRAFT_REPORT_ICON_TEXTURE,
					exitButton.getX() + exitButton.getWidth() - 17,
					exitButton.getY() + 3,
					15,
					15
			);
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (showMenu) {
			super.renderBackground(context, mouseX, mouseY, deltaTicks);
		}
	}

	public boolean shouldShowNowPlayingToast() {
		GameOptions gameOptions = client.options;
		return gameOptions.getMusicToast().getValue().canShow()
				&& gameOptions.getSoundVolume(SoundCategory.MUSIC) > 0.0F
				&& showMenu;
	}

	private ButtonWidget createButton(Text text, Supplier<Screen> screenSupplier) {
		return ButtonWidget.builder(text, button -> client.setScreen(screenSupplier.get())).width(NORMAL_BUTTON_WIDTH).build();
	}

	private static ButtonWidget createUrlButton(Screen parent, Text text, URI uri) {
		return ButtonWidget.builder(text, ConfirmLinkScreen.opening(parent, uri)).width(NORMAL_BUTTON_WIDTH).build();
	}

	@Environment(EnvType.CLIENT)
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
			layoutWidget.addHeader(TITLE, textRenderer);
			GridWidget gridWidget = layoutWidget.addBody(new GridWidget());
			gridWidget.getMainPositioner().margin(4, 4, 4, 0);
			GridWidget.Adder adder = gridWidget.createAdder(2);
			GameMenuScreen.addFeedbackAndBugsButtons(this, adder);
			layoutWidget.addFooter(ButtonWidget.builder(ScreenTexts.BACK, button -> close()).width(200).build());
			layoutWidget.forEachChild(this::addDrawableChild);
			refreshWidgetPositions();
		}

		@Override
		protected void refreshWidgetPositions() {
			layoutWidget.refreshPositions();
		}

		@Override
		public void close() {
			client.setScreen(parent);
		}
	}
}
