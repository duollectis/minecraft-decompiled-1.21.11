package net.minecraft.network.message;

import net.minecraft.command.argument.SignedArgumentList;
import net.minecraft.network.PacketByteBuf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Карта подписей аргументов подписанной команды.
 * Каждая запись связывает имя аргумента с его криптографической подписью.
 */
public record ArgumentSignatureDataMap(List<ArgumentSignatureDataMap.Entry> entries) {

	public static final ArgumentSignatureDataMap EMPTY = new ArgumentSignatureDataMap(List.of());
	private static final int MAX_ARGUMENTS = 8;
	private static final int MAX_ARGUMENT_NAME_LENGTH = 16;

	public ArgumentSignatureDataMap(PacketByteBuf buf) {
		this(buf.<ArgumentSignatureDataMap.Entry, ArrayList<ArgumentSignatureDataMap.Entry>>readCollection(
				PacketByteBuf.getMaxValidator(ArrayList::new, MAX_ARGUMENTS),
				ArgumentSignatureDataMap.Entry::new
		));
	}

	public void write(PacketByteBuf buf) {
		buf.writeCollection(entries, (packetBuf, entry) -> entry.write(packetBuf));
	}

	/**
	 * Подписывает все аргументы из списка с помощью переданного подписчика.
	 * Аргументы, для которых подписчик вернул {@code null}, исключаются из результата.
	 *
	 * @param arguments список аргументов команды с их значениями
	 * @param signer    функция подписи строкового значения аргумента
	 * @return карта подписей для всех успешно подписанных аргументов
	 */
	public static ArgumentSignatureDataMap sign(
			SignedArgumentList<?> arguments,
			ArgumentSignatureDataMap.ArgumentSigner signer
	) {
		List<ArgumentSignatureDataMap.Entry> signed = arguments
				.arguments()
				.stream()
				.map(argument -> {
					MessageSignatureData signature = signer.sign(argument.value());
					return signature != null
							? new ArgumentSignatureDataMap.Entry(argument.getNodeName(), signature)
							: null;
				})
				.filter(Objects::nonNull)
				.toList();

		return new ArgumentSignatureDataMap(signed);
	}

	/**
	 * Функциональный интерфейс для подписи строкового значения аргумента команды.
	 */
	@FunctionalInterface
	public interface ArgumentSigner {

		@Nullable MessageSignatureData sign(String value);
	}

	/**
	 * Пара «имя аргумента — подпись» для одного аргумента подписанной команды.
	 */
	public record Entry(String name, MessageSignatureData signature) {

		public Entry(PacketByteBuf buf) {
			this(buf.readString(MAX_ARGUMENT_NAME_LENGTH), MessageSignatureData.fromBuf(buf));
		}

		public void write(PacketByteBuf buf) {
			buf.writeString(name, MAX_ARGUMENT_NAME_LENGTH);
			MessageSignatureData.write(buf, signature);
		}
	}
}
