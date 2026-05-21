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

	static <B extends PacketByteBuf> PacketCodec<B, CustomPayload> createCodec(
			CustomPayload.CodecFactory<B> unknownCodecFactory, List<CustomPayload.Type<? super B, ?>> types
	) {
		final Map<Identifier, PacketCodec<? super B, ? extends CustomPayload>> map = types.stream()
		                                                                                  .collect(Collectors.toUnmodifiableMap(
				                                                                                  type -> type
						                                                                                  .id()
						                                                                                  .id(),
				                                                                                  CustomPayload.Type::codec
		                                                                                  ));
		return new PacketCodec<B, CustomPayload>() {
			private PacketCodec<? super B, ? extends CustomPayload> getCodec(Identifier id) {
				PacketCodec<? super B, ? extends CustomPayload> packetCodec = map.get(id);
				return packetCodec != null ? packetCodec : unknownCodecFactory.create(id);
			}

			@SuppressWarnings("unchecked")
			private <T extends CustomPayload> void encode(B value, CustomPayload.Id<T> id, CustomPayload payload) {
				value.writeIdentifier(id.id());
				PacketCodec<B, T> packetCodec = (PacketCodec<B, T>) this.getCodec(id.id);
				packetCodec.encode(value, (T) payload);
			}

			public void encode(B packetByteBuf, CustomPayload customPayload) {
				this.encode(packetByteBuf, customPayload.getId(), customPayload);
			}

			public CustomPayload decode(B packetByteBuf) {
				Identifier identifier = packetByteBuf.readIdentifier();
				return (CustomPayload) this.getCodec(identifier).decode(packetByteBuf);
			}
		};
	}

	public interface CodecFactory<B extends PacketByteBuf> {

		PacketCodec<B, ? extends CustomPayload> create(Identifier id);
	}

	public record Id<T extends CustomPayload>(Identifier id) {
	}

	public record Type<B extends PacketByteBuf, T extends CustomPayload>(
			CustomPayload.Id<T> id,
			PacketCodec<B, T> codec
	) {
	}
}
