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
 * Запись message link.
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

	/**
	 * Of.
	 *
	 * @param sender sender
	 *
	 * @return MessageLink — результат операции
	 */
	public static MessageLink of(UUID sender) {
		return of(sender, Util.NIL_UUID);
	}

	/**
	 * Of.
	 *
	 * @param sender sender
	 * @param sessionId session id
	 *
	 * @return MessageLink — результат операции
	 */
	public static MessageLink of(UUID sender, UUID sessionId) {
		return new MessageLink(0, sender, sessionId);
	}

	/**
	 * Update.
	 *
	 * @param updater updater
	 */
	public void update(SignatureUpdatable.SignatureUpdater updater) throws SignatureException {
		updater.update(Uuids.toByteArray(this.sender));
		updater.update(Uuids.toByteArray(this.sessionId));
		updater.update(Ints.toByteArray(this.index));
	}

	/**
	 * Links to.
	 *
	 * @param preceding preceding
	 *
	 * @return boolean — результат операции
	 */
	public boolean linksTo(MessageLink preceding) {
		return this.index > preceding.index() && this.sender.equals(preceding.sender()) && this.sessionId.equals(
				preceding.sessionId());
	}

	/**
	 * Next.
	 *
	 * @return @Nullable MessageLink — результат операции
	 */
	public @Nullable MessageLink next() {
		return this.index == Integer.MAX_VALUE ? null : new MessageLink(this.index + 1, this.sender, this.sessionId);
	}
}
