package net.minecraft.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketDecoder;
import net.minecraft.network.codec.ValueFirstEncoder;
import net.minecraft.network.listener.PacketListener;

/**
 * Базовый интерфейс сетевого пакета Minecraft.
 * Каждый пакет знает свой тип ({@link PacketType}) и умеет применять себя к слушателю.
 *
 * @param <T> тип слушателя, которому адресован пакет
 */
public interface Packet<T extends PacketListener> {

	PacketType<? extends Packet<T>> getPacketType();

	void apply(T listener);

	default boolean isWritingErrorSkippable() {
		return false;
	}

	default boolean transitionsNetworkState() {
		return false;
	}

	static <B extends ByteBuf, T extends Packet<?>> PacketCodec<B, T> createCodec(
			ValueFirstEncoder<B, T> encoder,
			PacketDecoder<B, T> decoder
	) {
		return PacketCodec.of(encoder, decoder);
	}
}
