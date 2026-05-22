package net.minecraft.network.handler;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;

/**
 * Строитель диспетчера кодеков с проверкой направления пакета.
 * Гарантирует, что все зарегистрированные пакеты принадлежат одной стороне ({@link NetworkSide}),
 * предотвращая случайную регистрацию серверных пакетов в клиентском кодеке и наоборот.
 */
public class SideValidatingDispatchingCodecBuilder<B extends ByteBuf, L extends PacketListener> {

	private final PacketCodecDispatcher.Builder<B, Packet<? super L>, PacketType<? extends Packet<? super L>>>
			backingBuilder = PacketCodecDispatcher.builder(Packet::getPacketType);
	private final NetworkSide side;

	public SideValidatingDispatchingCodecBuilder(NetworkSide side) {
		this.side = side;
	}

	public <T extends Packet<? super L>> SideValidatingDispatchingCodecBuilder<B, L> add(
			PacketType<T> id,
			PacketCodec<? super B, T> codec
	) {
		if (id.side() != side) {
			throw new IllegalArgumentException(
					"Invalid packet flow for packet " + id + ", expected " + side.name()
			);
		}

		backingBuilder.add(id, codec);
		return this;
	}

	public PacketCodec<B, Packet<? super L>> build() {
		return backingBuilder.build();
	}
}
