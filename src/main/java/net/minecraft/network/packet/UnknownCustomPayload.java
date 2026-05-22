package net.minecraft.network.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;

/**
 * Заглушка для неизвестного плагинового канала: содержимое пакета просто
 * пропускается (skip), а сам объект хранит только идентификатор канала.
 * Используется, когда для данного {@link Identifier} не зарегистрирован кодек.
 */
public record UnknownCustomPayload(Identifier id) implements CustomPayload {

	/**
	 * Создаёт кодек, который при декодировании пропускает байты канала,
	 * не превышающие {@code maxBytes}, а при кодировании ничего не пишет.
	 *
	 * @param id       идентификатор канала
	 * @param maxBytes максимально допустимый размер полезной нагрузки
	 */
	public static <T extends PacketByteBuf> PacketCodec<T, UnknownCustomPayload> createCodec(
			Identifier id,
			int maxBytes
	) {
		return CustomPayload.codecOf(
				(value, buf) -> {}, buf -> {
					int readableBytes = buf.readableBytes();

					if (readableBytes < 0 || readableBytes > maxBytes) {
						throw new IllegalArgumentException("Payload may not be larger than " + maxBytes + " bytes");
					}

					buf.skipBytes(readableBytes);
					return new UnknownCustomPayload(id);
				}
		);
	}

	@Override
	public CustomPayload.Id<UnknownCustomPayload> getId() {
		return new CustomPayload.Id<>(id);
	}
}
