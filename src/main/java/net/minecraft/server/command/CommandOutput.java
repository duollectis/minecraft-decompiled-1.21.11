package net.minecraft.server.command;

import net.minecraft.text.Text;

/**
 * Получатель вывода команды: игрок, консоль или заглушка.
 * Определяет, должен ли получатель видеть обратную связь, трекинг вывода и трансляцию операторам.
 */
public interface CommandOutput {

	CommandOutput DUMMY = new CommandOutput() {
		@Override
		public void sendMessage(Text message) {
		}

		@Override
		public boolean shouldReceiveFeedback() {
			return false;
		}

		@Override
		public boolean shouldTrackOutput() {
			return false;
		}

		@Override
		public boolean shouldBroadcastConsoleToOps() {
			return false;
		}
	};

	void sendMessage(Text message);

	boolean shouldReceiveFeedback();

	boolean shouldTrackOutput();

	boolean shouldBroadcastConsoleToOps();

	default boolean cannotBeSilenced() {
		return false;
	}
}
