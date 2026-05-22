package net.minecraft.client.gui.screen.multiplayer;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Экран прямого подключения к серверу по IP-адресу.
 */
@Environment(EnvType.CLIENT)
public class DirectConnectScreen extends Screen {

	private static final Text ENTER_IP_TEXT = Text.translatable("manageServer.enterIp");
	private static final int FIELD_WIDTH = 200;
	private static final int FIELD_HEIGHT = 20;
	private static final int ADDRESS_MAX_LENGTH = 128;
	private static final int TITLE_Y = 20;
	private static final int LABEL_Y = 100;
	private static final int FIELD_Y = 116;
	private static final int CONNECT_BUTTON_Y_OFFSET = 96 + 12;
	private static final int CANCEL_BUTTON_Y_OFFSET = 120 + 12;
	private static final int TEXT_COLOR_LABEL = -6250336;

	private ButtonWidget selectServerButton;
	private final ServerInfo serverEntry;
	private TextFieldWidget addressField;
	private final BooleanConsumer callback;
	private final Screen parent;

	public DirectConnectScreen(Screen parent, BooleanConsumer callback, ServerInfo server) {
		super(Text.translatable("selectServer.direct"));
		this.parent = parent;
		serverEntry = server;
		this.callback = callback;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (selectServerButton.active && getFocused() == addressField && input.isEnter()) {
			saveAndClose();
			return true;
		}

		return super.keyPressed(input);
	}

	@Override
	protected void init() {
		addressField = new TextFieldWidget(textRenderer, width / 2 - 100, FIELD_Y, FIELD_WIDTH, FIELD_HEIGHT, ENTER_IP_TEXT);
		addressField.setMaxLength(ADDRESS_MAX_LENGTH);
		addressField.setText(client.options.lastServer);
		addressField.setChangedListener(text -> onAddressFieldChanged());
		addSelectableChild(addressField);
		selectServerButton = addDrawableChild(
			ButtonWidget.builder(Text.translatable("selectServer.select"), button -> saveAndClose())
				.dimensions(width / 2 - 100, height / 4 + CONNECT_BUTTON_Y_OFFSET, FIELD_WIDTH, FIELD_HEIGHT)
				.build()
		);
		addDrawableChild(
			ButtonWidget.builder(ScreenTexts.CANCEL, button -> callback.accept(false))
				.dimensions(width / 2 - 100, height / 4 + CANCEL_BUTTON_Y_OFFSET, FIELD_WIDTH, FIELD_HEIGHT)
				.build()
		);
		onAddressFieldChanged();
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(addressField);
	}

	@Override
	public void resize(int width, int height) {
		String savedAddress = addressField.getText();
		init(width, height);
		addressField.setText(savedAddress);
	}

	private void saveAndClose() {
		serverEntry.address = addressField.getText();
		callback.accept(true);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	@Override
	public void removed() {
		client.options.lastServer = addressField.getText();
		client.options.write();
	}

	private void onAddressFieldChanged() {
		selectServerButton.active = ServerAddress.isValid(addressField.getText());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, -1);
		context.drawTextWithShadow(textRenderer, ENTER_IP_TEXT, width / 2 - 100 + 1, LABEL_Y, TEXT_COLOR_LABEL);
		addressField.render(context, mouseX, mouseY, deltaTicks);
	}
}
