package net.minecraft.network.handler;

import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.BundlePacket;
import net.minecraft.network.packet.BundleSplitterPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Интерфейс для обработки пакетных бандлов: разбивает {@link BundlePacket} на отдельные пакеты
 * при отправке и собирает их обратно при получении.
 */
public interface PacketBundleHandler {

	int MAX_PACKETS = 4096;

	static <T extends PacketListener, P extends BundlePacket<? super T>> PacketBundleHandler create(
			PacketType<P> id,
			Function<Iterable<Packet<? super T>>, P> bundleFunction,
			BundleSplitterPacket<? super T> splitter
	) {
		return new PacketBundleHandler() {
			@Override
			public void forEachPacket(Packet<?> packet, Consumer<Packet<?>> consumer) {
				if (packet.getPacketType() == id) {
					P bundlePacket = (P) packet;
					consumer.accept(splitter);
					bundlePacket.getPackets().forEach(consumer);
					consumer.accept(splitter);
				} else {
					consumer.accept(packet);
				}
			}

			@Override
			public PacketBundleHandler.@Nullable Bundler createBundler(Packet<?> packet) {
				return packet == splitter ? new PacketBundleHandler.Bundler() {
					private final List<Packet<? super T>> packets = new ArrayList<>();

					@Override
					public @Nullable Packet<?> add(Packet<?> incoming) {
						if (incoming == splitter) {
							return bundleFunction.apply(packets);
						}

						if (packets.size() >= MAX_PACKETS) {
							throw new IllegalStateException("Too many packets in a bundle");
						}

						packets.add((Packet<? super T>) incoming);
						return null;
					}
				} : null;
			}
		};
	}

	void forEachPacket(Packet<?> packet, Consumer<Packet<?>> consumer);

	PacketBundleHandler.@Nullable Bundler createBundler(Packet<?> splitter);

	/**
	 * Аккумулятор пакетов внутри бандла: принимает пакеты до получения завершающего сплиттера.
	 */
	interface Bundler {

		@Nullable Packet<?> add(Packet<?> packet);
	}
}
