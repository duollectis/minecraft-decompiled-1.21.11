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

@Environment(EnvType.CLIENT)
/**
 * {@code SleepingChatScreen}.
 */
public class SleepingChatScreen extends ChatScreen {

	private ButtonWidget stopSleepingButton;

	public SleepingChatScreen(String string, boolean bl) {
		super(string, bl);
	}

	@Override
	protected void init() {
		super.init();
		this.stopSleepingButton =
				ButtonWidget.builder(Text.translatable("multiplayer.stopSleeping"), button -> this.stopSleeping())
				            .dimensions(this.width / 2 - 100, this.height - 40, 200, 20)
				            .build();
		this.addDrawableChild(this.stopSleepingButton);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (!this.client.getChatRestriction().allowsChat(this.client.isInSingleplayer())) {
			this.stopSleepingButton.render(context, mouseX, mouseY, deltaTicks);
		}
		else {
			super.render(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Override
	public void close() {
		this.stopSleeping();
	}

	@Override
	public boolean charTyped(CharInput input) {
		return !this.client.getChatRestriction().allowsChat(this.client.isInSingleplayer()) ? true
		                                                                                    : super.charTyped(input);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEscape()) {
			this.stopSleeping();
		}

		if (!this.client.getChatRestriction().allowsChat(this.client.isInSingleplayer())) {
			return true;
		}
		else if (input.isEnter()) {
			this.sendMessage(this.chatField.getText(), true);
			this.chatField.setText("");
			this.client.inGameHud.getChatHud().resetScroll();
			return true;
		}
		else {
			return super.keyPressed(input);
		}
	}

	private void stopSleeping() {
		ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.player.networkHandler;
		clientPlayNetworkHandler.sendPacket(new ClientCommandC2SPacket(
				this.client.player,
				ClientCommandC2SPacket.Mode.STOP_SLEEPING
		));
	}

	public void closeChatIfEmpty() {
		String string = this.chatField.getText();
		if (!this.draft && !string.isEmpty()) {
			this.closeReason = ChatScreen.CloseReason.DONE;
			this.client.setScreen(new ChatScreen(string, false));
		}
		else {
			this.closeReason = ChatScreen.CloseReason.INTERRUPTED;
			this.client.setScreen(null);
		}
	}
}
