package net.minecraft.client.gui.screen.multiplayer;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.report.AbuseReportTypeScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Запись игрока в списке социальных взаимодействий — отображает аватар, имя, статус
 * и кнопки скрытия/показа/жалобы. Поддерживает оффлайн-режим и черновики жалоб.
 */
@Environment(EnvType.CLIENT)
public class SocialInteractionsPlayerListEntry extends ElementListWidget.Entry<SocialInteractionsPlayerListEntry> {

	private static final Identifier DRAFT_REPORT_ICON_TEXTURE = Identifier.ofVanilla("icon/draft_report");
	private static final Duration TOOLTIP_DELAY = Duration.ofMillis(500L);
	private static final ButtonTextures REPORT_BUTTON_TEXTURES = new ButtonTextures(
		Identifier.ofVanilla("social_interactions/report_button"),
		Identifier.ofVanilla("social_interactions/report_button_disabled"),
		Identifier.ofVanilla("social_interactions/report_button_highlighted")
	);
	private static final ButtonTextures MUTE_BUTTON_TEXTURES = new ButtonTextures(
		Identifier.ofVanilla("social_interactions/mute_button"),
		Identifier.ofVanilla("social_interactions/mute_button_highlighted")
	);
	private static final ButtonTextures UNMUTE_BUTTON_TEXTURES = new ButtonTextures(
		Identifier.ofVanilla("social_interactions/unmute_button"),
		Identifier.ofVanilla("social_interactions/unmute_button_highlighted")
	);

	private static final Text HIDDEN_TEXT = Text.translatable("gui.socialInteractions.status_hidden").formatted(Formatting.ITALIC);
	private static final Text BLOCKED_TEXT = Text.translatable("gui.socialInteractions.status_blocked").formatted(Formatting.ITALIC);
	private static final Text OFFLINE_TEXT = Text.translatable("gui.socialInteractions.status_offline").formatted(Formatting.ITALIC);
	private static final Text HIDDEN_OFFLINE_TEXT = Text.translatable("gui.socialInteractions.status_hidden_offline").formatted(Formatting.ITALIC);
	private static final Text BLOCKED_OFFLINE_TEXT = Text.translatable("gui.socialInteractions.status_blocked_offline").formatted(Formatting.ITALIC);
	private static final Text REPORT_DISABLED_TEXT = Text.translatable("gui.socialInteractions.tooltip.report.disabled");
	private static final Text HIDE_TEXT = Text.translatable("gui.socialInteractions.tooltip.hide");
	private static final Text SHOW_TEXT = Text.translatable("gui.socialInteractions.tooltip.show");
	private static final Text REPORT_TEXT = Text.translatable("gui.socialInteractions.tooltip.report");

	private static final int ENTRY_HEIGHT = 24;
	private static final int ICON_PADDING = 4;
	private static final int BUTTON_SIZE = 20;
	private static final int DOUBLE_LINE_HEIGHT = 9 + 9;

	public static final int BLACK_COLOR = ColorHelper.getArgb(190, 0, 0, 0);
	public static final int GRAY_COLOR = ColorHelper.getArgb(255, 74, 74, 74);
	public static final int DARK_GRAY_COLOR = ColorHelper.getArgb(255, 48, 48, 48);
	public static final int WHITE_COLOR = ColorHelper.getArgb(255, 255, 255, 255);
	public static final int LIGHT_GRAY_COLOR = ColorHelper.getArgb(140, 255, 255, 255);

	private final MinecraftClient client;
	private final List<ClickableWidget> buttons;
	private final UUID uuid;
	private final String name;
	private final Supplier<SkinTextures> skinSupplier;
	private boolean offline;
	private boolean sentMessage;
	private final boolean canSendReports;
	private boolean hasDraftReport;
	private final boolean reportable;
	private @Nullable ButtonWidget hideButton;
	private @Nullable ButtonWidget showButton;
	private @Nullable ButtonWidget reportButton;
	private float timeCounter;

	public SocialInteractionsPlayerListEntry(
		MinecraftClient client,
		SocialInteractionsScreen parent,
		UUID uuid,
		String name,
		Supplier<SkinTextures> skinTexture,
		boolean reportable
	) {
		this.client = client;
		this.uuid = uuid;
		this.name = name;
		skinSupplier = skinTexture;
		AbuseReportContext abuseReportContext = client.getAbuseReportContext();
		canSendReports = abuseReportContext.getSender().canSendReports();
		this.reportable = reportable;
		updateHasDraftReport(abuseReportContext);
		Text hideNarration = Text.translatable("gui.socialInteractions.narration.hide", name);
		Text showNarration = Text.translatable("gui.socialInteractions.narration.show", name);
		SocialInteractionsManager socialManager = client.getSocialInteractionsManager();
		boolean chatAllowed = client.getChatRestriction().allowsChat(client.isInSingleplayer());
		boolean isOtherPlayer = !client.player.getUuid().equals(uuid);
		if (!SharedConstants.SOCIAL_INTERACTIONS && (!isOtherPlayer || !chatAllowed || socialManager.isPlayerBlocked(uuid))) {
			buttons = ImmutableList.of();
			return;
		}

		reportButton = new TexturedButtonWidget(
			0, 0, BUTTON_SIZE, BUTTON_SIZE,
			REPORT_BUTTON_TEXTURES,
			button -> abuseReportContext.tryShowDraftScreen(
				client,
				parent,
				() -> client.setScreen(new AbuseReportTypeScreen(parent, abuseReportContext, this)),
				false
			),
			Text.translatable("gui.socialInteractions.report")
		) {
			@Override
			protected MutableText getNarrationMessage() {
				return SocialInteractionsPlayerListEntry.this.getNarrationMessage(super.getNarrationMessage());
			}
		};
		reportButton.active = canSendReports;
		reportButton.setTooltip(getReportButtonTooltip());
		reportButton.setTooltipDelay(TOOLTIP_DELAY);
		hideButton = new TexturedButtonWidget(
			0, 0, BUTTON_SIZE, BUTTON_SIZE,
			MUTE_BUTTON_TEXTURES,
			button -> {
				socialManager.hidePlayer(uuid);
				onButtonClick(true, Text.translatable("gui.socialInteractions.hidden_in_chat", name));
			},
			Text.translatable("gui.socialInteractions.hide")
		) {
			@Override
			protected MutableText getNarrationMessage() {
				return SocialInteractionsPlayerListEntry.this.getNarrationMessage(super.getNarrationMessage());
			}
		};
		hideButton.setTooltip(Tooltip.of(HIDE_TEXT, hideNarration));
		hideButton.setTooltipDelay(TOOLTIP_DELAY);
		showButton = new TexturedButtonWidget(
			0, 0, BUTTON_SIZE, BUTTON_SIZE,
			UNMUTE_BUTTON_TEXTURES,
			button -> {
				socialManager.showPlayer(uuid);
				onButtonClick(false, Text.translatable("gui.socialInteractions.shown_in_chat", name));
			},
			Text.translatable("gui.socialInteractions.show")
		) {
			@Override
			protected MutableText getNarrationMessage() {
				return SocialInteractionsPlayerListEntry.this.getNarrationMessage(super.getNarrationMessage());
			}
		};
		showButton.setTooltip(Tooltip.of(SHOW_TEXT, showNarration));
		showButton.setTooltipDelay(TOOLTIP_DELAY);
		buttons = new ArrayList<>();
		buttons.add(hideButton);
		buttons.add(reportButton);
		setShowButtonVisible(socialManager.isPlayerHidden(uuid));
	}

	public void updateHasDraftReport(AbuseReportContext context) {
		hasDraftReport = context.draftPlayerUuidEquals(uuid);
	}

	private Tooltip getReportButtonTooltip() {
		return !canSendReports
			? Tooltip.of(REPORT_DISABLED_TEXT)
			: Tooltip.of(REPORT_TEXT, Text.translatable("gui.socialInteractions.narration.report", name));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
		int iconX = getContentX() + ICON_PADDING;
		int iconY = getContentY() + (getContentHeight() - ENTRY_HEIGHT) / 2;
		int textX = iconX + ENTRY_HEIGHT + ICON_PADDING;
		Text statusText = getStatusText();
		int textY;
		if (statusText == ScreenTexts.EMPTY) {
			context.fill(getContentX(), getContentY(), getContentRightEnd(), getContentBottomEnd(), GRAY_COLOR);
			textY = getContentY() + (getContentHeight() - 9) / 2;
		}
		else {
			context.fill(getContentX(), getContentY(), getContentRightEnd(), getContentBottomEnd(), DARK_GRAY_COLOR);
			textY = getContentY() + (getContentHeight() - DOUBLE_LINE_HEIGHT) / 2;
			context.drawTextWithShadow(client.textRenderer, statusText, textX, textY + 12, LIGHT_GRAY_COLOR);
		}

		PlayerSkinDrawer.draw(context, skinSupplier.get(), iconX, iconY, ENTRY_HEIGHT);
		context.drawTextWithShadow(client.textRenderer, name, textX, textY, WHITE_COLOR);
		if (offline) {
			context.fill(iconX, iconY, iconX + ENTRY_HEIGHT, iconY + ENTRY_HEIGHT, BLACK_COLOR);
		}

		if (hideButton != null && showButton != null && reportButton != null) {
			float savedTimeCounter = timeCounter;
			hideButton.setX(getContentX() + (getContentWidth() - hideButton.getWidth() - ICON_PADDING) - BUTTON_SIZE - ICON_PADDING);
			hideButton.setY(getContentY() + (getContentHeight() - hideButton.getHeight()) / 2);
			hideButton.render(context, mouseX, mouseY, deltaTicks);
			showButton.setX(getContentX() + (getContentWidth() - showButton.getWidth() - ICON_PADDING) - BUTTON_SIZE - ICON_PADDING);
			showButton.setY(getContentY() + (getContentHeight() - showButton.getHeight()) / 2);
			showButton.render(context, mouseX, mouseY, deltaTicks);
			reportButton.setX(getContentX() + (getContentWidth() - showButton.getWidth() - ICON_PADDING));
			reportButton.setY(getContentY() + (getContentHeight() - showButton.getHeight()) / 2);
			reportButton.render(context, mouseX, mouseY, deltaTicks);
			if (savedTimeCounter == timeCounter) {
				timeCounter = 0.0F;
			}
		}

		if (hasDraftReport && reportButton != null) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				DRAFT_REPORT_ICON_TEXTURE,
				reportButton.getX() + 5,
				reportButton.getY() + 1,
				15,
				15
			);
		}
	}

	@Override
	public List<? extends Element> children() {
		return buttons;
	}

	@Override
	public List<? extends Selectable> selectableChildren() {
		return buttons;
	}

	public String getName() {
		return name;
	}

	public UUID getUuid() {
		return uuid;
	}

	public Supplier<SkinTextures> getSkinSupplier() {
		return skinSupplier;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
	}

	public boolean isOffline() {
		return offline;
	}

	public void setSentMessage(boolean sentMessage) {
		this.sentMessage = sentMessage;
	}

	public boolean hasSentMessage() {
		return sentMessage;
	}

	public boolean isReportable() {
		return reportable;
	}

	private void onButtonClick(boolean showButtonVisible, Text chatMessage) {
		setShowButtonVisible(showButtonVisible);
		client.inGameHud.getChatHud().addMessage(chatMessage);
		client.getNarratorManager().narrateSystemImmediately(chatMessage);
	}

	private void setShowButtonVisible(boolean showButtonVisible) {
		showButton.visible = showButtonVisible;
		hideButton.visible = !showButtonVisible;
		buttons.set(0, showButtonVisible ? showButton : hideButton);
	}

	MutableText getNarrationMessage(MutableText text) {
		Text statusText = getStatusText();
		return statusText == ScreenTexts.EMPTY
			? Text.literal(name).append(", ").append(text)
			: Text.literal(name).append(", ").append(statusText).append(", ").append(text);
	}

	private Text getStatusText() {
		boolean isHidden = client.getSocialInteractionsManager().isPlayerHidden(uuid);
		boolean isBlocked = client.getSocialInteractionsManager().isPlayerBlocked(uuid);
		if (isBlocked && offline) {
			return BLOCKED_OFFLINE_TEXT;
		}
		else if (isHidden && offline) {
			return HIDDEN_OFFLINE_TEXT;
		}
		else if (isBlocked) {
			return BLOCKED_TEXT;
		}
		else if (isHidden) {
			return HIDDEN_TEXT;
		}

		return offline ? OFFLINE_TEXT : ScreenTexts.EMPTY;
	}
}
