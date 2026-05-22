package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.PublishCommand;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.util.NetworkUtils;
import net.minecraft.world.GameMode;
import org.jspecify.annotations.Nullable;

/**
 * Экран открытия одиночной игры для локальной сети (LAN).
 * Позволяет выбрать режим игры, разрешить команды и задать порт.
 */
@Environment(EnvType.CLIENT)
public class OpenToLanScreen extends Screen {

	private static final int MIN_PORT = 1024;
	private static final int MAX_PORT = 65535;
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAME_MODE_BUTTON_X_OFFSET = -155;
	private static final int COMMANDS_BUTTON_X_OFFSET = 5;
	private static final int GAME_MODE_BUTTON_Y = 100;
	private static final int PORT_FIELD_Y = 160;
	private static final int BOTTOM_BUTTON_Y_OFFSET = 28;
	private static final int TITLE_Y = 50;
	private static final int OTHER_PLAYERS_Y = 82;
	private static final int PORT_LABEL_Y = 142;
	private static final int PORT_FIELD_WIDTH = 150;
	private static final int PORT_FIELD_X_OFFSET = 75;
	private static final int COLOR_VALID = -2039584;
	private static final int COLOR_INVALID = -2142128;

	private static final Text ALLOW_COMMANDS_TEXT = Text.translatable("selectWorld.allowCommands");
	private static final Text GAME_MODE_TEXT = Text.translatable("selectWorld.gameMode");
	private static final Text OTHER_PLAYERS_TEXT = Text.translatable("lanServer.otherPlayers");
	private static final Text PORT_TEXT = Text.translatable("lanServer.port");
	private static final Text UNAVAILABLE_PORT_TEXT = Text.translatable("lanServer.port.unavailable", MIN_PORT, MAX_PORT);
	private static final Text INVALID_PORT_TEXT = Text.translatable("lanServer.port.invalid", MIN_PORT, MAX_PORT);

	private final Screen parent;
	private GameMode gameMode = GameMode.SURVIVAL;
	private boolean allowCommands;
	private int port = NetworkUtils.findLocalPort();
	private @Nullable TextFieldWidget portField;

	public OpenToLanScreen(Screen parent) {
		super(Text.translatable("lanServer.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		IntegratedServer server = client.getServer();
		gameMode = server.getDefaultGameMode();
		allowCommands = server.getSaveProperties().areCommandsAllowed();

		addDrawableChild(
			CyclingButtonWidget.builder(GameMode::getSimpleTranslatableName, gameMode)
				.values(GameMode.SURVIVAL, GameMode.SPECTATOR, GameMode.CREATIVE, GameMode.ADVENTURE)
				.build(
					width / 2 + GAME_MODE_BUTTON_X_OFFSET, GAME_MODE_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT,
					GAME_MODE_TEXT, (button, mode) -> gameMode = mode
				)
		);
		addDrawableChild(
			CyclingButtonWidget.onOffBuilder(allowCommands)
				.build(
					width / 2 + COMMANDS_BUTTON_X_OFFSET, GAME_MODE_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT,
					ALLOW_COMMANDS_TEXT, (button, commands) -> allowCommands = commands
				)
		);

		ButtonWidget startButton = ButtonWidget.builder(
			Text.translatable("lanServer.start"), button -> {
				client.setScreen(null);
				Text result = server.openToLan(gameMode, allowCommands, port)
					? PublishCommand.getStartedText(port)
					: Text.translatable("commands.publish.failed");
				client.inGameHud.getChatHud().addMessage(result);
				client.getNarratorManager().narrateSystemMessage(result);
				client.updateWindowTitle();
			}
		).dimensions(width / 2 + GAME_MODE_BUTTON_X_OFFSET, height - BOTTOM_BUTTON_Y_OFFSET, BUTTON_WIDTH, BUTTON_HEIGHT).build();

		portField = new TextFieldWidget(
			textRenderer,
			width / 2 - PORT_FIELD_X_OFFSET,
			PORT_FIELD_Y,
			PORT_FIELD_WIDTH,
			BUTTON_HEIGHT,
			Text.translatable("lanServer.port")
		);
		portField.setChangedListener(portText -> {
			Text error = updatePort(portText);
			portField.setPlaceholder(Text.literal(port + ""));

			if (error == null) {
				portField.setEditableColor(COLOR_VALID);
				portField.setTooltip(null);
				startButton.active = true;
			} else {
				portField.setEditableColor(COLOR_INVALID);
				portField.setTooltip(Tooltip.of(error));
				startButton.active = false;
			}
		});
		portField.setPlaceholder(Text.literal(port + ""));

		addDrawableChild(portField);
		addDrawableChild(startButton);
		addDrawableChild(
			ButtonWidget.builder(ScreenTexts.CANCEL, button -> close())
				.dimensions(width / 2 + COMMANDS_BUTTON_X_OFFSET, height - BOTTOM_BUTTON_Y_OFFSET, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build()
		);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private @Nullable Text updatePort(String portText) {
		if (portText.isBlank()) {
			port = NetworkUtils.findLocalPort();
			return null;
		}

		try {
			port = Integer.parseInt(portText);

			if (port < MIN_PORT || port > MAX_PORT) {
				return INVALID_PORT_TEXT;
			}

			return !NetworkUtils.isPortAvailable(port) ? UNAVAILABLE_PORT_TEXT : null;
		} catch (NumberFormatException ignored) {
			port = NetworkUtils.findLocalPort();
			return INVALID_PORT_TEXT;
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, -1);
		context.drawCenteredTextWithShadow(textRenderer, OTHER_PLAYERS_TEXT, width / 2, OTHER_PLAYERS_Y, -1);
		context.drawCenteredTextWithShadow(textRenderer, PORT_TEXT, width / 2, PORT_LABEL_Y, -1);
	}
}
