package net.minecraft.client.realms.gui.screen.tab;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.realms.dto.Ops;
import net.minecraft.client.realms.dto.PlayerInfo;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.gui.screen.RealmsConfigureWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsConfirmScreen;
import net.minecraft.client.realms.gui.screen.RealmsInviteScreen;
import net.minecraft.client.realms.util.RealmsUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsPlayerTab}.
 */
public class RealmsPlayerTab extends GridScreenTab implements RealmsUpdatableTab {

	static final Logger LOGGER = LogUtils.getLogger();
	public static final Text TITLE = Text.translatable("mco.configure.world.players.title");
	static final Text QUESTION_TEXT = Text.translatable("mco.question");
	private static final int SPACING = 8;
	final RealmsConfigureWorldScreen screen;
	final MinecraftClient client;
	final TextRenderer textRenderer;
	RealmsServer serverData;
	final RealmsPlayerTab.InvitedObjectSelectionList playerList;

	public RealmsPlayerTab(RealmsConfigureWorldScreen screen, MinecraftClient client, RealmsServer serverData) {
		super(TITLE);
		this.screen = screen;
		this.client = client;
		this.textRenderer = screen.getTextRenderer();
		this.serverData = serverData;
		GridWidget.Adder adder = this.grid.setSpacing(8).createAdder(1);
		this.playerList = adder.add(
				new RealmsPlayerTab.InvitedObjectSelectionList(screen.width, this.getPlayerListHeight()),
				Positioner.create().alignTop().alignHorizontalCenter()
		);
		adder.add(
				ButtonWidget
						.builder(
								Text.translatable("mco.configure.world.buttons.invite"),
								button -> client.setScreen(new RealmsInviteScreen(screen, serverData))
						)
						.build(),
				Positioner.create().alignBottom().alignHorizontalCenter()
		);
		this.update(serverData);
	}

	public int getPlayerListHeight() {
		return this.screen.getContentHeight() - 20 - 16;
	}

	@Override
	public void refreshGrid(ScreenRect tabArea) {
		this.playerList.position(this.screen.width, this.getPlayerListHeight(), this.screen.layout.getHeaderHeight());
		super.refreshGrid(tabArea);
	}

	@Override
	public void update(RealmsServer server) {
		this.serverData = server;
		this.playerList.refreshEntries(server);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code HeaderEntry}.
	 */
	class HeaderEntry extends RealmsPlayerTab.PlayerTabEntry {

		private String invitedPlayerCount = "";
		private final NarratedMultilineTextWidget textWidget;

		public HeaderEntry() {
			Text text = Text.translatable("mco.configure.world.invited.number", "").formatted(Formatting.UNDERLINE);
			this.textWidget = NarratedMultilineTextWidget.builder(text, RealmsPlayerTab.this.textRenderer)
			                                             .alwaysShowBorders(false)
			                                             .backgroundRendering(NarratedMultilineTextWidget.BackgroundRendering.ON_FOCUS)
			                                             .build();
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			String
					string =
					RealmsPlayerTab.this.serverData.players != null
					? Integer.toString(RealmsPlayerTab.this.serverData.players.size()) : "0";
			if (!string.equals(this.invitedPlayerCount)) {
				this.invitedPlayerCount = string;
				Text
						text =
						Text.translatable("mco.configure.world.invited.number", string).formatted(Formatting.UNDERLINE);
				this.textWidget.setMessage(text);
			}

			this.textWidget
					.setPosition(
							RealmsPlayerTab.this.playerList.getRowLeft()
									+ RealmsPlayerTab.this.playerList.getRowWidth() / 2
									- this.textWidget.getWidth() / 2,
							this.getY() + this.getHeight() / 2 - this.textWidget.getHeight() / 2
					);
			this.textWidget.render(context, mouseX, mouseY, deltaTicks);
		}

		int calculateHeight(int i) {
			return i + this.textWidget.getMArgin() * 2;
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return List.of(this.textWidget);
		}

		@Override
		public List<? extends Element> children() {
			return List.of(this.textWidget);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code InvitedObjectSelectionList}.
	 */
	class InvitedObjectSelectionList extends ElementListWidget<RealmsPlayerTab.PlayerTabEntry> {

		private static final int HEADER_HEIGHT = 36;

		public InvitedObjectSelectionList(final int width, final int height) {
			super(MinecraftClient.getInstance(), width, height, RealmsPlayerTab.this.screen.getHeaderHeight(), 36);
		}

		void refreshEntries(RealmsServer serverData) {
			this.clearEntries();
			this.addEntries(serverData);
		}

		private void addEntries(RealmsServer serverData) {
			RealmsPlayerTab.HeaderEntry headerEntry = RealmsPlayerTab.this.new HeaderEntry();
			this.addEntry(headerEntry, headerEntry.calculateHeight(9));

			for (RealmsPlayerTab.InvitedObjectSelectionListEntry invitedObjectSelectionListEntry : serverData.players
					.stream()
					.map(playerInfo -> RealmsPlayerTab.this.new InvitedObjectSelectionListEntry(playerInfo))
					.toList()) {
				this.addEntry(invitedObjectSelectionListEntry);
			}
		}

		@Override
		protected void drawMenuListBackground(DrawContext context) {
		}

		@Override
		protected void drawHeaderAndFooterSeparators(DrawContext context) {
		}

		@Override
		public int getRowWidth() {
			return 300;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code InvitedObjectSelectionListEntry}.
	 */
	class InvitedObjectSelectionListEntry extends RealmsPlayerTab.PlayerTabEntry {

		protected static final int ICON_WIDTH = 32;
		private static final Text NORMAL_TOOLTIP_TEXT = Text.translatable("mco.configure.world.invites.normal.tooltip");
		private static final Text OPS_TOOLTIP_TEXT = Text.translatable("mco.configure.world.invites.ops.tooltip");
		private static final Text REMOVE_TOOLTIP_TEXT = Text.translatable("mco.configure.world.invites.remove.tooltip");
		private static final Identifier MAKE_OPERATOR_TEXTURE = Identifier.ofVanilla("player_list/make_operator");
		private static final Identifier REMOVE_OPERATOR_TEXTURE = Identifier.ofVanilla("player_list/remove_operator");
		private static final Identifier REMOVE_PLAYER_TEXTURE = Identifier.ofVanilla("player_list/remove_player");
		private static final int ICON_PADDING = 8;
		private static final int TEXT_PADDING = 7;
		private final PlayerInfo playerInfo;
		private final ButtonWidget uninviteButton;
		private final ButtonWidget opButton;
		private final ButtonWidget deopButton;

		public InvitedObjectSelectionListEntry(final PlayerInfo playerInfo) {
			this.playerInfo = playerInfo;
			int i = RealmsPlayerTab.this.serverData.players.indexOf(this.playerInfo);
			this.opButton = TextIconButtonWidget.builder(NORMAL_TOOLTIP_TEXT, button -> this.op(i), false)
			                                    .texture(MAKE_OPERATOR_TEXTURE, 8, 7)
			                                    .width(16 + RealmsPlayerTab.this.screen
					                                    .getTextRenderer()
					                                    .getWidth(NORMAL_TOOLTIP_TEXT))
			                                    .narration(
					                                    textSupplier -> ScreenTexts.joinSentences(
							                                    Text.translatable(
									                                    "mco.invited.player.narration",
									                                    playerInfo.name
							                                    ),
							                                    textSupplier.get(),
							                                    Text.translatable(
									                                    "narration.cycle_button.usage.focused",
									                                    OPS_TOOLTIP_TEXT
							                                    )
					                                    )
			                                    )
			                                    .build();
			this.deopButton = TextIconButtonWidget.builder(OPS_TOOLTIP_TEXT, button -> this.deop(i), false)
			                                      .texture(REMOVE_OPERATOR_TEXTURE, 8, 7)
			                                      .width(16 + RealmsPlayerTab.this.screen
					                                      .getTextRenderer()
					                                      .getWidth(OPS_TOOLTIP_TEXT))
			                                      .narration(
					                                      textSupplier -> ScreenTexts.joinSentences(
							                                      Text.translatable(
									                                      "mco.invited.player.narration",
									                                      playerInfo.name
							                                      ),
							                                      textSupplier.get(),
							                                      Text.translatable(
									                                      "narration.cycle_button.usage.focused",
									                                      NORMAL_TOOLTIP_TEXT
							                                      )
					                                      )
			                                      )
			                                      .build();
			this.uninviteButton = TextIconButtonWidget.builder(REMOVE_TOOLTIP_TEXT, button -> this.uninvite(i), false)
			                                          .texture(REMOVE_PLAYER_TEXTURE, 8, 7)
			                                          .width(16 + RealmsPlayerTab.this.screen
					                                          .getTextRenderer()
					                                          .getWidth(REMOVE_TOOLTIP_TEXT))
			                                          .narration(textSupplier -> ScreenTexts.joinSentences(
					                                          Text.translatable(
							                                          "mco.invited.player.narration",
							                                          playerInfo.name
					                                          ), textSupplier.get()
			                                          ))
			                                          .build();
			this.refreshOpButtonsVisibility();
		}

		private void op(int index) {
			UUID uUID = RealmsPlayerTab.this.serverData.players.get(index).uuid;
			RealmsUtil.<Ops>runAsync(
					          (RealmsUtil.RealmsSupplier<Ops>) client -> client.op(RealmsPlayerTab.this.serverData.id, uUID),
					          error -> RealmsPlayerTab.LOGGER.error("Couldn't op the user", error)
			          )
			          .thenAcceptAsync(
					          ops -> {
						          this.setOps(ops);
						          this.refreshOpButtonsVisibility();
						          this.setFocused(this.deopButton);
					          }, RealmsPlayerTab.this.client
			          );
		}

		private void deop(int index) {
			UUID uUID = RealmsPlayerTab.this.serverData.players.get(index).uuid;
			RealmsUtil.<Ops>runAsync(
					          (RealmsUtil.RealmsSupplier<Ops>) client -> client.deop(RealmsPlayerTab.this.serverData.id, uUID),
					          error -> RealmsPlayerTab.LOGGER.error("Couldn't deop the user", error)
			          )
			          .thenAcceptAsync(
					          ops -> {
						          this.setOps(ops);
						          this.refreshOpButtonsVisibility();
						          this.setFocused(this.opButton);
					          }, RealmsPlayerTab.this.client
			          );
		}

		private void uninvite(int index) {
			if (index >= 0 && index < RealmsPlayerTab.this.serverData.players.size()) {
				PlayerInfo playerInfo = RealmsPlayerTab.this.serverData.players.get(index);
				RealmsConfirmScreen realmsConfirmScreen = new RealmsConfirmScreen(
						confirmed -> {
							if (confirmed) {
								RealmsUtil.runAsync(
										client -> client.uninvite(RealmsPlayerTab.this.serverData.id, playerInfo.uuid),
										error -> RealmsPlayerTab.LOGGER.error("Couldn't uninvite user", error)
								);
								RealmsPlayerTab.this.serverData.players.remove(index);
								RealmsPlayerTab.this.update(RealmsPlayerTab.this.serverData);
							}

							RealmsPlayerTab.this.client.setScreen(RealmsPlayerTab.this.screen);
						},
						RealmsPlayerTab.QUESTION_TEXT,
						Text.translatable("mco.configure.world.uninvite.player", playerInfo.name)
				);
				RealmsPlayerTab.this.client.setScreen(realmsConfirmScreen);
			}
		}

		private void setOps(Ops ops) {
			for (PlayerInfo playerInfo : RealmsPlayerTab.this.serverData.players) {
				playerInfo.operator = ops.ops().contains(playerInfo.name);
			}
		}

		private void refreshOpButtonsVisibility() {
			this.opButton.visible = !this.playerInfo.operator;
			this.deopButton.visible = !this.opButton.visible;
		}

		private ButtonWidget getOpButton() {
			return this.opButton.visible ? this.opButton : this.deopButton;
		}

		@Override
		public List<? extends Element> children() {
			return ImmutableList.of(this.getOpButton(), this.uninviteButton);
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return ImmutableList.of(this.getOpButton(), this.uninviteButton);
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int i;
			if (!this.playerInfo.accepted) {
				i = -6250336;
			}
			else if (this.playerInfo.online) {
				i = -16711936;
			}
			else {
				i = -1;
			}

			int j = this.getContentMiddleY() - 16;
			RealmsUtil.drawPlayerHead(context, this.getContentX(), j, 32, this.playerInfo.uuid);
			int k = this.getContentMiddleY() - 9 / 2;
			context.drawTextWithShadow(
					RealmsPlayerTab.this.textRenderer,
					this.playerInfo.name,
					this.getContentX() + 8 + 32,
					k,
					i
			);
			int l = this.getContentMiddleY() - 10;
			int m = this.getContentRightEnd() - this.uninviteButton.getWidth();
			this.uninviteButton.setPosition(m, l);
			this.uninviteButton.render(context, mouseX, mouseY, deltaTicks);
			int n = m - this.getOpButton().getWidth() - 8;
			this.opButton.setPosition(n, l);
			this.opButton.render(context, mouseX, mouseY, deltaTicks);
			this.deopButton.setPosition(n, l);
			this.deopButton.render(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code PlayerTabEntry}.
	 */
	abstract static class PlayerTabEntry extends ElementListWidget.Entry<RealmsPlayerTab.PlayerTabEntry> {
	}
}
