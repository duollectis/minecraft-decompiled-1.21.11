package net.minecraft.client.gui.screen.multiplayer;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Экран добавления нового сервера в список серверов.
 */
@Environment(EnvType.CLIENT)
public class AddServerScreen extends Screen {

	private static final Text ENTER_NAME_TEXT = Text.translatable("manageServer.enterName");
	private static final Text ENTER_IP_TEXT = Text.translatable("manageServer.enterIp");
	private static final Text DEFAULT_SERVER_NAME = Text.translatable("selectServer.defaultName");
	private static final int FIELD_WIDTH = 200;
	private static final int FIELD_HEIGHT = 20;
	private static final int ADDRESS_MAX_LENGTH = 128;
	private static final int TITLE_Y = 17;
	private static final int NAME_FIELD_Y = 66;
	private static final int ADDRESS_FIELD_Y = 106;
	private static final int NAME_LABEL_Y = 53;
	private static final int ADDRESS_LABEL_Y = 94;
	private static final int RESOURCE_PACK_BUTTON_Y_OFFSET = 72;
	private static final int DONE_BUTTON_Y_OFFSET = 96 + 18;
	private static final int CANCEL_BUTTON_Y_OFFSET = 120 + 18;
	private static final int TEXT_COLOR_LABEL = -6250336;
	private static final int TEXT_COLOR_WHITE = -1;

	private ButtonWidget addButton;
	private final BooleanConsumer callback;
	private final ServerInfo server;
	private TextFieldWidget addressField;
	private TextFieldWidget serverNameField;
	private final Screen parent;

	public AddServerScreen(Screen parent, Text text, BooleanConsumer booleanConsumer, ServerInfo serverInfo) {
		super(text);
		this.parent = parent;
		callback = booleanConsumer;
		server = serverInfo;
	}

	@Override
	protected void init() {
		serverNameField = new TextFieldWidget(textRenderer, width / 2 - 100, NAME_FIELD_Y, FIELD_WIDTH, FIELD_HEIGHT, ENTER_NAME_TEXT);
		serverNameField.setText(server.name);
		serverNameField.setPlaceholder(DEFAULT_SERVER_NAME);
		serverNameField.setChangedListener(serverName -> updateAddButton());
		addSelectableChild(serverNameField);

		addressField = new TextFieldWidget(textRenderer, width / 2 - 100, ADDRESS_FIELD_Y, FIELD_WIDTH, FIELD_HEIGHT, ENTER_IP_TEXT);
		addressField.setMaxLength(ADDRESS_MAX_LENGTH);
		addressField.setText(server.address);
		addressField.setChangedListener(address -> updateAddButton());
		addSelectableChild(addressField);

		addDrawableChild(
			CyclingButtonWidget.builder(ServerInfo.ResourcePackPolicy::getName, server.getResourcePackPolicy())
				.values(ServerInfo.ResourcePackPolicy.values())
				.build(
					width / 2 - 100,
					height / 4 + RESOURCE_PACK_BUTTON_Y_OFFSET,
					FIELD_WIDTH,
					FIELD_HEIGHT,
					Text.translatable("manageServer.resourcePack"),
					(button, policy) -> server.setResourcePackPolicy(policy)
				)
		);

		addButton = addDrawableChild(
			ButtonWidget.builder(ScreenTexts.DONE, button -> addAndClose())
				.dimensions(width / 2 - 100, height / 4 + DONE_BUTTON_Y_OFFSET, FIELD_WIDTH, FIELD_HEIGHT)
				.build()
		);

		addDrawableChild(
			ButtonWidget.builder(ScreenTexts.CANCEL, button -> callback.accept(false))
				.dimensions(width / 2 - 100, height / 4 + CANCEL_BUTTON_Y_OFFSET, FIELD_WIDTH, FIELD_HEIGHT)
				.build()
		);

		updateAddButton();
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(serverNameField);
	}

	@Override
	public void resize(int width, int height) {
		String savedAddress = addressField.getText();
		String savedName = serverNameField.getText();
		init(width, height);
		addressField.setText(savedAddress);
		serverNameField.setText(savedName);
	}

	private void addAndClose() {
		String name = serverNameField.getText();
		server.name = name.isEmpty() ? DEFAULT_SERVER_NAME.getString() : name;
		server.address = addressField.getText();
		callback.accept(true);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private void updateAddButton() {
		addButton.active = ServerAddress.isValid(addressField.getText());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, TEXT_COLOR_WHITE);
		context.drawTextWithShadow(textRenderer, ENTER_NAME_TEXT, width / 2 - 100 + 1, NAME_LABEL_Y, TEXT_COLOR_LABEL);
		context.drawTextWithShadow(textRenderer, ENTER_IP_TEXT, width / 2 - 100 + 1, ADDRESS_LABEL_Y, TEXT_COLOR_LABEL);
		serverNameField.render(context, mouseX, mouseY, deltaTicks);
		addressField.render(context, mouseX, mouseY, deltaTicks);
	}
}
