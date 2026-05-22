package net.minecraft.network.message;

import com.google.common.primitives.Ints;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.encryption.SignatureUpdatable;
import net.minecraft.network.encryption.SignatureVerifier;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Подписанное чат-сообщение с полным набором метаданных для верификации.
 * Содержит ссылку в цепочке, подпись, тело, опциональный неподписанный контент и маску фильтрации.
 * Сообщение считается истёкшим через {@link #SERVERBOUND_TIME_TO_LIVE} на сервере
 * и через {@link #CLIENTBOUND_TIME_TO_LIVE} на клиенте.
 */
public record SignedMessage(
		MessageLink link,
		@Nullable MessageSignatureData signature,
		MessageBody signedBody,
		@Nullable Text unsignedContent,
		FilterMask filterMask
) {

	public static final MapCodec<SignedMessage> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    MessageLink.CODEC.fieldOf("link").forGetter(SignedMessage::link),
					                    MessageSignatureData.CODEC
							                    .optionalFieldOf("signature")
							                    .forGetter(message -> Optional.ofNullable(message.signature)),
					                    MessageBody.CODEC.forGetter(SignedMessage::signedBody),
					                    TextCodecs.CODEC
							                    .optionalFieldOf("unsigned_content")
							                    .forGetter(message -> Optional.ofNullable(message.unsignedContent)),
					                    FilterMask.CODEC
							                    .optionalFieldOf("filter_mask", FilterMask.PASS_THROUGH)
							                    .forGetter(SignedMessage::filterMask)
			                    )
			                    .apply(
					                    instance,
					                    (link, signature, signedBody, unsignedContent, filterMask) -> new SignedMessage(
							                    link,
							                    signature.orElse(null),
							                    signedBody,
							                    unsignedContent.orElse(null),
							                    filterMask
					                    )
			                    )
	);

	private static final UUID NIL_UUID = Util.NIL_UUID;
	public static final Duration SERVERBOUND_TIME_TO_LIVE = Duration.ofMinutes(5L);
	public static final Duration CLIENTBOUND_TIME_TO_LIVE = SERVERBOUND_TIME_TO_LIVE.plus(Duration.ofMinutes(2L));

	public static SignedMessage ofUnsigned(String content) {
		return ofUnsigned(NIL_UUID, content);
	}

	public static SignedMessage ofUnsigned(UUID sender, String content) {
		MessageBody body = MessageBody.ofUnsigned(content);
		MessageLink messageLink = MessageLink.of(sender);
		return new SignedMessage(messageLink, null, body, null, FilterMask.PASS_THROUGH);
	}

	/**
	 * Возвращает копию с заменённым неподписанным контентом.
	 * Если переданный текст совпадает с подписанным содержимым, неподписанный контент сбрасывается.
	 *
	 * @param newUnsignedContent новый неподписанный текст для отображения
	 * @return новый экземпляр с обновлённым неподписанным контентом
	 */
	public SignedMessage withUnsignedContent(Text newUnsignedContent) {
		Text resolved = newUnsignedContent.equals(Text.literal(getSignedContent())) ? null : newUnsignedContent;
		return new SignedMessage(link, signature, signedBody, resolved, filterMask);
	}

	public SignedMessage withoutUnsigned() {
		return unsignedContent != null
				? new SignedMessage(link, signature, signedBody, null, filterMask)
				: this;
	}

	public SignedMessage withFilterMask(FilterMask newFilterMask) {
		return filterMask.equals(newFilterMask)
				? this
				: new SignedMessage(link, signature, signedBody, unsignedContent, newFilterMask);
	}

	public SignedMessage withFilterMaskEnabled(boolean enabled) {
		return withFilterMask(enabled ? filterMask : FilterMask.PASS_THROUGH);
	}

	/**
	 * Создаёт неподписанную копию сообщения, сбрасывая цепочку подписей.
	 * Используется когда верификация невозможна, но сообщение нужно отобразить.
	 *
	 * @return неподписанная копия с сохранённым контентом и маской фильтрации
	 */
	public SignedMessage stripSignature() {
		MessageBody strippedBody = MessageBody.ofUnsigned(getSignedContent());
		MessageLink strippedLink = MessageLink.of(getSender());
		return new SignedMessage(strippedLink, null, strippedBody, unsignedContent, filterMask);
	}

	/**
	 * Добавляет данные сообщения в обновляемый объект подписи.
	 * Версия протокола (1) записывается первой для совместимости.
	 *
	 * @param updater  объект для накопления данных подписи
	 * @param link     ссылка в цепочке
	 * @param body     тело сообщения
	 * @throws SignatureException при ошибке криптографической операции
	 */
	public static void update(SignatureUpdatable.SignatureUpdater updater, MessageLink link, MessageBody body)
	throws SignatureException {
		updater.update(Ints.toByteArray(1));
		link.update(updater);
		body.update(updater);
	}

	public boolean verify(SignatureVerifier verifier) {
		return signature != null && signature.verify(
				verifier,
				updater -> update(updater, link, signedBody)
		);
	}

	public String getSignedContent() {
		return signedBody.content();
	}

	public Text getContent() {
		return Objects.requireNonNullElseGet(unsignedContent, () -> Text.literal(getSignedContent()));
	}

	public Instant getTimestamp() {
		return signedBody.timestamp();
	}

	public long getSalt() {
		return signedBody.salt();
	}

	public boolean isExpiredOnServer(Instant currentTime) {
		return currentTime.isAfter(getTimestamp().plus(SERVERBOUND_TIME_TO_LIVE));
	}

	public boolean isExpiredOnClient(Instant currentTime) {
		return currentTime.isAfter(getTimestamp().plus(CLIENTBOUND_TIME_TO_LIVE));
	}

	public UUID getSender() {
		return link.sender();
	}

	public boolean isSenderMissing() {
		return getSender().equals(NIL_UUID);
	}

	public boolean hasSignature() {
		return signature != null;
	}

	public boolean canVerifyFrom(UUID sender) {
		return hasSignature() && link.sender().equals(sender);
	}

	public boolean isFullyFiltered() {
		return filterMask.isFullyFiltered();
	}

	public static String toString(SignedMessage message) {
		return "'"
				+ message.signedBody.content()
				+ "' @ "
				+ message.signedBody.timestamp()
				+ "\n - From: "
				+ message.link.sender()
				+ "/"
				+ message.link.sessionId()
				+ ", message #"
				+ message.link.index()
				+ "\n - Salt: "
				+ message.signedBody.salt()
				+ "\n - Signature: "
				+ MessageSignatureData.toString(message.signature)
				+ "\n - Last Seen: [\n"
				+ message.signedBody
				.lastSeenMessages()
				.entries()
				.stream()
				.map(entry -> "     " + MessageSignatureData.toString(entry) + "\n")
				.collect(Collectors.joining())
				+ " ]\n";
	}
}
