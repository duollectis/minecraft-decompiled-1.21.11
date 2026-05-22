package net.minecraft.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketDecoder;
import net.minecraft.network.codec.ValueFirstEncoder;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Произвольная полезная нагрузка пакета с идентификатором канала.
 * Используется для расширяемых каналов данных (плагин-каналы, Fabric API и т.д.).
 */
public interface CustomPayload {

	CustomPayload.Id<? extends CustomPayload> getId();

	static <B extends ByteBuf, T extends CustomPayload> PacketCodec<B, T> codecOf(
			ValueFirstEncoder<B, T> encoder,
			PacketDecoder<B, T> decoder
	) {
		return PacketCodec.of(encoder, decoder);
	}

	static <T extends CustomPayload> CustomPayload.Id<T> id(String id) {
		return new CustomPayload.Id<>(Identifier.ofVanilla(id));
	}

	/**
	 * Создаёт диспетчерный кодек, маршрутизирующий по идентификатору канала.
	 * Неизвестные идентификаторы делегируются в {@code unknownCodecFactory}.
	 *
	 * @param unknownCodecFactory фабрика кодеков для неизвестных идентификаторов
	 * @param types               список зарегистрированных типов полезной нагрузки
	 * @return кодек, умеющий кодировать и декодировать любой зарегистрированный тип
	 */
	static <B extends PacketByteBuf> PacketCodec<B, CustomPayload> createCodec(
			CustomPayload.CodecFactory<B> unknownCodecFactory,
			List<CustomPayload.Type<? super B, ?>> types
	) {
		final Map<Identifier, PacketCodec<? super B, ? extends CustomPayload>> codecByChannel = types
				.stream()
				.collect(Collectors.toUnmodifiableMap(
						type -> type.id().id(),
						CustomPayload.Type::codec
				));

		return new PacketCodec<B, CustomPayload>() {
			private PacketCodec<? super B, ? extends CustomPayload> getCodec(Identifier channelId) {
				PacketCodec<? super B, ? extends CustomPayload> codec = codecByChannel.get(channelId);
				return codec != null ? codec : unknownCodecFactory.create(channelId);
			}

			@SuppressWarnings("unchecked")
			private <T extends CustomPayload> void encode(B buf, CustomPayload.Id<T> payloadId, CustomPayload payload) {
				buf.writeIdentifier(payloadId.id());
				PacketCodec<B, T> codec = (PacketCodec<B, T>) getCodec(payloadId.id);
				codec.encode(buf, (T) payload);
			}

			@Override
			public void encode(B buf, CustomPayload payload) {
				encode(buf, payload.getId(), payload);
			}

			@Override
			public CustomPayload decode(B buf) {
				Identifier channelId = buf.readIdentifier();
				return (CustomPayload) getCodec(channelId).decode(buf);
			}
		};
	}

	/**
	 * Фабрика кодеков для неизвестных идентификаторов каналов.
	 */
	interface CodecFactory<B extends PacketByteBuf> {

		PacketCodec<B, ? extends CustomPayload> create(Identifier id);
	}

	/**
	 * Типизированный идентификатор канала полезной нагрузки.
	 */
	record Id<T extends CustomPayload>(Identifier id) {
	}

	/**
	 * Пара идентификатор + кодек для конкретного типа полезной нагрузки.
	 */
	record Type<B extends PacketByteBuf, T extends CustomPayload>(
			CustomPayload.Id<T> id,
			PacketCodec<B, T> codec
	) {
	}
}
