package net.minecraft.network.packet;

import net.minecraft.network.listener.PacketListener;

/**
 * Базовый класс пакета-пачки (bundle): объединяет несколько пакетов в один
 * логический блок, который конвейер обрабатывает атомарно.
 * Открывается маркером {@link BundleSplitterPacket} и закрывается им же.
 */
public abstract class BundlePacket<T extends PacketListener> implements Packet<T> {

	private final Iterable<Packet<? super T>> packets;

	protected BundlePacket(Iterable<Packet<? super T>> packets) {
		this.packets = packets;
	}

	public final Iterable<Packet<? super T>> getPackets() {
		return packets;
	}

	@Override
	public abstract PacketType<? extends BundlePacket<T>> getPacketType();
}
