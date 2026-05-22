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

/**
 * Привязанное состояние сетевого протокола для конкретного слушателя.
 * Содержит кодек для сериализации/десериализации пакетов и опциональный обработчик bundle-пакетов.
 *
 * @param <T> тип слушателя пакетов
 */
public interface NetworkState<T extends PacketListener> {

	NetworkPhase id();

	NetworkSide side();

	PacketCodec<ByteBuf, Packet<? super T>> codec();

	@Nullable PacketBundleHandler bundleHandler();

	/**
	 * Фабрика для создания {@link NetworkState} с привязкой к реестру.
	 */
	interface Factory {

		NetworkState.Unbound buildUnbound();
	}

	/**
	 * Непривязанное состояние протокола — описание без конкретного реестра.
	 * Используется для регистрации типов пакетов до инициализации реестров.
	 */
	interface Unbound {

		NetworkPhase phase();

		NetworkSide side();

		@Debug
		void forEachPacketType(NetworkState.Unbound.PacketTypeConsumer callback);

		/**
		 * Потребитель типов пакетов с их протокольными идентификаторами.
		 */
		@FunctionalInterface
		interface PacketTypeConsumer {

			void accept(PacketType<?> type, int protocolId);
		}
	}
}
