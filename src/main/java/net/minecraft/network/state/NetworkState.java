package net.minecraft.network.state;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.handler.PacketBundleHandler;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.util.annotation.Debug;
import org.jspecify.annotations.Nullable;

public interface NetworkState<T extends PacketListener> {

	NetworkPhase id();

	NetworkSide side();

	PacketCodec<ByteBuf, Packet<? super T>> codec();

	@Nullable PacketBundleHandler bundleHandler();

	public interface Factory {

		NetworkState.Unbound buildUnbound();
	}

	public interface Unbound {

		NetworkPhase phase();

		NetworkSide side();

		@Debug
		void forEachPacketType(NetworkState.Unbound.PacketTypeConsumer callback);

		@FunctionalInterface
		public interface PacketTypeConsumer {

			void accept(PacketType<?> type, int protocolId);
		}
	}
}
