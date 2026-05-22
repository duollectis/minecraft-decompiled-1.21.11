package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.text.Text;

/**
 * Экран чата, отображаемый во время сна персонажа.
 * Добавляет кнопку прерывания сна и ограничивает ввод при запрете чата.
 */
@Environment(EnvType.CLIENT)
public class SleepingChatScreen extends ChatScreen {

	private static final int BUTTON_WIDTH = 200;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_Y_OFFSET = 40;

	private ButtonWidget stopSleepingButton;

	public SleepingChatScreen(String string, boolean draft) {
		super(string, draft);
	}

	@Override
	protected void init() {
		super.init();
		stopSleepingButton = ButtonWidget.builder(Text.translatable("multiplayer.stopSleeping"), button -> stopSleeping())
			.dimensions(width / 2 - BUTTON_WIDTH / 2, height - BUTTON_Y_OFFSET, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build();
		addDrawableChild(stopSleepingButton);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (client.getChatRestriction().allowsChat(client.isInSingleplayer())) {
			super.render(context, mouseX, mouseY, deltaTicks);
			return;
		}

		stopSleepingButton.render(context, mouseX, mouseY, deltaTicks);
	}

	@Override
	public void close() {
		stopSleeping();
	}

	@Override
	public boolean charTyped(CharInput input) {
		return !client.getChatRestriction().allowsChat(client.isInSingleplayer()) || super.charTyped(input);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEscape()) {
			stopSleeping();
		}

		if (!client.getChatRestriction().allowsChat(client.isInSingleplayer())) {
			return true;
		}

		if (input.isEnter()) {
			sendMessage(chatField.getText(), true);
			chatField.setText("");
			client.inGameHud.getChatHud().resetScroll();
			return true;
		}

		return super.keyPressed(input);
	}

	private void stopSleeping() {
		ClientPlayNetworkHandler networkHandler = client.player.networkHandler;
		networkHandler.sendPacket(new ClientCommandC2SPacket(client.player, ClientCommandC2SPacket.Mode.STOP_SLEEPING));
	}

	/**
	 * Закрывает чат, если поле ввода пустое, или переносит черновик в обычный экран чата.
	 */
	public void closeChatIfEmpty() {
		String text = chatField.getText();

		if (!draft && !text.isEmpty()) {
			closeReason = ChatScreen.CloseReason.DONE;
			client.setScreen(new ChatScreen(text, false));
		} else {
			closeReason = ChatScreen.CloseReason.INTERRUPTED;
			client.setScreen(null);
		}
	}
}
