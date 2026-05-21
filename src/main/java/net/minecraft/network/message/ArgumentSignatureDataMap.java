package net.minecraft.network.message;

import net.minecraft.command.argument.SignedArgumentList;
import net.minecraft.network.PacketByteBuf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Запись argument signature data map.
 */
public record ArgumentSignatureDataMap(List<ArgumentSignatureDataMap.Entry> entries) {

	public static final ArgumentSignatureDataMap EMPTY = new ArgumentSignatureDataMap(List.of());
	private static final int MAX_ARGUMENTS = 8;
	private static final int MAX_ARGUMENT_NAME_LENGTH = 16;

	public ArgumentSignatureDataMap(PacketByteBuf buf) {
		this(buf.<ArgumentSignatureDataMap.Entry, ArrayList<ArgumentSignatureDataMap.Entry>>readCollection(
				PacketByteBuf.getMaxValidator(ArrayList::new, 8),
				ArgumentSignatureDataMap.Entry::new
		));
	}

	/**
	 * Write.
	 *
	 * @param buf buf
	 */
	public void write(PacketByteBuf buf) {
		buf.writeCollection(this.entries, (buf2, entry) -> entry.write(buf2));
	}

	public static ArgumentSignatureDataMap sign(
			SignedArgumentList<?> arguments,
			ArgumentSignatureDataMap.ArgumentSigner signer
	) {
		List<ArgumentSignatureDataMap.Entry> list = arguments.arguments().stream().map(argument -> {
			MessageSignatureData messageSignatureData = signer.sign(argument.value());
			return messageSignatureData != null ? new ArgumentSignatureDataMap.Entry(
					argument.getNodeName(),
					messageSignatureData
			) : null;
		}).filter(Objects::nonNull).toList();
		return new ArgumentSignatureDataMap(list);
	}

	@FunctionalInterface
	/**
	 * Интерфейс argument signer.
	 */
	public interface ArgumentSigner {

		@Nullable MessageSignatureData sign(String value);
	}

	/**
	 * Запись entry.
	 */
	public record Entry(String name, MessageSignatureData signature) {

		public Entry(PacketByteBuf buf) {
			this(buf.readString(16), MessageSignatureData.fromBuf(buf));
		}

		/**
		 * Write.
		 *
		 * @param buf buf
		 */
		public void write(PacketByteBuf buf) {
			buf.writeString(this.name, 16);
			MessageSignatureData.write(buf, this.signature);
		}
	}
}
