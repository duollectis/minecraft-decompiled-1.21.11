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
 * Запись signed message.
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
							                    (MessageSignatureData) signature.orElse(null),
							                    signedBody,
							                    (Text) unsignedContent.orElse(null),
							                    filterMask
					                    )
			                    )
	);
	private static final UUID NIL_UUID = Util.NIL_UUID;
	public static final Duration SERVERBOUND_TIME_TO_LIVE = Duration.ofMinutes(5L);
	public static final Duration CLIENTBOUND_TIME_TO_LIVE = SERVERBOUND_TIME_TO_LIVE.plus(Duration.ofMinutes(2L));

	/**
	 * Of unsigned.
	 *
	 * @param content content
	 *
	 * @return SignedMessage — результат операции
	 */
	public static SignedMessage ofUnsigned(String content) {
		return ofUnsigned(NIL_UUID, content);
	}

	/**
	 * Of unsigned.
	 *
	 * @param sender sender
	 * @param content content
	 *
	 * @return SignedMessage — результат операции
	 */
	public static SignedMessage ofUnsigned(UUID sender, String content) {
		MessageBody messageBody = MessageBody.ofUnsigned(content);
		MessageLink messageLink = MessageLink.of(sender);
		return new SignedMessage(messageLink, null, messageBody, null, FilterMask.PASS_THROUGH);
	}

	/**
	 * With unsigned content.
	 *
	 * @param unsignedContent unsigned content
	 *
	 * @return SignedMessage — результат операции
	 */
	public SignedMessage withUnsignedContent(Text unsignedContent) {
		Text text = !unsignedContent.equals(Text.literal(this.getSignedContent())) ? unsignedContent : null;
		return new SignedMessage(this.link, this.signature, this.signedBody, text, this.filterMask);
	}

	/**
	 * Without unsigned.
	 *
	 * @return SignedMessage — результат операции
	 */
	public SignedMessage withoutUnsigned() {
		return this.unsignedContent != null ? new SignedMessage(
				this.link,
				this.signature,
				this.signedBody,
				null,
				this.filterMask
		) : this;
	}

	/**
	 * With filter mask.
	 *
	 * @param filterMask filter mask
	 *
	 * @return SignedMessage — результат операции
	 */
	public SignedMessage withFilterMask(FilterMask filterMask) {
		return this.filterMask.equals(filterMask) ? this : new SignedMessage(
				this.link,
				this.signature,
				this.signedBody,
				this.unsignedContent,
				filterMask
		);
	}

	/**
	 * With filter mask enabled.
	 *
	 * @param enabled enabled
	 *
	 * @return SignedMessage — результат операции
	 */
	public SignedMessage withFilterMaskEnabled(boolean enabled) {
		return this.withFilterMask(enabled ? this.filterMask : FilterMask.PASS_THROUGH);
	}

	/**
	 * Strip signature.
	 *
	 * @return SignedMessage — результат операции
	 */
	public SignedMessage stripSignature() {
		MessageBody messageBody = MessageBody.ofUnsigned(this.getSignedContent());
		MessageLink messageLink = MessageLink.of(this.getSender());
		return new SignedMessage(messageLink, null, messageBody, this.unsignedContent, this.filterMask);
	}

	public static void update(SignatureUpdatable.SignatureUpdater updater, MessageLink link, MessageBody body)
	throws SignatureException {
		updater.update(Ints.toByteArray(1));
		link.update(updater);
		body.update(updater);
	}

	/**
	 * Verify.
	 *
	 * @param verifier verifier
	 *
	 * @return boolean — результат операции
	 */
	public boolean verify(SignatureVerifier verifier) {
		return this.signature != null && this.signature.verify(
				verifier,
				updater -> update(updater, this.link, this.signedBody)
		);
	}

	public String getSignedContent() {
		return this.signedBody.content();
	}

	public Text getContent() {
		return Objects.requireNonNullElseGet(this.unsignedContent, () -> Text.literal(this.getSignedContent()));
	}

	public Instant getTimestamp() {
		return this.signedBody.timestamp();
	}

	public long getSalt() {
		return this.signedBody.salt();
	}

	public boolean isExpiredOnServer(Instant currentTime) {
		return currentTime.isAfter(this.getTimestamp().plus(SERVERBOUND_TIME_TO_LIVE));
	}

	public boolean isExpiredOnClient(Instant currentTime) {
		return currentTime.isAfter(this.getTimestamp().plus(CLIENTBOUND_TIME_TO_LIVE));
	}

	public UUID getSender() {
		return this.link.sender();
	}

	public boolean isSenderMissing() {
		return this.getSender().equals(NIL_UUID);
	}

	public boolean hasSignature() {
		return this.signature != null;
	}

	/**
	 * Проверяет возможность verify from.
	 *
	 * @param sender sender
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canVerifyFrom(UUID sender) {
		return this.hasSignature() && this.link.sender().equals(sender);
	}

	public boolean isFullyFiltered() {
		return this.filterMask.isFullyFiltered();
	}

	/**
	 * To string.
	 *
	 * @param message message
	 *
	 * @return String — результат операции
	 */
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
