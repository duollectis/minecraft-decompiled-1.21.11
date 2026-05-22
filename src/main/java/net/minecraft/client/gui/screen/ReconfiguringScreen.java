package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.network.ClientConnection;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Экран ожидания переконфигурации соединения с сервером.
 * Кнопка отключения активируется только после {@link #DISCONNECT_BUTTON_ACTIVATION_TICK} тиков,
 * чтобы предотвратить случайное прерывание процесса.
 */
@Environment(EnvType.CLIENT)
public class ReconfiguringScreen extends Screen {

	private static final int DISCONNECT_BUTTON_ACTIVATION_TICK = 600;

	private final ClientConnection connection;
	private final DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical();
	private ButtonWidget disconnectButton;
	private int tick;

	public ReconfiguringScreen(Text title, ClientConnection connection) {
		super(title);
		this.connection = connection;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	protected void init() {
		layout.getMainPositioner().alignHorizontalCenter().margin(10);
		layout.add(new TextWidget(title, textRenderer));
		disconnectButton = layout.add(
			ButtonWidget.builder(ScreenTexts.DISCONNECT, button -> connection.disconnect(ConnectScreen.ABORTED_TEXT))
				.build()
		);
		disconnectButton.active = false;
		layout.refreshPositions();
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		SimplePositioningWidget.setPos(layout, getNavigationFocus());
	}

	@Override
	public void tick() {
		super.tick();
		tick++;

		if (tick == DISCONNECT_BUTTON_ACTIVATION_TICK) {
			disconnectButton.active = true;
		}

		if (!connection.isOpen()) {
			connection.handleDisconnection();
			return;
		}

		connection.tick();
	}
}
