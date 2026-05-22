package net.minecraft.network.message;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.encryption.SignatureUpdatable;
import net.minecraft.util.dynamic.Codecs;

import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Optional;

/**
 * Тело подписанного чат-сообщения: текст, временная метка, соль и список последних просмотренных.
 * Все поля включаются в криптографическую подпись для защиты от подмены.
 */
public record MessageBody(String content, Instant timestamp, long salt, LastSeenMessageList lastSeenMessages) {

	private static final int MAX_CONTENT_LENGTH = 256;

	public static final MapCodec<MessageBody> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Codec.STRING.fieldOf("content").forGetter(MessageBody::content),
					                    Codecs.INSTANT.fieldOf("time_stamp").forGetter(MessageBody::timestamp),
					                    Codec.LONG.fieldOf("salt").forGetter(MessageBody::salt),
					                    LastSeenMessageList.CODEC
							                    .optionalFieldOf("last_seen", LastSeenMessageList.EMPTY)
							                    .forGetter(MessageBody::lastSeenMessages)
			                    )
			                    .apply(instance, MessageBody::new)
	);

	public static MessageBody ofUnsigned(String content) {
		return new MessageBody(content, Instant.now(), 0L, LastSeenMessageList.EMPTY);
	}

	/**
	 * Добавляет все поля тела сообщения в обновляемый объект подписи.
	 * Порядок: соль → временная метка → длина контента → контент → последние просмотренные.
	 *
	 * @param updater объект для накопления данных подписи
	 * @throws SignatureException при ошибке криптографической операции
	 */
	public void update(SignatureUpdatable.SignatureUpdater updater) throws SignatureException {
		updater.update(Longs.toByteArray(salt));
		updater.update(Longs.toByteArray(timestamp.getEpochSecond()));
		byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
		updater.update(Ints.toByteArray(contentBytes.length));
		updater.update(contentBytes);
		lastSeenMessages.updateSignatures(updater);
	}

	public MessageBody.Serialized toSerialized(MessageSignatureStorage storage) {
		return new MessageBody.Serialized(content, timestamp, salt, lastSeenMessages.pack(storage));
	}

	/**
	 * Сериализованное тело сообщения для передачи по сети.
	 * Список последних просмотренных хранится в компактном индексированном виде.
	 */
	public record Serialized(String content, Instant timestamp, long salt, LastSeenMessageList.Indexed lastSeen) {

		public Serialized(PacketByteBuf buf) {
			this(buf.readString(MAX_CONTENT_LENGTH), buf.readInstant(), buf.readLong(), new LastSeenMessageList.Indexed(buf));
		}

		public void write(PacketByteBuf buf) {
			buf.writeString(content, MAX_CONTENT_LENGTH);
			buf.writeInstant(timestamp);
			buf.writeLong(salt);
			lastSeen.write(buf);
		}

		/**
		 * Разворачивает сериализованное тело в полный {@link MessageBody}, восстанавливая
		 * подписи из хранилища.
		 *
		 * @param storage хранилище известных подписей
		 * @return полное тело сообщения, или {@link Optional#empty()} если подписи не найдены
		 */
		public Optional<MessageBody> toBody(MessageSignatureStorage storage) {
			return lastSeen
					.unpack(storage)
					.map(lastSeenMessages -> new MessageBody(content, timestamp, salt, lastSeenMessages));
		}
	}
}
