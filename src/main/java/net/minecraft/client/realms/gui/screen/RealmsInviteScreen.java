package net.minecraft.client.realms.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsInviteScreen}.
 */
public class RealmsInviteScreen extends RealmsScreen {

	private static final Text INVITE_TEXT = Text.translatable("mco.configure.world.buttons.invite");
	private static final Text
			INVITE_PROFILE_NAME_TEXT =
			Text.translatable("mco.configure.world.invite.profile.name").withColor(-6250336);
	private static final Text
			INVITING_TEXT =
			Text.translatable("mco.configure.world.players.inviting").withColor(-6250336);
	private static final Text
			PLAYER_ERROR_TEXT =
			Text.translatable("mco.configure.world.players.error").withColor(-65536);
	private static final Text
			PLAYER_NOT_FOUND_ERROR =
			Text.translatable("mco.configure.world.players.invite.duplicate").withColor(-65536);
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	private @Nullable TextFieldWidget nameWidget;
	private @Nullable ButtonWidget inviteButton;
	private final RealmsServer serverData;
	private final RealmsConfigureWorldScreen configureScreen;
	private @Nullable Text errorMessage;

	public RealmsInviteScreen(RealmsConfigureWorldScreen configureScreen, RealmsServer serverData) {
		super(INVITE_TEXT);
		this.configureScreen = configureScreen;
		this.serverData = serverData;
	}

	@Override
	public void init() {
		this.layout.addHeader(INVITE_TEXT, this.textRenderer);
		DirectionalLayoutWidget
				directionalLayoutWidget =
				this.layout.addBody(DirectionalLayoutWidget.vertical().spacing(8));
		this.nameWidget =
				new TextFieldWidget(
						this.client.textRenderer,
						200,
						20,
						Text.translatable("mco.configure.world.invite.profile.name")
				);
		directionalLayoutWidget.add(LayoutWidgets.createLabeledWidget(
				this.textRenderer,
				this.nameWidget,
				INVITE_PROFILE_NAME_TEXT
		));
		this.inviteButton =
				directionalLayoutWidget.add(ButtonWidget
						.builder(INVITE_TEXT, button -> this.onInvite())
						.width(200)
						.build());
		this.layout.addFooter(ButtonWidget.builder(ScreenTexts.BACK, button -> this.close()).width(200).build());
		this.layout.forEachChild(element -> {
			ClickableWidget var10000 = this.addDrawableChild(element);
		});
		this.refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		this.layout.refreshPositions();
	}

	@Override
	protected void setInitialFocus() {
		if (this.nameWidget != null) {
			this.setInitialFocus(this.nameWidget);
		}
	}

	private void onInvite() {
		if (this.inviteButton != null && this.nameWidget != null) {
			if (StringHelper.isBlank(this.nameWidget.getText())) {
				this.showError(PLAYER_ERROR_TEXT);
			}
			else if (this.serverData.players
					.stream()
					.anyMatch(playerInfo -> playerInfo.name.equalsIgnoreCase(this.nameWidget.getText()))) {
				this.showError(PLAYER_NOT_FOUND_ERROR);
			}
			else {
				long l = this.serverData.id;
				String string = this.nameWidget.getText().trim();
				this.inviteButton.active = false;
				this.nameWidget.setEditable(false);
				this.showError(INVITING_TEXT);
				CompletableFuture
						.<Boolean>supplyAsync(() -> this.configureScreen.invite(l, string), Util.getIoWorkerExecutor())
						.thenAcceptAsync(
								success -> {
									if (success) {
										this.client.setScreen(this.configureScreen);
									}
									else {
										this.showError(PLAYER_ERROR_TEXT);
									}

									this.nameWidget.setEditable(true);
									this.inviteButton.active = true;
								}, this.executor
						);
			}
		}
	}

	private void showError(Text errorMessage) {
		this.errorMessage = errorMessage;
		this.client.getNarratorManager().narrateSystemImmediately(errorMessage);
	}

	@Override
	public void close() {
		this.client.setScreen(this.configureScreen);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		if (this.errorMessage != null && this.inviteButton != null) {
			context.drawCenteredTextWithShadow(
					this.textRenderer,
					this.errorMessage,
					this.width / 2,
					this.inviteButton.getY() + this.inviteButton.getHeight() + 8,
					-1
			);
		}
	}
}
