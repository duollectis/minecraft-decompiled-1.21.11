package net.minecraft.client.realms.gui.screen;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.RealmsPeriodicCheckers;
import net.minecraft.client.realms.dto.PendingInvite;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.util.RealmsUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsPendingInvitesScreen}.
 */
public class RealmsPendingInvitesScreen extends RealmsScreen {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final Text NO_PENDING_TEXT = Text.translatable("mco.invites.nopending");
	private final Screen parent;
	private final CompletableFuture<List<PendingInvite>> pendingInvites = CompletableFuture.supplyAsync(
			() -> {
				try {
					return RealmsClient.create().pendingInvites().pendingInvites();
				}
				catch (RealmsServiceException var1) {
					LOGGER.error("Couldn't list invites", var1);
					return List.of();
				}
			}, Util.getIoWorkerExecutor()
	);
	final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	RealmsPendingInvitesScreen.@Nullable PendingInvitationSelectionList pendingInvitationSelectionList;

	public RealmsPendingInvitesScreen(Screen parent, Text title) {
		super(title);
		this.parent = parent;
	}

	@Override
	public void init() {
		RealmsMainScreen.resetPendingInvitesCount();
		this.layout.addHeader(this.title, this.textRenderer);
		this.pendingInvitationSelectionList =
				this.layout.addBody(new RealmsPendingInvitesScreen.PendingInvitationSelectionList(this.client));
		this.pendingInvites
				.thenAcceptAsync(
						pendingInvites -> {
							List<RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry>
									list =
									pendingInvites.stream()
									              .map(invite -> new RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry(
											              invite))
									              .toList();
							this.pendingInvitationSelectionList.replaceEntries(list);
							if (list.isEmpty()) {
								this.client.getNarratorManager().narrateSystemMessage(NO_PENDING_TEXT);
							}
						},
						this.executor
				);
		this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close()).width(200).build());
		this.layout.forEachChild(child -> {
			ClickableWidget var10000 = this.addDrawableChild(child);
		});
		this.refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		this.layout.refreshPositions();
		if (this.pendingInvitationSelectionList != null) {
			this.pendingInvitationSelectionList.position(this.width, this.layout);
		}
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		if (this.pendingInvites.isDone() && this.pendingInvitationSelectionList.isEmpty()) {
			context.drawCenteredTextWithShadow(
					this.textRenderer,
					NO_PENDING_TEXT,
					this.width / 2,
					this.height / 2 - 20,
					-1
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code PendingInvitationSelectionList}.
	 */
	class PendingInvitationSelectionList extends ElementListWidget<RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry> {

		public static final int ENTRY_HEIGHT = 36;

		public PendingInvitationSelectionList(final MinecraftClient client) {
			super(
					client,
					RealmsPendingInvitesScreen.this.width,
					RealmsPendingInvitesScreen.this.layout.getContentHeight(),
					RealmsPendingInvitesScreen.this.layout.getHeaderHeight(),
					36
			);
		}

		@Override
		public int getRowWidth() {
			return 280;
		}

		public boolean isEmpty() {
			return this.getEntryCount() == 0;
		}

		/**
		 * Remove.
		 *
		 * @param invitation invitation
		 */
		public void remove(RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry invitation) {
			this.removeEntry(invitation);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code PendingInvitationSelectionListEntry}.
	 */
	class PendingInvitationSelectionListEntry extends ElementListWidget.Entry<RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry> {

		private static final Text ACCEPT_TEXT = Text.translatable("mco.invites.button.accept");
		private static final Text REJECT_TEXT = Text.translatable("mco.invites.button.reject");
		private static final ButtonTextures ACCEPT_TEXTURE = new ButtonTextures(
				Identifier.ofVanilla("pending_invite/accept"), Identifier.ofVanilla("pending_invite/accept_highlighted")
		);
		private static final ButtonTextures REJECT_TEXTURE = new ButtonTextures(
				Identifier.ofVanilla("pending_invite/reject"), Identifier.ofVanilla("pending_invite/reject_highlighted")
		);
		private static final int BUTTON_SIZE = 18;
		private static final int BUTTON_OFFSET = 21;
		private static final int ENTRY_PADDING = 38;
		private final PendingInvite pendingInvite;
		private final List<ClickableWidget> buttons = new ArrayList<>();
		private final TextIconButtonWidget acceptButton;
		private final TextIconButtonWidget rejectButton;
		private final TextWidget worldNameText;
		private final TextWidget worldOwnerNameText;
		private final TextWidget dateText;

		PendingInvitationSelectionListEntry(final PendingInvite pendingInvite) {
			this.pendingInvite = pendingInvite;
			int i = RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.getRowWidth() - 32 - 32 - 42;
			this.worldNameText =
					new TextWidget(
							Text.literal(pendingInvite.worldName()),
							RealmsPendingInvitesScreen.this.textRenderer
					).setMaxWidth(i);
			this.worldOwnerNameText = new TextWidget(
					Text.literal(pendingInvite.worldOwnerName()).withColor(-6250336),
					RealmsPendingInvitesScreen.this.textRenderer
			)
					.setMaxWidth(i);
			this.dateText = new TextWidget(
					Texts.withStyle(
							RealmsUtil.convertToAgePresentation(pendingInvite.date()),
							Style.EMPTY.withColor(-6250336)
					),
					RealmsPendingInvitesScreen.this.textRenderer
			)
					.setMaxWidth(i);
			ButtonWidget.NarrationSupplier narrationSupplier = this.getNarration(pendingInvite);
			this.acceptButton = TextIconButtonWidget.builder(ACCEPT_TEXT, button -> this.handle(true), false)
			                                        .texture(ACCEPT_TEXTURE, 18, 18)
			                                        .dimension(21, 21)
			                                        .narration(narrationSupplier)
			                                        .useTextAsTooltip()
			                                        .build();
			this.rejectButton = TextIconButtonWidget.builder(REJECT_TEXT, button -> this.handle(false), false)
			                                        .texture(REJECT_TEXTURE, 18, 18)
			                                        .dimension(21, 21)
			                                        .narration(narrationSupplier)
			                                        .useTextAsTooltip()
			                                        .build();
			this.buttons.addAll(List.of(this.acceptButton, this.rejectButton));
		}

		private ButtonWidget.NarrationSupplier getNarration(PendingInvite invite) {
			return textSupplier -> {
				MutableText mutableText = ScreenTexts.joinSentences(
						textSupplier.get(),
						Text.literal(invite.worldName()),
						Text.literal(invite.worldOwnerName()),
						RealmsUtil.convertToAgePresentation(invite.date())
				);
				return Text.translatable("narrator.select", mutableText);
			};
		}

		@Override
		public List<? extends Element> children() {
			return this.buttons;
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return this.buttons;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int i = this.getContentX();
			int j = this.getContentY();
			int k = i + 38;
			RealmsUtil.drawPlayerHead(context, i, j, 32, this.pendingInvite.worldOwnerUuid());
			this.worldNameText.setPosition(k, j + 1);
			this.worldNameText.renderWidget(context, mouseX, mouseY, i);
			this.worldOwnerNameText.setPosition(k, j + 12);
			this.worldOwnerNameText.renderWidget(context, mouseX, mouseY, i);
			this.dateText.setPosition(k, j + 24);
			this.dateText.renderWidget(context, mouseX, mouseY, i);
			int l = j + this.getContentHeight() / 2 - 10;
			this.acceptButton.setPosition(i + this.getContentWidth() - 16 - 42, l);
			this.acceptButton.render(context, mouseX, mouseY, deltaTicks);
			this.rejectButton.setPosition(i + this.getContentWidth() - 8 - 21, l);
			this.rejectButton.render(context, mouseX, mouseY, deltaTicks);
		}

		private void handle(boolean accepted) {
			String string = this.pendingInvite.invitationId();
			CompletableFuture.<Boolean>supplyAsync(
					() -> {
						try {
							RealmsClient realmsClient = RealmsClient.create();
							if (accepted) {
								realmsClient.acceptInvitation(string);
							}
							else {
								realmsClient.rejectInvitation(string);
							}

							return true;
						}
						catch (RealmsServiceException var3) {
							RealmsPendingInvitesScreen.LOGGER.error("Couldn't handle invite", var3);
							return false;
						}
					}, Util.getIoWorkerExecutor()
			).thenAcceptAsync(
					processed -> {
						if (processed) {
							RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.remove(this);
							RealmsPeriodicCheckers
									realmsPeriodicCheckers =
									RealmsPendingInvitesScreen.this.client.getRealmsPeriodicCheckers();
							if (accepted) {
								realmsPeriodicCheckers.serverList.reset();
							}

							realmsPeriodicCheckers.pendingInvitesCount.reset();
						}
					}, RealmsPendingInvitesScreen.this.executor
			);
		}
	}
}
