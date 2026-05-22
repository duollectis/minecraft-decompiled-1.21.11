package net.minecraft.network.message;

import com.mojang.logging.LogUtils;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.encryption.SignatureVerifier;
import net.minecraft.network.encryption.Signer;
import net.minecraft.text.Text;
import net.minecraft.util.TextifiedException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Цепочка подписанных сообщений одного игрока в рамках одной сессии.
 * Каждое сообщение ссылается на предыдущее через {@link MessageLink}, образуя
 * криптографически верифицируемую последовательность.
 */
public class MessageChain {

	static final Logger LOGGER = LogUtils.getLogger();

	@Nullable MessageLink link;
	Instant lastTimestamp = Instant.EPOCH;

	public MessageChain(UUID sender, UUID sessionId) {
		link = MessageLink.of(sender, sessionId);
	}

	/**
	 * Создаёт упаковщик сообщений, подписывающий каждое тело с помощью переданного {@link Signer}.
	 * После исчерпания цепочки (переполнение индекса) возвращает {@code null}.
	 *
	 * @param signer криптографический подписчик
	 * @return упаковщик, привязанный к текущей цепочке
	 */
	public MessageChain.Packer getPacker(Signer signer) {
		return body -> {
			MessageLink currentLink = link;

			if (currentLink == null) {
				return null;
			}

			link = currentLink.next();
			return new MessageSignatureData(signer.sign(
					updatable -> SignedMessage.update(updatable, currentLink, body)
			));
		};
	}

	/**
	 * Создаёт распаковщик сообщений, верифицирующий подписи с помощью публичного ключа игрока.
	 * Проверяет: наличие подписи, срок действия ключа, порядок сообщений и корректность подписи.
	 *
	 * @param playerPublicKey публичный ключ игрока для верификации
	 * @return распаковщик, привязанный к текущей цепочке
	 */
	public MessageChain.Unpacker getUnpacker(PlayerPublicKey playerPublicKey) {
		final SignatureVerifier signatureVerifier = playerPublicKey.createSignatureInstance();

		return new MessageChain.Unpacker() {
			@Override
			public SignedMessage unpack(@Nullable MessageSignatureData signature, MessageBody body)
			throws MessageChain.MessageChainException {
				if (signature == null) {
					throw new MessageChainException(MessageChainException.MISSING_PROFILE_KEY_EXCEPTION);
				}

				if (playerPublicKey.data().isExpired()) {
					throw new MessageChainException(MessageChainException.EXPIRED_PROFILE_KEY_EXCEPTION);
				}

				MessageLink currentLink = MessageChain.this.link;

				if (currentLink == null) {
					throw new MessageChainException(MessageChainException.CHAIN_BROKEN_EXCEPTION);
				}

				if (body.timestamp().isBefore(MessageChain.this.lastTimestamp)) {
					setChainBroken();
					throw new MessageChainException(MessageChainException.OUT_OF_ORDER_CHAT_EXCEPTION);
				}

				MessageChain.this.lastTimestamp = body.timestamp();
				SignedMessage signedMessage = new SignedMessage(
						currentLink,
						signature,
						body,
						null,
						FilterMask.PASS_THROUGH
				);

				if (!signedMessage.verify(signatureVerifier)) {
					setChainBroken();
					throw new MessageChainException(MessageChainException.INVALID_SIGNATURE_EXCEPTION);
				}

				if (signedMessage.isExpiredOnServer(Instant.now())) {
					LOGGER.warn(
							"Received expired chat: '{}'. Is the client/server system time unsynchronized?",
							body.content()
					);
				}

				MessageChain.this.link = currentLink.next();
				return signedMessage;
			}

			@Override
			public void setChainBroken() {
				MessageChain.this.link = null;
			}
		};
	}

	public static class MessageChainException extends TextifiedException {

		static final Text MISSING_PROFILE_KEY_EXCEPTION = Text.translatable("chat.disabled.missingProfileKey");
		static final Text CHAIN_BROKEN_EXCEPTION = Text.translatable("chat.disabled.chain_broken");
		static final Text EXPIRED_PROFILE_KEY_EXCEPTION = Text.translatable("chat.disabled.expiredProfileKey");
		static final Text INVALID_SIGNATURE_EXCEPTION = Text.translatable("chat.disabled.invalid_signature");
		static final Text OUT_OF_ORDER_CHAT_EXCEPTION = Text.translatable("chat.disabled.out_of_order_chat");

		public MessageChainException(Text message) {
			super(message);
		}
	}

	/**
	 * Упаковщик тела сообщения в подписанную форму.
	 * Возвращает {@code null} если цепочка исчерпана.
	 */
	@FunctionalInterface
	public interface Packer {

		Packer NONE = body -> null;

		@Nullable MessageSignatureData pack(MessageBody body);
	}

	/**
	 * Распаковщик и верификатор входящих подписанных сообщений.
	 */
	@FunctionalInterface
	public interface Unpacker {

		/**
		 * Создаёт распаковщик для неподписанных сообщений.
		 * Если включён режим обязательной безопасности, выбрасывает исключение.
		 *
		 * @param sender              UUID отправителя
		 * @param secureProfileEnforced поставщик флага обязательной безопасности
		 * @return распаковщик для неподписанных сообщений
		 */
		static MessageChain.Unpacker unsigned(UUID sender, BooleanSupplier secureProfileEnforced) {
			return (signature, body) -> {
				if (secureProfileEnforced.getAsBoolean()) {
					throw new MessageChainException(MessageChainException.MISSING_PROFILE_KEY_EXCEPTION);
				}

				return SignedMessage.ofUnsigned(sender, body.content());
			};
		}

		SignedMessage unpack(@Nullable MessageSignatureData signature, MessageBody body)
		throws MessageChain.MessageChainException;

		default void setChainBroken() {
		}
	}
}
