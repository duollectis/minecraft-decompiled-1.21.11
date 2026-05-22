package net.minecraft.network.message;

import com.google.common.primitives.Ints;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.encryption.SignatureUpdatable;
import net.minecraft.util.Util;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.security.SignatureException;
import java.util.UUID;

/**
 * Ссылка на конкретное сообщение в цепочке подписей.
 * Содержит порядковый номер, UUID отправителя и UUID сессии.
 * Цепочка обрывается при достижении {@link Integer#MAX_VALUE}.
 */
public record MessageLink(int index, UUID sender, UUID sessionId) {

	public static final Codec<MessageLink> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codecs.NON_NEGATIVE_INT.fieldOf("index").forGetter(MessageLink::index),
					                    Uuids.INT_STREAM_CODEC.fieldOf("sender").forGetter(MessageLink::sender),
					                    Uuids.INT_STREAM_CODEC.fieldOf("session_id").forGetter(MessageLink::sessionId)
			                    )
			                    .apply(instance, MessageLink::new)
	);

	public static MessageLink of(UUID sender) {
		return of(sender, Util.NIL_UUID);
	}

	public static MessageLink of(UUID sender, UUID sessionId) {
		return new MessageLink(0, sender, sessionId);
	}

	/**
	 * Добавляет данные ссылки в обновляемый объект подписи.
	 * Порядок: UUID отправителя → UUID сессии → индекс.
	 *
	 * @param updater объект для накопления данных подписи
	 * @throws SignatureException при ошибке криптографической операции
	 */
	public void update(SignatureUpdatable.SignatureUpdater updater) throws SignatureException {
		updater.update(Uuids.toByteArray(sender));
		updater.update(Uuids.toByteArray(sessionId));
		updater.update(Ints.toByteArray(index));
	}

	/**
	 * Проверяет, является ли данная ссылка непосредственным продолжением {@code preceding}.
	 * Требует: тот же отправитель, та же сессия, больший индекс.
	 *
	 * @param preceding предшествующая ссылка
	 * @return {@code true} если данная ссылка следует за {@code preceding}
	 */
	public boolean linksTo(MessageLink preceding) {
		return index > preceding.index()
				&& sender.equals(preceding.sender())
				&& sessionId.equals(preceding.sessionId());
	}

	/**
	 * Возвращает следующую ссылку в цепочке с увеличенным индексом.
	 * При достижении {@link Integer#MAX_VALUE} цепочка обрывается.
	 *
	 * @return следующая ссылка, или {@code null} если индекс исчерпан
	 */
	public @Nullable MessageLink next() {
		return index == Integer.MAX_VALUE
				? null
				: new MessageLink(index + 1, sender, sessionId);
	}
}
