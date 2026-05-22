package net.minecraft.client.session.report.log;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringIdentifiable;

import java.util.function.Supplier;

/**
 * Запись в логе чата. Может представлять сообщение игрока или системное сообщение.
 * Тип записи определяет codec для сериализации конкретной реализации.
 */
@Environment(EnvType.CLIENT)
public interface ChatLogEntry {

	Codec<ChatLogEntry> CODEC = StringIdentifiable
		.createCodec(ChatLogEntry.Type::values)
		.dispatch(ChatLogEntry::getType, ChatLogEntry.Type::getCodec);

	ChatLogEntry.Type getType();

	/**
	 * Тип записи лога чата, определяющий codec для десериализации.
	 * {@link #PLAYER} — сообщение от игрока с подписью,
	 * {@link #SYSTEM} — системное сообщение без подписи.
	 */
	@Environment(EnvType.CLIENT)
	enum Type implements StringIdentifiable {
		PLAYER("player", () -> ReceivedMessage.ChatMessage.CHAT_MESSAGE_CODEC),
		SYSTEM("system", () -> ReceivedMessage.GameMessage.GAME_MESSAGE_CODEC);

		private final String id;
		private final Supplier<MapCodec<? extends ChatLogEntry>> codecSupplier;

		Type(String id, Supplier<MapCodec<? extends ChatLogEntry>> codecSupplier) {
			this.id = id;
			this.codecSupplier = codecSupplier;
		}

		private MapCodec<? extends ChatLogEntry> getCodec() {
			return codecSupplier.get();
		}

		@Override
		public String asString() {
			return id;
		}
	}
}
