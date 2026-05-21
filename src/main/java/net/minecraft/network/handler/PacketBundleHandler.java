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
				}
				else {
					consumer.accept(packet);
				}
			}

			@Override
			public PacketBundleHandler.@Nullable Bundler createBundler(Packet<?> packet) {
				return packet == splitter ? new PacketBundleHandler.Bundler() {
					private final List<Packet<? super T>> packets = new ArrayList<>();

					@Override
					public @Nullable Packet<?> add(Packet<?> packet) {
						if (packet == splitter) {
							return bundleFunction.apply(this.packets);
						}
						else if (this.packets.size() >= 4096) {
							throw new IllegalStateException("Too many packets in a bundle");
						}
						else {
							this.packets.add((Packet<? super T>) packet);
							return null;
						}
					}
				} : null;
			}
		};
	}

	void forEachPacket(Packet<?> packet, Consumer<Packet<?>> consumer);

	PacketBundleHandler.@Nullable Bundler createBundler(Packet<?> splitter);

	public interface Bundler {

		@Nullable Packet<?> add(Packet<?> packet);
	}
}
