package net.minecraft.client.gui.screen.report;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListEntry;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Экран выбора типа жалобы на нарушение правил (чат, скин, имя).
 */
@Environment(EnvType.CLIENT)
public class AbuseReportTypeScreen extends Screen {

	private static final Text TITLE_TEXT = Text.translatable("gui.abuseReport.title");
	private static final Text MESSAGE_TEXT = Text.translatable("gui.abuseReport.message");
	private static final Text CHAT_TYPE_TEXT = Text.translatable("gui.abuseReport.type.chat");
	private static final Text SKIN_TYPE_TEXT = Text.translatable("gui.abuseReport.type.skin");
	private static final Text NAME_TYPE_TEXT = Text.translatable("gui.abuseReport.type.name");

	private final Screen parent;
	private final AbuseReportContext context;
	private final SocialInteractionsPlayerListEntry selectedPlayer;
	private final DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical().spacing(6);

	public AbuseReportTypeScreen(
			Screen parent,
			AbuseReportContext context,
			SocialInteractionsPlayerListEntry selectedPlayer
	) {
		super(TITLE_TEXT);
		this.parent = parent;
		this.context = context;
		this.selectedPlayer = selectedPlayer;
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(super.getNarratedTitle(), MESSAGE_TEXT);
	}

	@Override
	protected void init() {
		layout.getMainPositioner().alignHorizontalCenter();
		layout.add(new TextWidget(title, textRenderer), layout.copyPositioner().marginBottom(6));
		layout.add(
				new MultilineTextWidget(MESSAGE_TEXT, textRenderer).setCentered(true),
				layout.copyPositioner().marginBottom(6)
		);

		ButtonWidget chatButton = layout.add(
				ButtonWidget.builder(
						CHAT_TYPE_TEXT,
						button -> client.setScreen(new ChatReportScreen(parent, context, selectedPlayer.getUuid()))
				).build()
		);

		if (!selectedPlayer.isReportable()) {
			chatButton.active = false;
			chatButton.setTooltip(Tooltip.of(
					Text.translatable("gui.socialInteractions.tooltip.report.not_reportable")
			));
		} else if (!selectedPlayer.hasSentMessage()) {
			chatButton.active = false;
			chatButton.setTooltip(Tooltip.of(Text.translatable(
					"gui.socialInteractions.tooltip.report.no_messages",
					selectedPlayer.getName()
			)));
		}

		layout.add(ButtonWidget.builder(
				SKIN_TYPE_TEXT,
				button -> client.setScreen(new SkinReportScreen(
						parent,
						context,
						selectedPlayer.getUuid(),
						selectedPlayer.getSkinSupplier()
				))
		).build());

		layout.add(ButtonWidget.builder(
				NAME_TYPE_TEXT,
				button -> client.setScreen(new UsernameReportScreen(
						parent,
						context,
						selectedPlayer.getUuid(),
						selectedPlayer.getName()
				))
		).build());

		layout.add(EmptyWidget.ofHeight(20));
		layout.add(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close()).build());
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
		SimplePositioningWidget.setPos(layout, getNavigationFocus());
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}
}
