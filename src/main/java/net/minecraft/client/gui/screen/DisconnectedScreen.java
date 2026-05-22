package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.*;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * Экран отключения от сервера с причиной разрыва соединения и кнопкой возврата.
 */
@Environment(EnvType.CLIENT)
public class DisconnectedScreen extends Screen {

	private static final Text TO_MENU_TEXT = Text.translatable("gui.toMenu");
	private static final Text TO_TITLE_TEXT = Text.translatable("gui.toTitle");
	private static final Text REPORT_TO_SERVER_TEXT = Text.translatable("gui.report_to_server");
	private static final Text OPEN_REPORT_DIR_TEXT = Text.translatable("gui.open_report_dir");
	private final Screen parent;
	private final DisconnectionInfo info;
	private final Text buttonLabel;
	private final DirectionalLayoutWidget grid = DirectionalLayoutWidget.vertical();

	public DisconnectedScreen(Screen parent, Text title, Text reason) {
		this(parent, title, new DisconnectionInfo(reason));
	}

	public DisconnectedScreen(Screen parent, Text title, Text reason, Text buttonLabel) {
		this(parent, title, new DisconnectionInfo(reason), buttonLabel);
	}

	public DisconnectedScreen(Screen parent, Text title, DisconnectionInfo info) {
		this(parent, title, info, TO_MENU_TEXT);
	}

	public DisconnectedScreen(Screen parent, Text title, DisconnectionInfo info, Text buttonLabel) {
		super(title);
		this.parent = parent;
		this.info = info;
		this.buttonLabel = buttonLabel;
	}

	@Override
	protected void init() {
		grid.getMainPositioner().alignHorizontalCenter().margin(10);
		grid.add(new TextWidget(title, textRenderer));
		grid.add(
				new MultilineTextWidget(info.reason(), textRenderer)
						.setMaxWidth(width - 50)
						.setCentered(true)
		);
		grid.getMainPositioner().margin(2);
		info.bugReportLink().ifPresent(uri -> grid.add(
				ButtonWidget.builder(REPORT_TO_SERVER_TEXT, ConfirmLinkScreen.opening(this, uri, false))
						.width(200)
						.build()
		));
		info.report().ifPresent(path -> grid.add(
				ButtonWidget.builder(
						OPEN_REPORT_DIR_TEXT,
						button -> Util.getOperatingSystem().open(path.getParent())
				).width(200).build()
		));

		ButtonWidget backButton = client.isMultiplayerEnabled()
				? ButtonWidget.builder(buttonLabel, button -> client.setScreen(parent)).width(200).build()
				: ButtonWidget.builder(TO_TITLE_TEXT, button -> client.setScreen(new TitleScreen())).width(200).build();

		grid.add(backButton);
		grid.refreshPositions();
		grid.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		SimplePositioningWidget.setPos(grid, getNavigationFocus());
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(title, info.reason());
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
